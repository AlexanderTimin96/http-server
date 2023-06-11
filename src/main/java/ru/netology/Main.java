package ru.netology;

import ru.netology.server.Server;
import ru.netology.server.ThreadClientHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;


public class Main {
    public static void main(String[] args) {
        final var server = new Server();

        server.addHandler("GET", "/spring.svg", ThreadClientHandler::responseOK);
        server.addHandler("GET", "/spring.png", ThreadClientHandler::responseOK);
        server.addHandler("GET", "/resources.html", ThreadClientHandler::responseOK);
        server.addHandler("GET", "/styles.css", ThreadClientHandler::responseOK);
        server.addHandler("GET", "/app.js", ThreadClientHandler::responseOK);
        server.addHandler("GET", "/links.html", ThreadClientHandler::responseOK);
        server.addHandler("GET", "/events.js", ThreadClientHandler::responseOK);
        server.addHandler("GET", "/events.html", ThreadClientHandler::responseOK);

        server.addHandler("GET", "/forms.html", (request, responseStream) -> {
            if (request.getPath().contains("?")) {
                ThreadClientHandler.logInConsole(request, responseStream);
            } else {
                ThreadClientHandler.responseOK(request, responseStream);
            }
        });
        server.addHandler("POST", "/forms.html", ThreadClientHandler::logInConsole);

        server.addHandler("GET", "/default-get.html", (request, responseStream) -> {
            if (request.getPath().contains("?")) {
                ThreadClientHandler.logInConsole(request, responseStream);
            } else {
                ThreadClientHandler.responseOK(request, responseStream);
            }
        });
        server.addHandler("POST", "/default-get.html", ThreadClientHandler::logInConsole);

        server.addHandler("GET", "/classic.html", (request, responseStream) -> {
            try {
                final var filePath = Path.of(".", "public", request.getPath());
                final var mimeType = Files.probeContentType(filePath);
                final var template = Files.readString(filePath);
                final var content = template.replace("{time}", LocalDateTime.now().toString()).getBytes();
                responseStream.write(("HTTP/1.1 200 OK\r\n"
                        + "Content-Type: " + mimeType + "\r\n"
                        + "Content-Length: "
                        + content.length + "\r\n"
                        + "Connection: close\r\n" + "\r\n")
                        .getBytes());
                responseStream.write(content);
                responseStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        server.start(9999);
    }
}

