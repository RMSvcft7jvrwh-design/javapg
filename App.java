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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

enum TodoStatus { // Todoが進む3つの状態を表します。
    NOT_STARTED, IN_PROGRESS, DONE
}

class DbTodo { // ★ Main.javaのTodoと名前が重ならないようにします。
    private final int id;
    private final String title;
    private final TodoStatus status; // DBから読み取った進行状態を保持します。
    private final Long startedAt; // 開始時刻をミリ秒で保持します。未開始ならnullです。
    private final Long completedAt; // 完了時刻をミリ秒で保持します。未完了ならnullです。

    DbTodo(int id, String title, TodoStatus status, Long startedAt, Long completedAt) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    int getId() {
        return id;
    }

    String getTitle() {
        return title;
    }

    TodoStatus getStatus() {
        return status;
    }

    Long getStartedAt() {
        return startedAt;
    }

    Long getCompletedAt() {
        return completedAt;
    }

    boolean isDone() { // 既存のJSON API用に完了かどうかを返します。
        return status == TodoStatus.DONE;
    }
}

public class App {
    private static final String DB_URL = "jdbc:sqlite:todos.db"; // ★ 保存先のSQLiteファイルです。
    private static final DateTimeFormatter START_FORMAT = DateTimeFormatter
            .ofPattern("yyyy/MM/dd HH:mm").withZone(ZoneId.systemDefault());

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

