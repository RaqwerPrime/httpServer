package ruslan.araslanov;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;

public class Server {
    private final Map<String, Map<String, Handler>> handlers;
    private final ExecutorService threadPool;
    private volatile boolean isRunning;

    public Server() {
        this.handlers = new ConcurrentHashMap<>();
        this.threadPool = Executors.newFixedThreadPool(64);
        this.isRunning = false;
    }

    public void addHandler(String method, String path , Handler handler) {
        handlers.computeIfAbsent(method, k -> new ConcurrentHashMap<>()).put(path, handler);

    }


    public void listen(int port) {
        isRunning = true;
        try (final var serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту " + port);

            while (isRunning) {
                try {
                    final var socket = serverSocket.accept();
                    threadPool.submit(() -> Connection(socket));
                } catch (IOException e) {
                    if (isRunning) {
                        System.out.println("Ошибка принятия подключения " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Не удалось запустить сервер на порту " + port + " " + e.getMessage());

        }

    }

    public void stop() {
        isRunning = false;
        threadPool.shutdown();
    }

    private void handleConnection(Socket socket) {
        try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream())) {
            proccesRequest(in, out);

        } catch (IOException e) {
            System.out.println("Ошибка при обработке подключения " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Ошибка закрытия сокета" + e.getMessage());
            }
        }

    }

    private void proccesRequest(BufferedReader in, BufferedOutputStream out) throws IOException {
        final var requestLine = in.readLine();
        if (requestLine == null) {
            return;
        }

        final var parts = requestLine.split(" ");
        if (parts.length != 3) {
            sendErrorResponse(out, 400, "Bad Request");
            return;
        }

        final var method = parts[0];
        final var path = parts[1];

        if (!"GET".equals(method)) {
            sendErrorResponse(out, 405, "Method Not Allowed");
            return;
        }

        if (!validPaths.contains(path)) {
            sendErrorResponse(out, 404, "Not Found");
            return;
        }

        servFile(path, out);

    }

    private void servFile(String path, BufferedOutputStream out) throws IOException {
        final var filePath = Path.of(".", "public", path);

        if (!Files.exists(filePath)) {
            sendErrorResponse(out, 404, "Not Found");
            return;
        }

        final var mimeType = Files.probeContentType(filePath);

        if ("/classic.html".equals(path)) {
            servDynamicContent(filePath, mimeType, out);
        } else {
            servStatFile(filePath, mimeType, out);
        }

    }

    private void servStatFile(Path filePath, String mimeType, BufferedOutputStream out) throws IOException {
        final var length = Files.size(filePath);
        final var headers = buildHeaders(200, "OK", mimeType, length);

        out.write(headers.getBytes());
        Files.copy(filePath, out);
        out.flush();

    }

    private void servDynamicContent(Path filePath, String mimeType, BufferedOutputStream out) throws IOException {
        final var template = Files.readString(filePath);
        final var content = template.replace("{time}", LocalDateTime.now().toString()).getBytes();
        final var headers = buildHeaders(200, "OK", mimeType, content.length);

        out.write(headers.getBytes());
        out.write(content);
        out.flush();

    }

    private void sendErrorResponse(BufferedOutputStream out, int statusCode, String statusMessage) throws IOException {
        final var response = buildHeaders(statusCode, statusMessage, "text/plain", 0);
        out.write(response.getBytes());
        out.flush();
    }

    private String buildHeaders(int statusCode, String statusMessage, String contentType, long contentLength) {
        return String.format(
                "HTTP/1.1 %d %s\r\n" +
                        "Content-Type: %s\r\n" +
                        "Content-Length: %d\r\n" +
                        "Connection: close\r\n" +
                        "\r\n",
                statusCode, statusMessage, contentType, contentLength
        );
    }
    public static class Request {
        private final String method;
        private final String path;
        private final Map<String, String> headers;
        private final String body;

        public Request(String method, String path, Map<String, String> headers, String body) {
            this.method = method;
            this.path = path;
            this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
            this.body = body;
        }
    }

}

