
// Webサーバーを使うためのクラスを読み込みます。
import com.sun.net.httpserver.HttpServer;
// 通信の待ち受け先を指定するクラスを読み込みます。
import java.net.InetSocketAddress;
import java.net.URLDecoder;
// 複数の文字列を入れるListを読み込みます。
import java.util.List;
// Listに文字列を追加するArrayListを読み込みます。
import java.util.ArrayList;

// プログラム全体を入れるクラスです。
public class App {
    // プログラムを開始する場所です。
    public static void main(String[] args) throws Exception {
        // 8080番ポートで待ち受けるWebサーバーを作ります。【1】
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        // 「/」にアクセスされたときの処理を直接書きます。 【1】
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出します。
            String message;
            // 返す文字の種類を最初はtext/plainにします。
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            if (path.equals("/hello")) { // パスが「/hello」か比べます。
                String query = exchange.getRequestURI().getRawQuery(); // URLのクエリを取り出します。
                String name;
                if (query == null) { // クエリがないか調べます。
                    name = "ゲスト"; // 名前がないときは「ゲスト」にします。
                } else {
                    name = query.substring(5); // name= の後ろを切り出します。
                    name = URLDecoder.decode(name, "UTF-8"); // URL用に変換された名前を日本語に戻します。
                }
                message = "こんにちは、" + name + "さん！"; // 名前を応答に加えます。
            } else if (path.equals("/todos")) { // パスが「/todos」か比べます。
                List<String> todos = new ArrayList<>(); // Todoを入れるListを作ります。
                todos.add("牛乳を買う"); // Todoを1件追加します。
                todos.add("卵を買う"); // Todoを1件追加します。
                todos.add("パンを買う"); // Todoを1件追加します。
                todos.add("新聞を取る");// Todoを1件追加します。
                String html = "<ul>"; // ulの開始タグを入れます。
                for (String todo : todos) { // Todoを1件ずつ取り出します。
                    html += "<li>" + todo + "</li>"; // Todoをliとして追加します。
                }
                html += "</ul>"; // ulの終了タグを入れます。
                message = html; // 組み立てたHTMLを返す中身にします。
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // /todosをHTMLとして返します。
            } else if (path.equals("/bye")) {
                message = "さようなら！";
            } else if (path.equals("/menu")) {
                message = "今日の定食はカレー";
            } else {
                message = "ページが見つかりません";
            }
            // 返す文字をUTF-8のバイト列に変換します。 【毎】
            byte[] body = message.getBytes("UTF-8");
            // 成功を表す番号と返すデータの長さを送ります。 【毎】
            exchange.sendResponseHeaders(200, body.length);
            // 返すデータを書き込みます。 【毎】
            exchange.getResponseBody().write(body);
            // 返し終わった通信を閉じます。 【毎】
            // 返し終わった通信を閉じます。 【毎】
            exchange.getResponseBody().close();
        });
        // Webサーバーの待ち受けを開始します。【1】
        server.start();
        // サーバーの起動メッセージをターミナルに表示します。
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）");
    }
}