    private static void createTable() throws SQLException { // 表を作り、古いdone列から状態を引き継ぎます。
        try (Connection connection = connect()) {
            connection.setAutoCommit(false); // 移行中に失敗したら元へ戻せるようにします。
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS todos "
                        + "(id INTEGER PRIMARY KEY, title TEXT, "
                        + "status TEXT NOT NULL DEFAULT 'NOT_STARTED', "
                        + "started_at INTEGER, completed_at INTEGER)"); // 開始・完了時刻も保存します。
                boolean hasStatus = false;
                boolean hasStartedAt = false;
                boolean hasCompletedAt = false;
                try (ResultSet columns = statement.executeQuery("PRAGMA table_info(todos)")) {
                    while (columns.next()) {
                        String column = columns.getString("name");
                        if ("status".equals(column)) {
                            hasStatus = true;
                        } else if ("started_at".equals(column)) {
                            hasStartedAt = true;
                        } else if ("completed_at".equals(column)) {
                            hasCompletedAt = true;
                        }
                    }
                }
                if (!hasStatus) { // 以前のdone列だけがあるDBを更新します。
                    statement.executeUpdate("ALTER TABLE todos ADD COLUMN "
                            + "status TEXT NOT NULL DEFAULT 'NOT_STARTED'");
                    statement.executeUpdate("UPDATE todos SET status = 'DONE' WHERE done = 1");
                }
                if (!hasStartedAt) {
                    statement.executeUpdate("ALTER TABLE todos ADD COLUMN started_at INTEGER");
                }
                if (!hasCompletedAt) {
                    statement.executeUpdate("ALTER TABLE todos ADD COLUMN completed_at INTEGER");
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback(); // 途中までの変更を残しません。
                throw e;
            }
        }
    }

    private static void insertTodo(String title) throws SQLException { // 追加時は未着手にします。
        String sql = "INSERT INTO todos (title, status) VALUES (?, ?)"; // IDはSQLiteに採番させます。
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) { // ★
                                                                                                                  // 値を埋め込まずに渡します。
            statement.setString(1, title); // ★ 1番目の?にタイトルを設定します。
            statement.setString(2, TodoStatus.NOT_STARTED.name()); // 新しいTodoの状態です。
            statement.executeUpdate(); // ★ 1件追加します。
        }
    }

    private static void startTodo(int id) throws SQLException { // 未着手のTodoだけ開始します。
        String sql = "UPDATE todos SET status = ?, started_at = ?, completed_at = NULL "
                + "WHERE id = ? AND status = ?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TodoStatus.IN_PROGRESS.name());
            statement.setLong(2, System.currentTimeMillis()); // 開始ボタンを押した時刻です。
            statement.setInt(3, id);
            statement.setString(4, TodoStatus.NOT_STARTED.name());
            statement.executeUpdate();
        }
    }

    private static void completeTodo(int id) throws SQLException { // 作業中のTodoだけ完了します。
        String sql = "UPDATE todos SET status = ?, completed_at = ? WHERE id = ? AND status = ?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) { // ★
                                                                                                                  // SQLを準備します。
            statement.setString(1, TodoStatus.DONE.name());
            statement.setLong(2, System.currentTimeMillis()); // 完了ボタンを押した時刻です。
            statement.setInt(3, id);
            statement.setString(4, TodoStatus.IN_PROGRESS.name());
            statement.executeUpdate(); // 完了状態を保存します。
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

    private static void deleteCompletedTodos() throws SQLException { // 完了済みだけをまとめて削除します。
        String sql = "DELETE FROM todos WHERE status = ?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TodoStatus.DONE.name()); // 対象を完了状態に限定します。
            statement.executeUpdate(); // 条件に合うTodoを1回のSQLで削除します。
        }
    }

    private static List<DbTodo> selectTodos() throws SQLException { // ★ 一覧は毎回SELECTで取得します。
        List<DbTodo> todos = new ArrayList<>(); // ★ 検索結果を入れる一覧です。
        String sql = "SELECT id, title, status, started_at, completed_at FROM todos ORDER BY id";
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet results = statement.executeQuery()) { // ★ 検索後に資源を閉じます。
            while (results.next()) { // ★ 行を1件ずつ取り出します。
                String status = results.getString("status");
                long startValue = results.getLong("started_at");
                Long startedAt = results.wasNull() ? null : startValue;
                long completedValue = results.getLong("completed_at");
                Long completedAt = results.wasNull() ? null : completedValue;
                try {
                    todos.add(new DbTodo(results.getInt("id"), results.getString("title"),
                            TodoStatus.valueOf(status), startedAt, completedAt)); // 時刻も読み込みます。
                } catch (IllegalArgumentException | NullPointerException e) {
                    throw new SQLException("不正なTodoの状態: " + status, e);
                }
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
            if (path.equals("/start") && method.equals("POST")) { // 開始ボタンを処理します。
                Integer id = requestedId(exchange);
                if (id != null) {
                    startTodo(id); // 未着手から作業中にします。
                }
                redirect(exchange);
                return;
            }
            if (path.equals("/done") && method.equals("POST")) { // 完了ボタンを処理します。
                Integer id = requestedId(exchange);
                if (id != null) { // ★ 数字のIDだけを扱います。
                    completeTodo(id); // 作業中から完了にします。
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
            if (path.equals("/delete-completed") && method.equals("POST")) { // 一括削除ボタンを処理します。
                deleteCompletedTodos(); // DB内の完了済みだけを削除します。
                redirect(exchange); // 更新後の件数を表示します。
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
        List<DbTodo> todos = selectTodos(); // 表示と集計に使うTodoを1回だけ読みます。
        long pageNow = System.currentTimeMillis(); // 画面を作った時刻をタイマーの基準にします。
        int notStartedCount = 0;
        int inProgressCount = 0;
        int doneCount = 0;
        for (DbTodo todo : todos) { // 状態ごとの件数を数えます。
            if (todo.getStatus() == TodoStatus.NOT_STARTED) {
                notStartedCount++;
            } else if (todo.getStatus() == TodoStatus.IN_PROGRESS) {
                inProgressCount++;
            } else {
                doneCount++;
            }
        }
        StringBuilder html = new StringBuilder(
                "<style>.working{background:#fff2b3;padding:0 .2em}"
                        + ".action-form{display:inline}"
                        + ".table-wrap{overflow-x:auto}"
                        + "table{border-collapse:collapse;margin-top:1em}"
                        + "th,td{padding:.35em .65em;text-align:left;vertical-align:middle}"
                        + "th{border-bottom:1px solid #bbb}"
                        + ".start-time,.timer{white-space:nowrap}"
                        + ".start-time{color:#555}"
                        + ".timer{font-variant-numeric:tabular-nums;text-align:right}</style>"
                        + "<h1>Todoリスト</h1>"); // 見出しと作業中の色を用意します。
        html.append("<p>合計 ").append(todos.size()).append("件｜未着手 ")
                .append(notStartedCount).append("件｜作業中 ").append(inProgressCount)
                .append("件｜完了 ").append(doneCount).append("件</p>"); // 見出しの下に件数を表示します。
        html.append("<form method='post' action='/add'><input name='todo'>")
                .append("<button>追加</button></form>"); // 入力欄を表示します。
        html.append("<form method='post' action='/delete-completed' ")
                .append("onsubmit=\"return confirm('完了済みのTodoをすべて削除しますか？')\">")
                .append("<button type='submit'"); // 押したときだけ確認を出します。
        if (doneCount == 0) {
            html.append(" disabled"); // 対象がないときは押せないようにします。
        }
        html.append(">完了済みを一括削除</button></form>");
        html.append("<div class='table-wrap'><table><thead><tr>")
                .append("<th>開始日時</th><th>Todo</th><th>経過時間</th>")
                .append("<th>状態・操作</th><th></th></tr></thead><tbody>");
        for (DbTodo todo : todos) { // 同じ一覧を1件ずつ表示します。
            html.append("<tr><td class='start-time'>");
            if (todo.getStartedAt() != null) {
                html.append(START_FORMAT.format(Instant.ofEpochMilli(todo.getStartedAt())));
            }
            html.append("</td><td>"); // 開始日時をタイトルの左に置きます。
            if (todo.getStatus() == TodoStatus.IN_PROGRESS) {
                html.append("<span class='working'>"); // 作業中だけ薄いマーカーを付けます。
            }
            html.append(escapeHtml(todo.getTitle())); // タイトルを安全に表示します。
            if (todo.getStatus() == TodoStatus.IN_PROGRESS) {
                html.append("</span>");
            }
            html.append("</td><td class='timer'");
            if (todo.getStatus() == TodoStatus.IN_PROGRESS && todo.getStartedAt() != null) {
                html.append(" data-start='").append(todo.getStartedAt()).append("'");
            }
            html.append(">"); // 経過時間をタイトルの右に置きます。
            if (todo.getStartedAt() != null) {
                if (todo.getStatus() == TodoStatus.IN_PROGRESS) {
                    html.append(formatElapsed(pageNow - todo.getStartedAt()));
                } else if (todo.getStatus() == TodoStatus.DONE && todo.getCompletedAt() != null) {
                    html.append(formatElapsed(todo.getCompletedAt() - todo.getStartedAt()));
                }
            }
            html.append("</td><td>");
            if (todo.getStatus() == TodoStatus.NOT_STARTED) {
                html.append("<form class='action-form' method='post' action='/start?id=")
                        .append(todo.getId()).append("'><button>開始</button></form>");
            } else if (todo.getStatus() == TodoStatus.IN_PROGRESS) {
                html.append("<small>作業中</small> ");
                html.append("<form class='action-form' method='post' action='/done?id=")
                        .append(todo.getId()).append("'><button>完了</button></form>");
            } else {
                html.append("✔"); // 完了後はマーカーと操作ボタンを出しません。
            }
            html.append("</td><td><a href='/delete?id=").append(todo.getId())
                    .append("'>削除</a></td></tr>"); // 削除リンクはどの状態でも使えます。
        }
        html.append("</tbody></table></div><script>const serverNow=").append(pageNow)
                .append(";const openedAt=performance.now();")
                .append("function updateTimers(){const now=serverNow+(performance.now()-openedAt);")
                .append("document.querySelectorAll('.timer[data-start]').forEach(el=>{")
                .append("const seconds=Math.max(0,Math.floor((now-Number(el.dataset.start))/1000));")
                .append("const hours=Math.floor(seconds/3600);")
                .append("const minutes=Math.floor(seconds%3600/60);const rest=seconds%60;")
                .append("el.textContent=String(hours).padStart(2,'0')+':'")
                .append("+String(minutes).padStart(2,'0')+':'")
                .append("+String(rest).padStart(2,'0');});}")
                .append("updateTimers();setInterval(updateTimers,1000);</script>"); // 作業中だけ毎秒更新します。
        return html.toString();
    }

    private static String formatElapsed(long elapsedMillis) { // 経過時間を時:分:秒にします。
        long seconds = Math.max(0, elapsedMillis / 1000);
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
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
