import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

class TodoRepository {
    private static final String DB_URL = "jdbc:sqlite:todos.db";

    private Connection connect() throws SQLException { // ★ SQLiteへの接続を開きます。
        return DriverManager.getConnection(DB_URL); // ★ jdbc:sqlite:todos.dbへ接続します。
    }

    void createTable() throws SQLException { // 表を作り、古いdone列から状態を引き継ぎます。
        try (Connection connection = connect()) {
            connection.setAutoCommit(false); // 移行中に失敗したら元へ戻せるようにします。
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS todos "
                        + "(id INTEGER PRIMARY KEY, title TEXT, "
                        + "status TEXT NOT NULL DEFAULT 'NOT_STARTED', "
                        + "started_at INTEGER, completed_at INTEGER, due_date TEXT)");
                boolean hasStatus = false;
                boolean hasStartedAt = false;
                boolean hasCompletedAt = false;
                boolean hasDueDate = false;
                try (ResultSet columns = statement.executeQuery("PRAGMA table_info(todos)")) {
                    while (columns.next()) {
                        String column = columns.getString("name");
                        if ("status".equals(column)) {
                            hasStatus = true;
                        } else if ("started_at".equals(column)) {
                            hasStartedAt = true;
                        } else if ("completed_at".equals(column)) {
                            hasCompletedAt = true;
                        } else if ("due_date".equals(column)) {
                            hasDueDate = true;
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
                if (!hasDueDate) {
                    statement.executeUpdate("ALTER TABLE todos ADD COLUMN due_date TEXT");
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback(); // 途中までの変更を残しません。
                throw e;
            }
        }
    }

    void insertTodo(String title, LocalDate dueDate) throws SQLException { // 予定日がなければnullのまま保存します。
        String sql = "INSERT INTO todos (title, status, due_date) VALUES (?, ?, ?)";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) { // ★
                                                                                                                  // 値を埋め込まずに渡します。
            statement.setString(1, title); // ★ 1番目の?にタイトルを設定します。
            statement.setString(2, TodoStatus.NOT_STARTED.name()); // 新しいTodoの状態です。
            statement.setString(3, dueDate == null ? null : dueDate.toString());
            statement.executeUpdate(); // ★ 1件追加します。
        }
    }

    void startTodo(int id) throws SQLException { // 未着手のTodoだけ開始します。
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

    void completeTodo(int id) throws SQLException { // 作業中のTodoだけ完了します。
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

    void deleteTodo(int id) throws SQLException { // ★ 削除はDELETEで行います。
        String sql = "DELETE FROM todos WHERE id = ?"; // ★ 指定したIDだけを削除します。
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) { // ★
                                                                                                                  // SQLを準備します。
            statement.setInt(1, id); // ★ 1番目の?にIDを設定します。
            statement.executeUpdate(); // ★ 1件削除します。
        }
    }

    void deleteCompletedTodos() throws SQLException { // 完了済みだけをまとめて削除します。
        String sql = "DELETE FROM todos WHERE status = ?";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, TodoStatus.DONE.name()); // 対象を完了状態に限定します。
            statement.executeUpdate(); // 条件に合うTodoを1回のSQLで削除します。
        }
    }

    List<DbTodo> selectTodos() throws SQLException {
        return selectTodos("all", LocalDate.now());
    }

    List<DbTodo> selectTodos(String filter, LocalDate today) throws SQLException {
        List<DbTodo> todos = new ArrayList<>(); // ★ 検索結果を入れる一覧です。
        String sql = "SELECT id, title, status, started_at, completed_at, due_date FROM todos";
        LocalDate dateParameter = null;
        boolean overdue = false;
        if ("today".equals(filter)) {
            sql += " WHERE due_date = ?";
            dateParameter = today;
        } else if ("tomorrow".equals(filter)) {
            sql += " WHERE due_date = ?";
            dateParameter = today.plusDays(1);
        } else if ("overdue".equals(filter)) {
            sql += " WHERE due_date < ? AND status <> ?";
            dateParameter = today;
            overdue = true;
        }
        sql += " ORDER BY id";
        try (Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (dateParameter != null) {
                statement.setString(1, dateParameter.toString());
            }
            if (overdue) {
                statement.setString(2, TodoStatus.DONE.name());
            }
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    String status = results.getString("status");
                    String dueDateText = results.getString("due_date");
                    long startValue = results.getLong("started_at");
                    Long startedAt = results.wasNull() ? null : startValue;
                    long completedValue = results.getLong("completed_at");
                    Long completedAt = results.wasNull() ? null : completedValue;
                    try {
                        LocalDate dueDate = dueDateText == null ? null : LocalDate.parse(dueDateText);
                        todos.add(new DbTodo(results.getInt("id"), results.getString("title"),
                                dueDate, TodoStatus.valueOf(status), startedAt, completedAt));
                    } catch (IllegalArgumentException | NullPointerException | DateTimeParseException e) {
                        throw new SQLException("不正なTodoの予定日または状態: " + dueDateText + ", " + status, e);
                    }
                }
            }
        }
        return todos; // ★ 画面表示に使う一覧を返します。
    }
}
