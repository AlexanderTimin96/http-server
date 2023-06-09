package ru.netology.server;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.http.NameValuePair;
import ru.netology.server.handler.Handler;
import ru.netology.server.request.Request;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadClientHandler extends Thread {
    private final Socket socket;
    private final ConcurrentHashMap<String, Map<String, Handler>> handlersMap;


    public ThreadClientHandler(Socket socket, ConcurrentHashMap<String, Map<String, Handler>> handlersMap) {
        this.socket = socket;
        this.handlersMap = handlersMap;
    }

    @Override
    public void run() {
        try (
                final var in = new BufferedInputStream(socket.getInputStream());
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            Request request = Request.requestBuild(in);
            if (request == null) {
                badRequest(out);
                this.interrupt();
            }

            if (request.getMethod().equals("POST")) {
                request.setParams();
            }

            if (!handlersMap.containsKey(request.getMethod())
                    || !(handlersMap.get(request.getMethod()).containsKey(request.getPath().split("\\?")[0]))) {
                responseNotFound(out);
                this.interrupt();
            }

            Handler handler = handlersMap.get(request.getMethod()).get(request.getPath().split("\\?")[0]);
            handler.handle(request, out);
            this.interrupt();

        } catch (IOException | URISyntaxException | FileUploadException e) {
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

    private static void badRequest(BufferedOutputStream out) throws IOException {
        out.write((
                "HTTP/1.1 400 Bad Request\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public static void responseOK(Request request, BufferedOutputStream responseStream) {
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

    public static void logInConsole(Request request, BufferedOutputStream responseStream) {
        System.out.println("Request: ");
        System.out.println("METHOD: " + request.getMethod());
        System.out.println("PATH: " + request.getPath());
        System.out.println();

        System.out.println("HEADERS:");
        for (String header : request.getHeaders()) {
            System.out.println(header);
        }
        System.out.println();

        if (request.getQueryParams() != null) {
            System.out.println("QUERY_STRING: ");
            for (NameValuePair nameValuePair : request.getQueryParams()) {
                System.out.println(nameValuePair.getName() + ": " + nameValuePair.getValue());
            }
        }

        if (request.getPostParams() != null) {
            System.out.println("BODY_PARAM: ");
            for (NameValuePair nameValuePair : request.getPostParams()) {
                System.out.println(nameValuePair.getName() + ": " + nameValuePair.getValue());
            }
        }

        if (request.getPostParts() != null) {
            System.out.println("BODY_PARTS: ");
            for (FileItem fileItem : request.getPostParts()) {
                if (fileItem.isFormField()) {
                    System.out.println(fileItem.getFieldName() + " : " + fileItem.getString());
                } else {
                    String fileName = fileItem.getName();
                    try (final var stream = fileItem.getInputStream()) {
                        Files.copy(stream, Path.of(".").resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("File save in project: " + fileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
