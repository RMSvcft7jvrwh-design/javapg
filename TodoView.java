import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

class TodoView {
    private static final DateTimeFormatter START_FORMAT = DateTimeFormatter
            .ofPattern("yyyy/MM/dd HH:mm").withZone(ZoneId.systemDefault());
    private static final String PAGE_STYLE = "<style>"
            + "*{box-sizing:border-box}"
            + "body{margin:0;background:#f5f7fb;color:#263346;"
            + "font:16px/1.5 -apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif}"
            + ".page{max-width:1040px;margin:0 auto;padding:32px 20px 56px}"
            + "h1,h2,p{margin-top:0}"
            + "h1{font-size:1.8rem;letter-spacing:.02em;margin-bottom:4px}"
            + "h2{font-size:1.15rem;margin:0}"
            + ".subtitle{color:#617084;margin-bottom:0}"
            + ".summary{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));"
            + "gap:12px;margin:24px 0}"
            + ".summary-card,.panel{background:#fff;border:1px solid #e1e7ef;"
            + "border-radius:12px;box-shadow:0 2px 8px #1d35570a}"
            + ".summary-card{padding:14px 18px}"
            + ".summary-label{display:block;color:#617084;font-size:.85rem}"
            + ".summary-value{display:block;font-size:1.5rem;font-weight:700;line-height:1.3}"
            + ".panel{padding:20px;margin-bottom:18px}"
            + ".add-form label{display:block;font-weight:600;margin-bottom:10px}"
            + ".add-controls{display:flex;gap:10px}"
            + ".add-controls input{flex:1;min-width:0;padding:10px 12px;border:1px solid #b9c5d4;"
            + "border-radius:8px;font:inherit}"
            + ".add-controls input:focus-visible,.button:focus-visible{outline:3px solid #a9c9ff;"
            + "outline-offset:2px}"
            + ".button{display:inline-flex;align-items:center;justify-content:center;min-height:40px;"
            + "padding:8px 14px;border:1px solid #c9d3df;border-radius:8px;background:#fff;"
            + "color:#263346;font:inherit;font-weight:600;text-decoration:none;cursor:pointer;"
            + "white-space:nowrap}"
            + ".button:hover{background:#f1f5fa}"
            + ".button--primary{background:#2458a6;border-color:#2458a6;color:#fff}"
            + ".button--primary:hover{background:#1a478d}"
            + ".button--danger{color:#a1333a;border-color:#e7b8bc}"
            + ".button--danger:hover{background:#fff1f2}"
            + ".button--small{min-height:34px;padding:5px 10px;font-size:.9rem}"
            + ".button:disabled{opacity:.5;cursor:not-allowed}"
            + ".list-header{display:flex;align-items:center;justify-content:space-between;"
            + "gap:12px;margin-bottom:16px}"
            + ".table-wrap{width:100%;overflow-x:auto}"
            + "table{width:100%;min-width:700px;border-collapse:collapse}"
            + "th,td{padding:12px 10px;text-align:left;vertical-align:middle}"
            + "th{background:#f4f7fb;color:#506176;font-size:.85rem;white-space:nowrap}"
            + "tbody tr+tr td{border-top:1px solid #e9edf3}"
            + "tbody tr:hover{background:#fafcff}"
            + ".title-cell{min-width:170px;font-weight:600;overflow-wrap:anywhere}"
            + ".working{background:#fff3cf;padding:2px 4px;border-radius:4px}"
            + ".start-time,.timer{white-space:nowrap;color:#536276}"
            + ".timer{font-variant-numeric:tabular-nums}"
            + ".state-cell{min-width:190px}"
            + ".row-actions{display:flex;align-items:center;gap:8px}"
            + ".status{display:inline-flex;padding:4px 9px;border-radius:999px;"
            + "font-size:.8rem;font-weight:700;white-space:nowrap}"
            + ".status--new{background:#edf1f6;color:#44546a}"
            + ".status--working{background:#fff1cc;color:#775600}"
            + ".status--done{background:#dcf5e7;color:#17633b}"
            + ".action-form{margin:0}"
            + ".empty-state{text-align:center;color:#617084;padding:28px 10px}"
            + "@media(max-width:640px){.page{padding:20px 14px 40px}"
            + "h1{font-size:1.5rem}.summary{grid-template-columns:repeat(2,minmax(0,1fr));"
            + "gap:8px;margin:18px 0}.summary-card{padding:12px}"
            + ".panel{padding:14px}.add-controls{flex-direction:column}"
            + ".list-header{align-items:stretch;flex-direction:column}"
            + ".list-header .button{width:100%}}"
            + "</style>";

