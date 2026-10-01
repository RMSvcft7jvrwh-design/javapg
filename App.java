
// Webサーバーを使うためのクラスを読み込みます。
import com.sun.net.httpserver.HttpServer;
// 通信の待ち受け先を指定するクラスを読み込みます。
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets; // 日本語を正しく扱うための文字コードです。
// 複数のTodoを入れるListを読み込みます。★変更
import java.util.List;
// ListにTodoを追加するArrayListを読み込みます。★変更
import java.util.ArrayList;

class Todo { // ★変更
    private final int id; // ★変更
    private final String title; // ★変更
    private boolean done; // ★変更

    Todo(int id, String title) { // ★変更
        this.id = id; // ★変更
        this.title = title; // ★変更
        this.done = false; // ★変更
    } // ★変更

    int getId() { // ★変更
        return id; // ★変更
    } // ★変更

    String getTitle() { // ★変更
        return title; // ★変更
    } // ★変更

    boolean isDone() { // ★変更
        return done; // ★変更
    } // ★変更

    void setDone(boolean done) { // ★変更
        this.done = done; // ★変更
    } // ★変更
} // ★変更

// プログラム全体を入れるクラスです。
public class App {
    static List<Todo> todos = new ArrayList<>(); // ★変更
    static int nextId = 1; // ★変更

    // プログラムを開始する場所です。
    public static void main(String[] args) throws Exception {
        // 8080番ポートで待ち受けるWebサーバーを作ります。【1】
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        todos.add(new Todo(nextId++, "牛乳を買う")); // ★変更
        Todo egg = new Todo(nextId++, "卵を買う"); // ★変更

        egg.setDone(true); // ★変更
        todos.add(egg); // ★変更

        // 「/」にアクセスされたときの処理を直接書きます。 【1】
        server.createContext("/", exchange -> {

            // アクセスされた場所と通信方法を確認します。
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出します。
            String message;
            String method = exchange.getRequestMethod(); // GETやPOSTなどの方法を取り出します。
            // 返す文字の種類を最初はtext/plainにします。
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");

            // Todoを追加する機能です。
            if (path.equals("/add") && method.equals("POST")) { // Todo追加のPOST（送信）を処理します。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // 送信された中身を受け取ります。
                String value = body.substring(5); // todo=の5文字を除きます。
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8); // URL形式を日本語に戻します。★変更
                if (!title.isEmpty()) { // 空でないTodoだけを追加します。★変更
                    todos.add(new Todo(nextId, title)); // ★変更
                    nextId++; // ★変更
                }
                exchange.getResponseHeaders().set("Location", "/"); // 戻り先を指定します。
                exchange.sendResponseHeaders(303, -1); // 303でトップページへ戻します。
                exchange.close(); // 通信を閉じます。
                return; // この分岐を終えます。

            } else if (path.equals("/done") && method.equals("GET")) { // 完了リンクを押したときの処理です。★追加
                String query = exchange.getRequestURI().getQuery(); // リンクに付いたidを受け取ります。★追加
                if (query != null && query.startsWith("id=") && query.length() > 3) { // idが付いているか確認します。★追加
                    try { // 数字でないidに備えます。★追加
                        int id = Integer.parseInt(query.substring(3)); // idを数に変えます。★追加
                        for (Todo todo : todos) { // Todoを1件ずつ見ます。★追加
                            if (todo.getId() == id) { // idが一致するTodoを探します。★追加
                                todo.setDone(true); // 一致したTodoを完了にします。★追加
                                break; // 見つかったので探すのを終えます。★追加
                            } // ★追加
                        } // ★追加
                    } catch (NumberFormatException e) { // idが数字でない場合を受け止めます。★追加
                        // 一覧は変更しません。★追加
                    } // ★追加
                } // ★追加
                exchange.getResponseHeaders().set("Location", "/"); // 一覧へ戻る場所を指定します。★追加
                exchange.sendResponseHeaders(303, -1); // 一覧へ戻します。★追加
                exchange.close(); // 通信を閉じます。★追加
                return; // この分岐を終えます。★追加

            } else if (path.equals("/delete") && method.equals("GET")) { // 削除リンクを押したときの処理です。★追加
                String query = exchange.getRequestURI().getQuery(); // リンクに付いたidを受け取ります。★追加
                if (query != null && query.startsWith("id=") && query.length() > 3) { // idが付いているか確認します。★追加
                    try { // 数字でないidに備えます。★追加
                        int id = Integer.parseInt(query.substring(3)); // idを数に変えます。★追加
                        todos.removeIf(todo -> todo.getId() == id); // idが一致するTodoを削除します。★追加
                    } catch (NumberFormatException e) { // idが数字でない場合を受け止めます。★追加
                        // 一覧は変更しません。★追加
                    } // ★追加
                } // ★追加
                exchange.getResponseHeaders().set("Location", "/"); // 一覧へ戻る場所を指定します。★追加
                exchange.sendResponseHeaders(303, -1); // 一覧へ戻します。★追加
                exchange.close(); // 通信を閉じます。★追加
                return; // この分岐を終えます。★追加

                // Todo入力フォームと一覧を表示する機能です。
            } else if (path.equals("/")) { // トップページにフォームとTodo一覧を表示します。
                String html = "<form method='post' action='/add'><input name='todo'><button>追加</button></form><ul>"; // 入力フォームを作ります。
                for (Todo todo : todos) { // Todoを1件ずつ取り出します。★変更
                    String mark = ""; // ★変更
                    if (todo.isDone()) { // ★変更
                        mark = " ✔"; // ★変更
                    } // ★変更
                    html += "<li>" + todo.getTitle() + mark + " <a href='/done?id=" + todo.getId()
                            + "'>完了</a> <a href='/delete?id=" + todo.getId() + "'>削除</a></li>"; // ★追加
                }
                html += "</ul>"; // 一覧を閉じます。
                message = html; // HTMLを返す内容にします。
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // HTMLとして返します。

                // どの機能にも当てはまらない場合の受け皿です。
            } else {
                message = "ページが見つかりません";
            }

            // 応答（画面に返す内容）を送る共通処理です。
            // 返す文字をUTF-8のバイト列に変換します。 【毎】
            byte[] body = message.getBytes("UTF-8");
            // 成功を表す番号と返すデータの長さを送ります。 【毎】
            exchange.sendResponseHeaders(200, body.length);
            // 返すデータを書き込みます。 【毎】
            exchange.getResponseBody().write(body);
            // 返し終わった通信を閉じます。 【毎】
            exchange.getResponseBody().close();
        });
        // Webサーバーの待ち受けを開始します。【1】
        server.start();
        // サーバーの起動メッセージをターミナルに表示します。
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）");
    }
}
