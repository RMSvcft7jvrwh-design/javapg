import java.util.List;

class TodoJson {
    static String todosJson(List<DbTodo> todos) { // 渡されたTodo一覧をJSON文字列にします。
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

    private static String escapeJson(String value) { // タイトル内の特殊文字をエスケープします。
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
