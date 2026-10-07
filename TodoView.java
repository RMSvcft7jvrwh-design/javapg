import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

class TodoView {
    private static final DateTimeFormatter START_FORMAT = DateTimeFormatter
            .ofPattern("yyyy/MM/dd HH:mm").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DUE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter
            .ofPattern("yyyy/MM/dd HH:mm:ss").withZone(ZoneId.systemDefault());

    String page(List<DbTodo> todos, String filter, LocalDate today) {
        long pageNow = System.currentTimeMillis(); // 画面を作った時刻をタイマーの基準にします。
        String listTitle = "すべてのTodo";
        if ("today".equals(filter)) {
            listTitle = "今日のTodo";
        } else if ("tomorrow".equals(filter)) {
            listTitle = "明日のTodo";
        } else if ("overdue".equals(filter)) {
            listTitle = "期限切れのTodo";
        }
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
                        + "<title>今日のTodo</title>");
        html.append("<link rel='stylesheet' href='/style.css'>")
                .append("</head><body><main class='page'>")
                .append("<header class='page-header'><div><h1>今日のTodo</h1>")
                .append("<p class='subtitle'>予定日と進み具合をまとめて確認できます。</p></div>")
                .append("<time class='clock' id='current-time'>現在時刻 ")
                .append(CLOCK_FORMAT.format(Instant.ofEpochMilli(pageNow)))
                .append("</time><svg class='kasumi' viewBox='0 0 380 96' preserveAspectRatio='xMaxYMax meet' ")
                .append("aria-hidden='true' focusable='false'><g fill='none' stroke='currentColor' ")
                .append("stroke-width='2' stroke-linecap='square' stroke-linejoin='miter'>")
                .append("<path d='M8 25h72V13h78v12h52v11h72v10h90'/>")
                .append("<path d='M42 47h70V35h84v12h58v12h106'/>")
                .append("<path d='M0 70h90V58h76v12h82v12h114'/>")
                .append("</g></svg></header>");
        html.append("<section class='summary' aria-label='Todoの件数'>")
                .append("<div class='summary-card'><span class='summary-label'>表示中</span>")
                .append("<span class='summary-value'>").append(todos.size()).append("</span></div>")
                .append("<div class='summary-card'><span class='summary-label'>未着手</span>")
                .append("<span class='summary-value'>").append(notStartedCount).append("</span></div>")
                .append("<div class='summary-card'><span class='summary-label'>作業中</span>")
                .append("<span class='summary-value'>").append(inProgressCount).append("</span></div>")
                .append("<div class='summary-card'><span class='summary-label'>完了</span>")
                .append("<span class='summary-value'>").append(doneCount).append("</span></div></section>");
        html.append("<section class='panel'><form class='add-form' method='post' action='/add'>")
                .append("<label for='todo-title'>新しいTodo</label><div class='add-controls'>")
                .append("<input id='todo-title' name='todo' placeholder='やることを入力' autocomplete='off'>")
                .append("<label class='due-field' for='due-date'><span>予定日（空欄なら今日）</span>")
                .append("<input id='due-date' name='dueDate' type='date'></label>")
                .append("<button class='button button--primary' type='submit'>追加</button>")
                .append("</div></form></section>");
        html.append("<section class='panel'><div class='list-header'><h2>")
                .append(listTitle).append("</h2>");
        if ("all".equals(filter)) {
            html.append("<form method='post' action='/delete-completed' ")
                    .append("onsubmit=\"return confirm('完了済みのTodoをすべて削除しますか？')\">")
                    .append("<button class='button button--danger' type='submit'");
            if (doneCount == 0) {
                html.append(" disabled");
            }
            html.append(">完了済みを一括削除</button></form>");
        }
        html.append("</div><nav class='filters' aria-label='予定日で絞り込み'>");
        appendFilterLink(html, "all", "すべて", filter);
        appendFilterLink(html, "today", "今日", filter);
        appendFilterLink(html, "tomorrow", "明日", filter);
        appendFilterLink(html, "overdue", "期限切れ", filter);
        html.append("</nav>");
        html.append("<div class='table-wrap'><table><thead><tr>")
                .append("<th scope='col'>開始日時</th><th scope='col'>Todo</th>")
                .append("<th scope='col'>予定日</th><th scope='col'>経過時間</th>")
                .append("<th scope='col'>状態・操作</th>")
                .append("<th scope='col'>削除</th></tr></thead><tbody>");
        if (todos.isEmpty()) {
            html.append("<tr><td class='empty-state' colspan='6'>")
                    .append("all".equals(filter) ? "Todoはまだありません。" : "この条件のTodoはありません。")
                    .append("</td></tr>");
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
            html.append("</td><td class='due-date");
            if (todo.getDueDate() != null && todo.getDueDate().isBefore(today) && !todo.isDone()) {
                html.append(" due-date--overdue");
            }
            html.append("'>");
            if (todo.getDueDate() == null) {
                html.append("—");
            } else {
                html.append(DUE_FORMAT.format(todo.getDueDate()));
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
                        .append(todo.getId()).append("&amp;filter=").append(filter)
                        .append("'><button class='button button--primary button--small' type='submit'>開始</button></form>");
            } else if (todo.getStatus() == TodoStatus.IN_PROGRESS) {
                html.append("<span class='status status--working'>作業中</span>");
                html.append("<form class='action-form' method='post' action='/done?id=")
                        .append(todo.getId()).append("&amp;filter=").append(filter)
                        .append("'><button class='button button--primary button--small' type='submit'>完了</button></form>");
            } else {
                html.append("<span class='status status--done'>完了</span>");
            }
            html.append("</div></td><td><a class='button button--danger button--small' href='/delete?id=")
                    .append(todo.getId()).append("&amp;filter=").append(filter)
                    .append("'>削除</a></td></tr>");
        }
        html.append("</tbody></table></div></section></main><script>const serverNow=").append(pageNow)
                .append(";const openedAt=performance.now();")
                .append("const renderedDay='").append(today).append("';")
                .append("function localDay(date){return date.getFullYear()+'-'")
                .append("+String(date.getMonth()+1).padStart(2,'0')+'-'")
                .append("+String(date.getDate()).padStart(2,'0');}")
                .append("function updateClock(){const now=new Date();")
                .append("document.getElementById('current-time').textContent='現在時刻 '+now.toLocaleString('ja-JP');")
                .append("if(localDay(now)!==renderedDay){location.reload();}}")
                .append("function updateTimers(){const now=serverNow+(performance.now()-openedAt);")
                .append("document.querySelectorAll('.timer[data-start]').forEach(el=>{")
                .append("const seconds=Math.max(0,Math.floor((now-Number(el.dataset.start))/1000));")
                .append("const hours=Math.floor(seconds/3600);")
                .append("const minutes=Math.floor(seconds%3600/60);const rest=seconds%60;")
                .append("el.textContent=String(hours).padStart(2,'0')+':'")
                .append("+String(minutes).padStart(2,'0')+':'")
                .append("+String(rest).padStart(2,'0');});}")
                .append("updateClock();updateTimers();")
                .append("setInterval(()=>{updateClock();updateTimers();},1000);</script></body></html>");
        return html.toString();
    }

    private void appendFilterLink(StringBuilder html, String value, String label, String selected) {
        html.append("<a class='filter");
        if (value.equals(selected)) {
            html.append(" filter--active");
        }
        html.append("' href='/?filter=").append(value).append("'");
        if (value.equals(selected)) {
            html.append(" aria-current='page'");
        }
        html.append(">").append(label).append("</a>");
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
}
