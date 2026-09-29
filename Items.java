public class Items { // クラス（プログラムをまとめる入れ物）を定義します
    public static void main(String[] args) { // main（実行開始地点）を定義します
        String[] todos = {"牛乳を買う", "卵を買う", "パンを買う"}; // 配列（複数の値を入れる箱）を作ります
        for (int i = 0; i < todos.length; i++) { // for文（繰り返し）でTodoを1件ずつ取り出します
            System.out.println("<li>" + todos[i] + "</li>"); // Todoを<li>と</li>で囲んで表示します
        } // for文を終了します
    } // mainメソッドを終了します
} // クラスを終了します
