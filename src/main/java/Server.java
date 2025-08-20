import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
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

    private void Connection(Socket socket) {
        try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final  var out = new BufferedOutputStream(socket.getOutputStream())) {


        } catch (IOException e) {

        }



    }
    private void proccesRequest(BufferedReader in, BufferedOutputStream out) {

    }

    private void servFile(String path, BufferedOutputStream out) {


    }

    private void servStatFile(Path filePath, String mimeType, BufferedOutputStream out) {

    }


}
