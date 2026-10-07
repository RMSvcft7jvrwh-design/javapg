import java.time.LocalDate;

class DbTodo { // SQLiteから読み込んだTodo1件を表します。
    private final int id;
    private final String title;
    private final LocalDate dueDate; // 予定日を持たないTodoはnullです。
    private final TodoStatus status; // DBから読み取った進行状態を保持します。
    private final Long startedAt; // 開始時刻をミリ秒で保持します。未開始ならnullです。
    private final Long completedAt; // 完了時刻をミリ秒で保持します。未完了ならnullです。

    DbTodo(int id, String title, LocalDate dueDate, TodoStatus status, Long startedAt, Long completedAt) {
        this.id = id;
        this.title = title;
        this.dueDate = dueDate;
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

    LocalDate getDueDate() {
        return dueDate;
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
