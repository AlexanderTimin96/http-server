package ru.netology.server;

import ru.netology.server.handler.Handler;
import ru.netology.server.request.Request;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadClientHandler extends Thread {
    Socket socket;
    private final ConcurrentHashMap<String, Map<String, Handler>> handlersMap;

    public ThreadClientHandler(Socket socket, ConcurrentHashMap<String, Map<String, Handler>> handlersMap) {
        this.socket = socket;
        this.handlersMap = handlersMap;
    }

    @Override
    public void run() {
        try (
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                this.interrupt();
            }

            final var request = new Request(parts[0], parts[1]);

            if (!handlersMap.containsKey(request.getMethod())
                    || !handlersMap.get(request.getMethod()).containsKey(request.getPath())) {
                responseNotFound(out);
                this.interrupt();
            }

            Handler handler = handlersMap.get(request.getMethod()).get(request.getPath());
            handler.handle(request, out);
            this.interrupt();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void responseNotFound(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }
}
