import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

class TodoHandler {
    private final TodoRepository repository;
    private final TodoView view;

    TodoHandler(TodoRepository repository, TodoView view) {
        this.repository = repository;
        this.view = view;
    }

    void handleApiTodos(HttpExchange exchange) throws IOException {
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
            sendJson(exchange, 200, view.todosJson(repository.selectTodos())); // 全TodoをJSONで返します。
        } catch (SQLException e) { // DBの読み込みに失敗した場合です。
            e.printStackTrace(); // 詳細をサーバー側へ出します。
            sendJson(exchange, 500, "{\"error\":\"database error\"}"); // JSON形式でエラーを返します。
        } // DB処理を終えます。
    }

    void handle(HttpExchange exchange) throws IOException { // HTTP操作を担当します。
        String path = exchange.getRequestURI().getPath(); // ★ アクセス先を調べます。
        String method = exchange.getRequestMethod(); // ★ GETかPOSTかを調べます。
        try { // ★ SQLエラーを画面に返せるようにします。
            if (path.equals("/add") && method.equals("POST")) { // ★ フォームからの追加です。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // ★
                                                                                                            // 送信内容を読みます。
                try {
                    String title = parameterValue(body, "todo");
                    String dueDateText = parameterValue(body, "dueDate");
                    if (title != null && !title.isBlank()) {
                        LocalDate dueDate = dueDateText == null || dueDateText.isBlank()
                                ? null : LocalDate.parse(dueDateText);
                        repository.insertTodo(title, dueDate);
                    }
                } catch (DateTimeParseException e) {
                    send(exchange, 400, "text/plain", "予定日の形式が正しくありません");
                    return;
                } catch (IllegalArgumentException e) {
                    send(exchange, 400, "text/plain", "入力の形式が正しくありません");
                    return;
                }
                redirect(exchange); // ★ 一覧へ戻します。
                return;
            }
            if (path.equals("/start") && method.equals("POST")) { // 開始ボタンを処理します。
                Integer id = requestedId(exchange);
                if (id != null) {
                    repository.startTodo(id); // 未着手から作業中にします。
                }
                redirect(exchange, requestedFilter(exchange));
                return;
            }
            if (path.equals("/done") && method.equals("POST")) { // 完了ボタンを処理します。
                Integer id = requestedId(exchange);
                if (id != null) { // ★ 数字のIDだけを扱います。
                    repository.completeTodo(id); // 作業中から完了にします。
                }
                redirect(exchange, requestedFilter(exchange));
                return;
            }
            if (path.equals("/delete") && method.equals("GET")) { // ★ 削除リンクを処理します。
                Integer id = requestedId(exchange); // ★ リンクのIDを読みます。
                if (id != null) { // ★ 数字のIDだけを扱います。
                    repository.deleteTodo(id); // ★ DELETEを呼びます。
                }
                redirect(exchange, requestedFilter(exchange));
                return;
            }
            if (path.equals("/delete-completed") && method.equals("POST")) { // 一括削除ボタンを処理します。
                repository.deleteCompletedTodos(); // DB内の完了済みだけを削除します。
                redirect(exchange, requestedFilter(exchange));
                return;
            }
            if (path.equals("/") && method.equals("GET")) { // ★ 一覧ページを表示します。
                String filter = requestedFilter(exchange);
                LocalDate today = LocalDate.now();
                send(exchange, 200, "text/html",
                        view.page(repository.selectTodos(filter, today), filter, today));
                return;
            }
            send(exchange, 404, "text/plain", "ページが見つかりません"); // ★ 未知の場所は404にします。
        } catch (SQLException e) { // ★ DBで問題が起きた場合です。
            e.printStackTrace(); // ★ 詳細をサーバー側に記録します。
            send(exchange, 500, "text/plain", "データベースエラーが発生しました"); // ★ 画面には簡潔に伝えます。
        }
    }

    private Integer requestedId(HttpExchange exchange) { // ★ URLからIDを取り出します。
        try { // ★ 数字でないIDに備えます。
            String id = parameterValue(exchange.getRequestURI().getRawQuery(), "id");
            return id == null ? null : Integer.parseInt(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String requestedFilter(HttpExchange exchange) {
        try {
            String filter = parameterValue(exchange.getRequestURI().getRawQuery(), "filter");
            if ("today".equals(filter) || "tomorrow".equals(filter) || "overdue".equals(filter)) {
                return filter;
            }
        } catch (IllegalArgumentException e) {
            // 不正な指定は全件表示に戻します。
        }
        return "all";
    }

    private String parameterValue(String encoded, String name) {
        if (encoded == null) {
            return null;
        }
        for (String part : encoded.split("&")) {
            int equals = part.indexOf('=');
            String key = equals < 0 ? part : part.substring(0, equals);
            if (name.equals(URLDecoder.decode(key, StandardCharsets.UTF_8))) {
                String value = equals < 0 ? "" : part.substring(equals + 1);
                return URLDecoder.decode(value, StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private void sendJson(HttpExchange exchange, int status, String message) throws IOException { // JSONを返します。
        byte[] body = message.getBytes(StandardCharsets.UTF_8); // 応答をUTF-8のバイト列にします。
        exchange.getResponseHeaders().set("Content-Type", "application/json"); // charsetなしで種類を指定します。
        exchange.sendResponseHeaders(status, body.length); // 状態とバイト数を送ります。
        try (var response = exchange.getResponseBody()) { // 応答の書き込み先を閉じられるようにします。
            response.write(body); // JSON本文を書き込みます。
        } // 応答を閉じます。
    } // JSON応答処理を終えます。

    private void redirect(HttpExchange exchange) throws IOException { // ★ 操作後に一覧へ戻します。
        redirect(exchange, "all");
    }

    private void redirect(HttpExchange exchange, String filter) throws IOException {
        String location = "all".equals(filter) ? "/" : "/?filter=" + filter;
        exchange.getResponseHeaders().set("Location", location);
        exchange.sendResponseHeaders(303, -1); // ★ ブラウザに再表示を指示します。
        exchange.close(); // ★ 通信を閉じます。
    }

    private void send(HttpExchange exchange, int status, String type, String message) throws IOException { // ★
                                                                                                                  // 応答を返します。
        byte[] body = message.getBytes(StandardCharsets.UTF_8); // ★ UTF-8に変換します。
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8"); // ★ 応答の種類を示します。
        exchange.sendResponseHeaders(status, body.length); // ★ 状態と長さを送ります。
        try (var response = exchange.getResponseBody()) { // ★ 通信を自動で閉じます。
            response.write(body); // ★ 本文を送ります。
        }
    }
}
