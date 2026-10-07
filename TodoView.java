import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

class TodoView {
    private static final DateTimeFormatter START_FORMAT = DateTimeFormatter
            .ofPattern("yyyy/MM/dd HH:mm").withZone(ZoneId.systemDefault());

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
