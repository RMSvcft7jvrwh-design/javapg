import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class App {
    public static void main(String[] args) throws Exception {
        TodoRepository repository = new TodoRepository();
        repository.createTable();

        TodoView view = new TodoView();
        TodoHandler handler = new TodoHandler(repository, view);

        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);
        server.createContext("/", handler::handle);
        server.createContext("/api/todos", handler::handleApiTodos);
        server.createContext("/style.css", handler::handleStyle);
        server.start();
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）");
    }
}
