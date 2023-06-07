package ru.netology.server;

import ru.netology.server.handler.Handler;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private final ExecutorService executorService;
    private final ConcurrentHashMap<String, Map<String, Handler>> handlersMap = new ConcurrentHashMap<>();

    public Server() {
        executorService = Executors.newFixedThreadPool(64);
    }

    public void start(int PORT) {
        System.out.println("Server start on port " + PORT);
        for (String path : validPaths) {
            System.out.println("Open in browser: http://localhost:" + PORT + path);
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (!serverSocket.isClosed()) {
                final var socket = serverSocket.accept();
                var clientHandler = new ThreadClientHandler(socket, handlersMap);
                executorService.submit(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addHandler(String method, String msg, Handler handler) {
        if (!handlersMap.containsKey(method))
            handlersMap.put(method, new ConcurrentHashMap<>());

        handlersMap.get(method).put(msg, handler);
    }
}
