package ru.netology.server.handler;

import ru.netology.server.request.Request;

import java.io.BufferedOutputStream;

@FunctionalInterface
public interface Handler {
    void handle(Request request, BufferedOutputStream responseStream);
}
