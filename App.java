import com.sun.net.httpserver.HttpExchange; // ★ HTTPの要求と応答を扱います。
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection; // ★ データベース接続を扱います。
import java.sql.DriverManager; // ★ SQLiteへ接続します。
import java.sql.PreparedStatement; // ★ 値を安全にSQLへ渡します。
import java.sql.ResultSet; // ★ SELECTの結果を読みます。
import java.sql.SQLException; // ★ SQLのエラーを扱います。
import java.sql.Statement; // ★ テーブル作成に使います。
import java.util.ArrayList;
import java.util.List;

class DbTodo { // ★ Main.javaのTodoと名前が重ならないようにします。
    private final int id;
    private final String title;
    private final boolean done; // ★ DBから読み取った完了状態を保持します。

    DbTodo(int id, String title, boolean done) { // ★ SELECTの3列を受け取ります。
        this.id = id;
        this.title = title;
        this.done = done; // ★ 0/1を変換した値を保存します。
    }

    int getId() {
        return id;
    }

    String getTitle() {
        return title;
    }

    boolean isDone() {
        return done;
    }
}

public class App {
    private static final String DB_URL = "jdbc:sqlite:todos.db"; // ★ 保存先のSQLiteファイルです。

    public static void main(String[] args) throws Exception {
        createTable(); // ★ 起動時にtodos表を用意します。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", App::handle); // ★ 各操作をDBへ送る処理を呼びます。
        server.createContext("/api/todos", exchange -> { // JSON一覧の入口を追加します。
            if (!exchange.getRequestURI().getPath().equals("/api/todos")) { // 入口と完全に一致するか確認します。
                exchange.sendResponseHeaders(404, -1); // 別のパスなら見つからないと返します。
                exchange.close(); // 通信を閉じます。
                return; // 以降の処理を止めます。
            } // パスの確認を終えます。
            if (!exchange.getRequestMethod().equals("GET")) { // GET以外を確認します。
                exchange.getResponseHeaders().set("Allow", "GET"); // 許可する方法を示します。
                exchange.sendResponseHeaders(405, -1); // GET以外は受け付けません。
                exchange.close(); // 通信を閉じます。
                return; // 以降の処理を止めます。
            } // 通信方法の確認を終えます。
            try { // DBの読み込みエラーに備えます。
                sendJson(exchange, 200, todosJson()); // 全TodoをJSONで返します。
            } catch (SQLException e) { // DBの読み込みに失敗した場合です。
                e.printStackTrace(); // 詳細をサーバー側へ出します。
                sendJson(exchange, 500, "{\"error\":\"database error\"}"); // JSON形式でエラーを返します。
            } // DB処理を終えます。
        }); // JSON一覧の入口を閉じます。
        server.start();
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）");
    }

    private static Connection connect() throws SQLException { // ★ SQLiteへの接続を開きます。
        return DriverManager.getConnection(DB_URL); // ★ jdbc:sqlite:todos.dbへ接続します。
    }

