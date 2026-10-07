import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    void insertTodo(String title) throws SQLException { // 追加時は未着手にします。
        String sql = "INSERT INTO todos (title, status) VALUES (?, ?)"; // IDはSQLiteに採番させます。
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) { // ★
                                                                                                                  // 値を埋め込まずに渡します。
            statement.setString(1, title); // ★ 1番目の?にタイトルを設定します。
            statement.setString(2, TodoStatus.NOT_STARTED.name()); // 新しいTodoの状態です。
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

    List<DbTodo> selectTodos() throws SQLException { // ★ 一覧は毎回SELECTで取得します。
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
}
