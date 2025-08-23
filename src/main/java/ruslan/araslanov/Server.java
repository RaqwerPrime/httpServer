package ruslan.araslanov;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;


public class Server {
    private final ExecutorService threadPool;
    private volatile boolean isRunning;
    private final Map<String, Map<String, Handler>> handlers;

    public Server() {
        this.threadPool = Executors.newFixedThreadPool(64);
        this.isRunning = false;
        this.handlers = new ConcurrentHashMap<>();
    }

    public void addHandler(String method, String path, Handler handler) {
        handlers.computeIfAbsent(method, k -> new ConcurrentHashMap<>()).put(path, handler);
    }

    public void listen(int port) {
        isRunning = true;
        try (final var serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту " + port);

            while (isRunning) {
                try {
                    final var socket = serverSocket.accept();
                    threadPool.submit(() -> handleConnection(socket));
                } catch (IOException e) {
                    if (isRunning) {
                        System.out.println("Ошибка принятия подключения: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Не удалось запустить сервер на порту " + port + ": " + e.getMessage());
        }
    }

    public void stop() {
        isRunning = false;
        threadPool.shutdown();
    }

    private void handleConnection(Socket socket) {
        try (final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream())) {

            final var request = parseRequest(in);
            if (request != null) {
                processRequest(request, out);
            }

        } catch (IOException e) {
            System.out.println("Ошибка при обработке подключения: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Ошибка закрытия сокета: " + e.getMessage());
            }
        }
    }

    private Request parseRequest(BufferedReader in) throws IOException {
        final var requestLine = in.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            return null;
        }

        final var parts = requestLine.split(" ");
        if (parts.length != 3) {
            return null;
        }

        final var method = parts[0];
        final var fullPath = parts[1];

        String path;
        String queryString = null;
        int queryIndex = fullPath.indexOf('?');
        if (queryIndex != -1) {
            path = fullPath.substring(0, queryIndex);
            queryString = fullPath.substring(queryIndex + 1);
        } else {
            path = fullPath;
        }

        final var headers = new HashMap<String, String>();

        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            final var headerParts = line.split(":", 2);
            if (headerParts.length == 2) {
                headers.put(headerParts[0].trim(), headerParts[1].trim());
            }
        }

        String body = "";
        if (headers.containsKey("Content-Length")) {
            try {
                final int contentLength = Integer.parseInt(headers.get("Content-Length"));
                if (contentLength > 0) {
                    char[] bodyBuffer = new char[contentLength];
                    in.read(bodyBuffer, 0, contentLength);
                    body = new String(bodyBuffer);
                }
            } catch (NumberFormatException e) {
                System.out.println("Неверный формат Content-Length: " + e.getMessage());
            }
        }

        return new Request(method, path, queryString, headers, body);
    }

    private void processRequest(Request request, BufferedOutputStream out) throws IOException {
        final var methodHandlers = handlers.get(request.getMethod());
        if (methodHandlers == null) {
            sendErrorResponse(out, 405, "Method Not Allowed");
            return;
        }

        final var handler = methodHandlers.get(request.getPath());
        if (handler == null) {
            sendErrorResponse(out, 404, "Not Found");
            return;
        }

        handler.handle(request, out);
    }

    private void sendErrorResponse(BufferedOutputStream out, int statusCode, String statusMessage) throws IOException {
        String response = buildHeaders(statusCode, statusMessage, "text/plain", 0);
        out.write(response.getBytes());
        out.flush();
    }

    String buildHeaders(int statusCode, String statusMessage, String contentType, long contentLength) {
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
        private final String queryString;
        private Map<String, List<String>> queryParams;

        public Request(String method, String path, String queryString, Map<String, String> headers, String body) {
            this.method = method;
            this.path = path;
            this.queryString = queryString;
            this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
            this.body = body;
            this.queryParams = parseQueryParams(queryString);
        }

        private static Map<String, List<String>> parseQueryParams(String queryString) {
            Map<String, List<String>> params = new HashMap<>();

            if (queryString != null && !queryString.isEmpty()) {
                try {
                    List<NameValuePair> pairs = URLEncodedUtils.parse(
                            "?" + queryString,
                            StandardCharsets.UTF_8
                    );

                    for (NameValuePair pair : pairs) {
                        String name = URLDecoder.decode(pair.getName(), StandardCharsets.UTF_8);
                        String value = URLDecoder.decode(pair.getValue(), StandardCharsets.UTF_8);

                        params.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
                    }
                } catch (Exception e) {
                    System.out.println("Ошибка парсинга query параметров: " + e.getMessage());
                }
            }
            return Collections.unmodifiableMap(params);
        }

        public String getMethod() {
            return method;
        }

        public String getPath() {
            return path;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public String getBody() {
            return body;
        }

        public String getHeader(String name) {
            return headers.get(name);
        }

        public String getQueryParam(String name) {
            List<String> values = queryParams.get(name);
            return (values != null && !values.isEmpty()) ? values.get(0) : null;
        }

        public List<String> getQueryParams(String name) {
            return queryParams.getOrDefault(name, Collections.emptyList());
        }

        public Map<String, List<String>> getQueryParams() {
            return queryParams;
        }

    }


    @FunctionalInterface
    public interface Handler {
        void handle(Request request, BufferedOutputStream responseStream) throws IOException;
    }
}

