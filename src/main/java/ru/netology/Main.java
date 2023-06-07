package ru.netology;

import ru.netology.server.Server;
import ru.netology.server.request.Request;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;


public class Main {
    public static void main(String[] args) {
        final var server = new Server();

        server.addHandler("GET", "/spring.svg", Main::responseOK);
        server.addHandler("GET", "/spring.png", Main::responseOK);
        server.addHandler("GET", "/resources.html", Main::responseOK);
        server.addHandler("GET", "/styles.css", Main::responseOK);
        server.addHandler("GET", "/app.js", Main::responseOK);
        server.addHandler("GET", "/links.html", Main::responseOK);
        server.addHandler("GET", "/forms.html", Main::responseOK);
        server.addHandler("GET", "/events.js", Main::responseOK);
        server.addHandler("GET", "/events.html", Main::responseOK);

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

    private static void responseOK(Request request, BufferedOutputStream responseStream) {
        try {
            final var filePath = Path.of(".", "public", request.getPath());
            final var mimeType = Files.probeContentType(filePath);
            final var length = Files.size(filePath);
            responseStream.write(("HTTP/1.1 200 OK\r\n"
                    + "Content-Type: " + mimeType + "\r\n"
                    + "Content-Length: " + length + "\r\n"
                    + "Connection: close\r\n" + "\r\n")
                    .getBytes());
            Files.copy(filePath, responseStream);
            responseStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

