import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int port;
    private final List<String> validPaths;
    private final ExecutorService threadPool;
    private volatile boolean isRunning;

    public Server(int port, List<String> validPaths) {
        this.port = port;
        this.validPaths = validPaths;
        this.threadPool = Executors.newFixedThreadPool(64);
        this.isRunning = false;
    }

    public void start() {
        isRunning = true;
        try (final var serverSocket = new ServerSocket(port)){
            System.out.println("Сервер запущен на порту " + port);

            while (isRunning) {
                final var Socket = serverSocket.accept();

            }

        } catch (IOException e) {

        }

    }

    public void stop() {
        isRunning = false;
        threadPool.shutdown();
    }


}
