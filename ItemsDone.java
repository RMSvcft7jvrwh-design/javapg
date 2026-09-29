public class ItemsDone { // クラス（プログラムをまとめる入れ物）を定義します
    public static void main(String[] args) { // main（実行開始地点）を定義します
        String[] todos = {"牛乳を買う", "卵を買う", "パンを買う", "部屋を掃除する"}; // Todoの配列（複数の値を入れる箱）を作ります
        boolean[] done = {true, false, false, false}; // boolean（真または偽）の配列で済んだかを管理します
        for (int i = 0; i < todos.length; i++) { // for文（繰り返し）でTodoを1件ずつ取り出します
            if (done[i]) { // doneがtrue（済み）かを確認します
                System.out.println("<li>[済] " + todos[i] + "</li>"); // 済んだTodoに[済]を付けて表示します
            } else { // doneがfalse（未完了）の場合はこちらを実行します
                System.out.println("<li>" + todos[i] + "</li>"); // 未完了のTodoを表示します
            } // ifとelseの分岐を終了します
        } // for文を終了します
    } // mainメソッドを終了します
} // クラスを終了します