    String page(List<DbTodo> todos) { // 渡されたTodo一覧からHTMLを作ります。
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
                "<!doctype html><html lang='ja'><head><meta charset='UTF-8'>"
                        + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                        + "<title>Todoリスト</title>");
        html.append(PAGE_STYLE).append("</head><body><main class='page'>")
                .append("<header><h1>Todoリスト</h1>")
                .append("<p class='subtitle'>やることと進み具合をまとめて確認できます。</p></header>");
        html.append("<section class='summary' aria-label='Todoの件数'>")
                .append("<div class='summary-card'><span class='summary-label'>合計</span>")
                .append("<span class='summary-value'>").append(todos.size()).append("</span></div>")
                .append("<div class='summary-card'><span class='summary-label'>未着手</span>")
                .append("<span class='summary-value'>").append(notStartedCount).append("</span></div>")
                .append("<div class='summary-card'><span class='summary-label'>作業中</span>")
                .append("<span class='summary-value'>").append(inProgressCount).append("</span></div>")
                .append("<div class='summary-card'><span class='summary-label'>完了</span>")
                .append("<span class='summary-value'>").append(doneCount).append("</span></div></section>");
        html.append("<section class='panel'><form class='add-form' method='post' action='/add'>")
                .append("<label for='todo-title'>新しいTodo</label><div class='add-controls'>")
                .append("<input id='todo-title' name='todo' placeholder='やることを入力'>")
                .append("<button class='button button--primary' type='submit'>追加</button>")
                .append("</div></form></section>");
        html.append("<section class='panel'><div class='list-header'><h2>一覧</h2>")
                .append("<form method='post' action='/delete-completed' ")
                .append("onsubmit=\"return confirm('完了済みのTodoをすべて削除しますか？')\">")
                .append("<button class='button button--danger' type='submit'");
        if (doneCount == 0) {
            html.append(" disabled");
        }
        html.append(">完了済みを一括削除</button></form></div>");
        html.append("<div class='table-wrap'><table><thead><tr>")
                .append("<th scope='col'>開始日時</th><th scope='col'>Todo</th>")
                .append("<th scope='col'>経過時間</th><th scope='col'>状態・操作</th>")
                .append("<th scope='col'>削除</th></tr></thead><tbody>");
        if (todos.isEmpty()) {
            html.append("<tr><td class='empty-state' colspan='5'>Todoはまだありません。</td></tr>");
        }
        for (DbTodo todo : todos) {
            html.append("<tr><td class='start-time'>");
            if (todo.getStartedAt() != null) {
                html.append(START_FORMAT.format(Instant.ofEpochMilli(todo.getStartedAt())));
            } else {
                html.append("—");
            }
            html.append("</td><td class='title-cell'>");
            if (todo.getStatus() == TodoStatus.IN_PROGRESS) {
                html.append("<span class='working'>");
            }
            html.append(escapeHtml(todo.getTitle()));
            if (todo.getStatus() == TodoStatus.IN_PROGRESS) {
                html.append("</span>");
            }
            html.append("</td><td class='timer'");
            if (todo.getStatus() == TodoStatus.IN_PROGRESS && todo.getStartedAt() != null) {
                html.append(" data-start='").append(todo.getStartedAt()).append("'");
            }
            html.append(">");
            if (todo.getStartedAt() != null) {
                if (todo.getStatus() == TodoStatus.IN_PROGRESS) {
                    html.append(formatElapsed(pageNow - todo.getStartedAt()));
                } else if (todo.getStatus() == TodoStatus.DONE && todo.getCompletedAt() != null) {
                    html.append(formatElapsed(todo.getCompletedAt() - todo.getStartedAt()));
                }
            } else {
                html.append("—");
            }
            html.append("</td><td class='state-cell'><div class='row-actions'>");
            if (todo.getStatus() == TodoStatus.NOT_STARTED) {
                html.append("<span class='status status--new'>未着手</span>");
                html.append("<form class='action-form' method='post' action='/start?id=")
                        .append(todo.getId())
                        .append("'><button class='button button--primary button--small' type='submit'>開始</button></form>");
            } else if (todo.getStatus() == TodoStatus.IN_PROGRESS) {
                html.append("<span class='status status--working'>作業中</span>");
                html.append("<form class='action-form' method='post' action='/done?id=")
                        .append(todo.getId())
                        .append("'><button class='button button--primary button--small' type='submit'>完了</button></form>");
            } else {
                html.append("<span class='status status--done'>完了</span>");
            }
            html.append("</div></td><td><a class='button button--danger button--small' href='/delete?id=")
                    .append(todo.getId()).append("'>削除</a></td></tr>");
        }
        html.append("</tbody></table></div></section></main><script>const serverNow=").append(pageNow)
                .append(";const openedAt=performance.now();")
                .append("function updateTimers(){const now=serverNow+(performance.now()-openedAt);")
                .append("document.querySelectorAll('.timer[data-start]').forEach(el=>{")
                .append("const seconds=Math.max(0,Math.floor((now-Number(el.dataset.start))/1000));")
                .append("const hours=Math.floor(seconds/3600);")
                .append("const minutes=Math.floor(seconds%3600/60);const rest=seconds%60;")
                .append("el.textContent=String(hours).padStart(2,'0')+':'")
                .append("+String(minutes).padStart(2,'0')+':'")
                .append("+String(rest).padStart(2,'0');});}")
                .append("updateTimers();setInterval(updateTimers,1000);</script></body></html>");
        return html.toString();
    }

    private String formatElapsed(long elapsedMillis) { // 経過時間を時:分:秒にします。
        long seconds = Math.max(0, elapsedMillis / 1000);
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds / 60) % 60, seconds % 60);
    }

    private String escapeHtml(String value) { // ★ タイトルをHTMLとして解釈させません。
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;"); // ★ 特殊文字を置き換えます。
    }

    String todosJson(List<DbTodo> todos) { // 渡されたTodo一覧をJSON文字列にします。
        StringBuilder json = new StringBuilder("["); // JSON配列を開始します。
        boolean first = true; // 最初の要素かどうかを覚えます。
        for (DbTodo todo : todos) { // 渡されたTodoを1件ずつ読み出します。
            if (!first) { // 2件目以降か確認します。
                json.append(','); // 要素間にカンマを入れます。
            } // カンマの処理を終えます。
            json.append("{\"title\":\"").append(escapeJson(todo.getTitle())) // タイトルをJSON用に変換します。
                    .append("\",\"done\":").append(todo.isDone()).append('}'); // 完了状態を真偽値で加えます。
            first = false; // 次の要素からカンマが必要です。
        } // 全Todoの処理を終えます。
        return json.append(']').toString(); // JSON配列を閉じます。
    } // JSON文字列の作成を終えます。

    private String escapeJson(String value) { // タイトル内の特殊文字をエスケープします。
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
}
