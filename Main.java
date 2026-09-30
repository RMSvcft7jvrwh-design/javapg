import java.util.ArrayList; // ArrayList（複数のデータを順番に入れる箱）を使えるようにします
import java.util.List; // List（一覧）を使えるようにします

class Todo { // Todoクラス（Todo1件の設計図）を定義します
    private String title; // title（題名）を保存します
    private boolean done; // done（済んだかどうか）を保存します

    Todo(String title, boolean done) { // Todoを作るためのコンストラクタ（初期設定）を定義します
        this.title = title; // 受け取った題名を保存します
        this.done = done; // 受け取った済み状態を保存します
    } // コンストラクタを終了します

    String toItem() { // Todoをli要素の文字列に変えるメソッド（処理）を定義します
        if (done) { // doneがtrue（済み）かを確認します
            return "<li>[済] " + title + "</li>"; // 済みのTodoを文字列として返します
        } else { // doneがfalse（未完了）の場合はこちらを実行します
            return "<li>" + title + "</li>"; // 未完了のTodoを文字列として返します
        } // ifとelseの分岐を終了します
    } // toItemメソッドを終了します
} // Todoクラスを終了します

public class Main { // Mainクラス（実行するプログラム）を定義します
    public static void main(String[] args) { // main（実行開始地点）を定義します
        List<Todo> todos = new ArrayList<>(); // Todoの一覧を作ります
        todos.add(new Todo("牛乳を買う", false)); // 未完了のTodoを1件追加します
        todos.add(new Todo("ゴミを出す", true)); // 済みのTodoを1件追加します
        for (Todo todo : todos) { // for文（繰り返し）でListのTodoを1件ずつ取り出します
            System.out.println(todo.toItem()); // toItemの結果を1行ずつ表示します
        } // for文を終了します
    } // mainメソッドを終了します
} // Mainクラスを終了します
