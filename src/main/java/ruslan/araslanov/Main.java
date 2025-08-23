package ruslan.araslanov;


import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        final var server = new Server();

        final List<String> validPaths = List.of(
                "/index.html", "/spring.svg", "/spring.png",
                "/resources.html", "/styles.css", "/app.js",
                "/links.html", "/forms.html", "/classic.html",
                "/events.html", "/events.js", "/treug.jpg"
        );

        for (String path : validPaths) {
            server.addHandler("GET", path, (request, out) -> {
                serveFile(path, out);
            });
        }


        server.addHandler("GET", "/messages", (request, out) -> {
            String lastParam = request.getQueryParam("last");
            String limitParam = request.getQueryParam("limit");
            String sortParam = request.getQueryParam("sort");

            StringBuilder responseBuilder = new StringBuilder();
            responseBuilder.append("GET messages handler\n");
            responseBuilder.append("Time: ").append(LocalDateTime.now()).append("\n");

            if (lastParam != null) {
                responseBuilder.append("Last messages: ").append(lastParam).append("\n");
            }

            if (limitParam != null) {
                responseBuilder.append("Limit: ").append(limitParam).append("\n");
            }

            if (sortParam != null) {
                responseBuilder.append("Sort by: ").append(sortParam).append("\n");
            }

            responseBuilder.append("All query params: ").append(request.getQueryParams()).append("\n");

            List<String> limitValues = request.getQueryParams("limit");
            if (!limitValues.isEmpty()) {
                responseBuilder.append("All limit values: ").append(limitValues).append("\n");
            }

            String response = responseBuilder.toString();
            String headers = buildHeaders(200, "OK", "text/plain", response.length());
            out.write(headers.getBytes());
            out.write(response.getBytes());
            out.flush();
        });

        server.addHandler("GET", "/search", (request, out) -> {
            String query = request.getQueryParam("q");
            String page = request.getQueryParam("page");
            String perPage = request.getQueryParam("per_page");

            String response;
            if (query != null) {
                response = "Результаты поиска для: '" + query + "'\n";
                if (page != null) {
                    response += "Страница: " + page + "\n";
                }
                if (perPage != null) {
                    response += "Результатов на странице: " + perPage + "\n";
                }
                response += "Все параметры: " + request.getQueryParams();
            } else {
                response = "Пожалуйста, укажите параметр q для поиска\n";
                response += "Пример: /search?q=java&page=1&per_page=10";
            }

            String headers = buildHeaders(200, "OK", "text/plain", response.length());
            out.write(headers.getBytes());
            out.write(response.getBytes());
            out.flush();
        });

        server.addHandler("GET", "/filter", (request, out) -> {
            List<String> categories = request.getQueryParams("category");
            List<String> tags = request.getQueryParams("tag");

            StringBuilder response = new StringBuilder();
            response.append("Фильтрация товаров:\n");

            if (!categories.isEmpty()) {
                response.append("Категории: ").append(categories).append("\n");
            }

            if (!tags.isEmpty()) {
                response.append("Теги: ").append(tags).append("\n");
            }

            response.append("Все параметры: ").append(request.getQueryParams());
            String headers = buildHeaders(200, "OK", "text/plain", response.length());
            out.write(headers.getBytes());
            out.write(response.toString().getBytes());
            out.flush();
        });


        server.addHandler("POST", "/messages", (request, out) -> {
            String userId = request.getQueryParam("user_id");
            String action = request.getQueryParam("action");

            StringBuilder response = new StringBuilder();
            response.append("POST messages handler\n");

            if (userId != null) {
                response.append("User ID: ").append(userId).append("\n");
            }

            if (action != null) {
                response.append("Action: ").append(action).append("\n");
            }

            response.append("Body: ").append(request.getBody()).append("\n");
            response.append("Query params: ").append(request.getQueryParams());

            String headers = buildHeaders(200, "OK", "text/plain", response.length());
            out.write(headers.getBytes());
            out.write(response.toString().getBytes());
            out.flush();
        });

        server.addHandler("GET", "/", (request, out) -> {
            StringBuilder response = new StringBuilder();
            response.append("Добро пожаловать на сервер!\n");
            response.append("Доступные эндпоинты:\n");
            response.append("- GET /messages?last=10&limit=5\n");
            response.append("- GET /search?q=java&page=1\n");
            response.append("- GET /filter?category=books&category=electronics&tag=sale\n");
            response.append("- POST /messages?user_id=123&action=create\n\n");

            if (!request.getQueryParams().isEmpty()) {
                response.append("Ваши query параметры: ").append(request.getQueryParams());
            } else {
                response.append("Попробуйте добавить query параметры к URL!");
            }

            String headers = buildHeaders(200, "OK", "text/plain", response.length());
            out.write(headers.getBytes());
            out.write(response.toString().getBytes());
            out.flush();
        });

        server.listen(9999);
    }


    private static void serveFile(String path, BufferedOutputStream out) throws IOException {
        final var filePath = Path.of(".", "public", path);

        if (!Files.exists(filePath)) {
            String response = "File not found";
            String headers = buildHeaders(404, "Not Found", "text/plain", response.length());
            out.write(headers.getBytes());
            out.write(response.getBytes());
            out.flush();
            return;
        }

        final String mimeType = Files.probeContentType(filePath);
        final long length = Files.size(filePath);

        if ("/classic.html".equals(path)) {
            String template = Files.readString(filePath);
            String content = template.replace("{time}", LocalDateTime.now().toString());
            String headers = buildHeaders(200, "OK", mimeType, content.length());
            out.write(headers.getBytes());
            out.write(content.getBytes());
            out.flush();
        } else {
            String headers = buildHeaders(200, "OK", mimeType, length);
            out.write(headers.getBytes());
            Files.copy(filePath, out);
            out.flush();
        }
    }

    private static String buildHeaders(int statusCode, String statusMessage, String contentType, long contentLength) {
        return String.format(
                "HTTP/1.1 %d %s\r\n" +
                        "Content-Type: %s\r\n" +
                        "Content-Length: %d\r\n" +
                        "Connection: close\r\n" +
                        "\r\n",
                statusCode, statusMessage, contentType, contentLength
        );
    }
}





