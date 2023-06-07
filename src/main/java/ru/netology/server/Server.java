package ru.netology.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final List<String> validPaths = List.of("/index.html",
            "/spring.svg",
            "/spring.png",
            "/resources.html",
            "/styles.css",
            "/app.js",
            "/links.html",
            "/forms.html",
            "/classic.html",
            "/events.html",
            "/events.js");
    private final int PORT = 9999;
    private final ExecutorService executorService;

    public Server() {
        executorService = Executors.newFixedThreadPool(64);
    }

    public void start() {
        System.out.println("Server start on port " + PORT);
                for (String path : validPaths) {
                    System.out.println("Open in brawuser: http://localhost:" + PORT + path);
        }


        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (!serverSocket.isClosed()) {
                final var socket = serverSocket.accept();
                var clientHandler = new ClientHandler(socket, validPaths);
                executorService.submit(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
