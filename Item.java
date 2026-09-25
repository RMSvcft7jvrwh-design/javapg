public class Item { // Itemという名前のクラス（プログラムの入れ物）を作ります
    public static void main(String[] args) { // mainメソッド（プログラムの開始地点）を作ります
        String title = "Javaの復習"; // titleという文字列（文字を入れる変数）を作ります
        String html = "<li>" + title + "</li>"; // +で文字列をつなぎ、1行分のHTMLを作ります
        System.out.println(html); // 作ったHTMLをターミナル（文字が表示される画面）に出します
        boolean done = false; // doneというboolean（はい／いいえを持つ変数）をfalse（いいえ）で作ります
        System.out.println(done); // doneの値をターミナル（文字が表示される画面）に出します
        int count = 3; // countというint（整数を入れる変数）を作り、3を入れます
        System.out.println("いま" + count + "件"); // +で文字列と整数をつなぎ、「いま3件」と表示します
    } // mainメソッドを終わります
} // Itemクラスを終わります