    private static void createTable() throws SQLException { // ★ 表がなければ作ります。
        try (Connection connection = connect(); Statement statement = connection.createStatement()) { // ★
                                                                                                      // 接続とSQLを自動で閉じます。
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS todos "
                    + "(id INTEGER PRIMARY KEY, title TEXT, done INTEGER)"); // ★ 指定された3列を作ります。
        }
    }

    private static void insertTodo(String title) throws SQLException { // ★ 追加はINSERTで行います。
        String sql = "INSERT INTO todos (title, done) VALUES (?, 0)"; // ★ IDはSQLiteに採番させます。
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) { // ★
                                                                                                                  // 値を埋め込まずに渡します。
            statement.setString(1, title); // ★ 1番目の?にタイトルを設定します。
            statement.executeUpdate(); // ★ 1件追加します。
        }
    }

    private static void completeTodo(int id) throws SQLException { // ★ 完了はUPDATEで行います。
        String sql = "UPDATE todos SET done = 1 WHERE id = ?"; // ★ 指定したIDだけを更新します。
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) { // ★
                                                                                                                  // SQLを準備します。
            statement.setInt(1, id); // ★ 1番目の?にIDを設定します。
            statement.executeUpdate(); // ★ 完了状態を保存します。
        }
    }

    private static void deleteTodo(int id) throws SQLException { // ★ 削除はDELETEで行います。
        String sql = "DELETE FROM todos WHERE id = ?"; // ★ 指定したIDだけを削除します。
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) { // ★
                                                                                                                  // SQLを準備します。
            statement.setInt(1, id); // ★ 1番目の?にIDを設定します。
            statement.executeUpdate(); // ★ 1件削除します。
        }
    }

    private static List<DbTodo> selectTodos() throws SQLException { // ★ 一覧は毎回SELECTで取得します。
        List<DbTodo> todos = new ArrayList<>(); // ★ 検索結果を入れる一覧です。
        String sql = "SELECT id, title, done FROM todos ORDER BY id"; // ★ ID順に読みます。
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) { // ★ 検索後に資源を閉じます。
            while (results.next()) { // ★ 行を1件ずつ取り出します。
                todos.add(new DbTodo(results.getInt("id"), results.getString("title"),
                        results.getInt("done") == 1)); // ★ DBの0/1をbooleanに変えます。
            }
        }
        return todos; // ★ 画面表示に使う一覧を返します。
    }

    private static void handle(HttpExchange exchange) throws IOException { // ★ HTTP操作をSQLに結びます。
        String path = exchange.getRequestURI().getPath(); // ★ アクセス先を調べます。
        String method = exchange.getRequestMethod(); // ★ GETかPOSTかを調べます。
        try { // ★ SQLエラーを画面に返せるようにします。
            if (path.equals("/add") && method.equals("POST")) { // ★ フォームからの追加です。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // ★
                                                                                                            // 送信内容を読みます。
                if (body.startsWith("todo=")) { // ★ フォームの項目名を確認します。
                    String title = URLDecoder.decode(body.substring(5), StandardCharsets.UTF_8); // ★ タイトルを復元します。
                    if (!title.isBlank()) { // ★ 空のタイトルは登録しません。
                        insertTodo(title); // ★ INSERTを呼びます。
                    }
                }
                redirect(exchange); // ★ 一覧へ戻します。
                return;
            }
            if (path.equals("/done") && method.equals("GET")) { // ★ 完了リンクを処理します。
                Integer id = requestedId(exchange); // ★ リンクのIDを読みます。
                if (id != null) { // ★ 数字のIDだけを扱います。
                    completeTodo(id); // ★ UPDATEを呼びます。
                }
                redirect(exchange); // ★ 一覧へ戻します。
                return;
            }
            if (path.equals("/delete") && method.equals("GET")) { // ★ 削除リンクを処理します。
                Integer id = requestedId(exchange); // ★ リンクのIDを読みます。
                if (id != null) { // ★ 数字のIDだけを扱います。
                    deleteTodo(id); // ★ DELETEを呼びます。
                }
                redirect(exchange); // ★ 一覧へ戻します。
                return;
            }
            if (path.equals("/") && method.equals("GET")) { // ★ 一覧ページを表示します。
                send(exchange, 200, "text/html", page()); // ★ SELECTした一覧を返します。
                return;
            }
            send(exchange, 404, "text/plain", "ページが見つかりません"); // ★ 未知の場所は404にします。
        } catch (SQLException e) { // ★ DBで問題が起きた場合です。
            e.printStackTrace(); // ★ 詳細をサーバー側に記録します。
            send(exchange, 500, "text/plain", "データベースエラーが発生しました"); // ★ 画面には簡潔に伝えます。
        }
    }

    private static Integer requestedId(HttpExchange exchange) { // ★ URLからIDを取り出します。
        String query = exchange.getRequestURI().getQuery(); // ★ ?以降を読みます。
        if (query == null || !query.startsWith("id=")) { // ★ ID指定がない場合です。
            return null;
        }
        try { // ★ 数字でないIDに備えます。
            return Integer.parseInt(query.substring(3)); // ★ 数字に変換します。
        } catch (NumberFormatException e) { // ★ 変換できない場合です。
            return null;
        }
    }

    private static String page() throws SQLException { // ★ DBの一覧からHTMLを作ります。
        StringBuilder html = new StringBuilder(
                "<form method='post' action='/add'><input name='todo'><button>追加</button></form><ul>"); // ★ 入力欄を作ります。
        for (DbTodo todo : selectTodos()) { // ★ SELECTの結果を1件ずつ表示します。
            html.append("<li>").append(escapeHtml(todo.getTitle())); // ★ タイトルを安全に表示します。
            if (todo.isDone()) { // ★ 完了したTodoを調べます。
                html.append(" ✔"); // ★ 完了マークを付けます。
            }
            html.append(" <a href='/done?id=").append(todo.getId())
                    .append("'>完了</a> <a href='/delete?id=").append(todo.getId())
                    .append("'>削除</a></li>"); // ★ 操作リンクにDBのIDを入れます。
        }
        return html.append("</ul>").toString(); // ★ 一覧を閉じて返します。
    }

    private static String escapeHtml(String value) { // ★ タイトルをHTMLとして解釈させません。
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;"); // ★ 特殊文字を置き換えます。
    }

    private static String todosJson() throws SQLException { // Todo一覧をJSON文字列にします。
        StringBuilder json = new StringBuilder("["); // JSON配列を開始します。
        boolean first = true; // 最初の要素かどうかを覚えます。
        for (DbTodo todo : selectTodos()) { // DBから全Todoを読み出します。
            if (!first) { // 2件目以降か確認します。
                json.append(','); // 要素間にカンマを入れます。
            } // カンマの処理を終えます。
            json.append("{\"title\":\"").append(escapeJson(todo.getTitle())) // タイトルをJSON用に変換します。
                    .append("\",\"done\":").append(todo.isDone()).append('}'); // 完了状態を真偽値で加えます。
            first = false; // 次の要素からカンマが必要です。
        } // 全Todoの処理を終えます。
        return json.append(']').toString(); // JSON配列を閉じます。
    } // JSON文字列の作成を終えます。

    private static String escapeJson(String value) { // タイトル内の特殊文字をエスケープします。
        StringBuilder escaped = new StringBuilder(); // 変換後の文字列を入れます。
        for (int i = 0; i < value.length(); i++) { // 文字を1つずつ調べます。
            char ch = value.charAt(i); // 現在の文字を取り出します。
            switch (ch) { // 文字の種類で処理を分けます。
                case '"':
                    escaped.append("\\\"");
                    break; // 二重引用符をエスケープします。
                case '\\':
                    escaped.append("\\\\");
                    break; // バックスラッシュをエスケープします。
                case '\n':
                    escaped.append("\\n");
                    break; // 改行をエスケープします。
                case '\r':
                    escaped.append("\\r");
                    break; // 復帰をエスケープします。
                case '\t':
                    escaped.append("\\t");
                    break; // タブをエスケープします。
                default: // その他の文字を処理します。
                    if (ch < 0x20) { // 残りの制御文字を確認します。
                        escaped.append("\\u00"); // Unicode形式の先頭を付けます。
                        escaped.append(Character.forDigit((ch >>> 4) & 0xf, 16)); // 上位1桁を付けます。
                        escaped.append(Character.forDigit(ch & 0xf, 16)); // 下位1桁を付けます。
                    } else { // 通常の文字の場合です。
                        escaped.append(ch); // そのまま加えます。
                    } // 制御文字の確認を終えます。
            } // 文字種ごとの処理を終えます。
        } // 全文字の処理を終えます。
        return escaped.toString(); // エスケープ済み文字列を返します。
    } // エスケープ処理を終えます。

    private static void sendJson(HttpExchange exchange, int status, String message) throws IOException { // JSONを返します。
        byte[] body = message.getBytes(StandardCharsets.UTF_8); // 応答をUTF-8のバイト列にします。
        exchange.getResponseHeaders().set("Content-Type", "application/json"); // charsetなしで種類を指定します。
        exchange.sendResponseHeaders(status, body.length); // 状態とバイト数を送ります。
        try (var response = exchange.getResponseBody()) { // 応答の書き込み先を閉じられるようにします。
            response.write(body); // JSON本文を書き込みます。
        } // 応答を閉じます。
    } // JSON応答処理を終えます。

    private static void redirect(HttpExchange exchange) throws IOException { // ★ 操作後に一覧へ戻します。
        exchange.getResponseHeaders().set("Location", "/"); // ★ 戻り先を指定します。
        exchange.sendResponseHeaders(303, -1); // ★ ブラウザに再表示を指示します。
        exchange.close(); // ★ 通信を閉じます。
    }

    private static void send(HttpExchange exchange, int status, String type, String message) throws IOException { // ★
                                                                                                                  // 応答を返します。
        byte[] body = message.getBytes(StandardCharsets.UTF_8); // ★ UTF-8に変換します。
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8"); // ★ 応答の種類を示します。
        exchange.sendResponseHeaders(status, body.length); // ★ 状態と長さを送ります。
        try (var response = exchange.getResponseBody()) { // ★ 通信を自動で閉じます。
            response.write(body); // ★ 本文を送ります。
        }
    }
}
