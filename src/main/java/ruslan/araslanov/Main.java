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
                "/events.html", "/events.js","/treug.jpg"
        );

        for (String path : validPaths) {
            server.addHandler("GET", path, (request, out) -> {
                serveFile(path, out);
            });
        }

        server.addHandler("GET", "/messages", (request, out) -> {
            String response = "GET messages handler: " + LocalDateTime.now();
            String headers = buildHeaders(200, "OK", "text/plain", response.length());
            out.write(headers.getBytes());
            out.write(response.getBytes());
            out.flush();
        });

        server.addHandler("POST", "/messages", (request, out) -> {
            String response = "POST messages handler. Body: " + request.getBody();
            String headers = buildHeaders(200, "OK", "text/plain", response.length());
            out.write(headers.getBytes());
            out.write(response.getBytes());
            out.flush();
        });

        server.addHandler("GET", "/", (request, out) -> {
            serveFile("/index.html", out);
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





