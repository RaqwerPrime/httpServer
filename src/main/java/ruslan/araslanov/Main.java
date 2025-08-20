package ruslan.araslanov;


import java.util.List;

public class Main {
    public static void main(String[] args) {
        final var validPath = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html",
                "/styles.css", "/app.js", "/links.html", "/forms.html",
                "/classic.html", "/events.html", "/events.js", "/treug.jpg");

        final var server = new Server(9999, validPath);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Заверщение работы сервера !!!");
            server.stop();
        }));

        server.start();

    }
}