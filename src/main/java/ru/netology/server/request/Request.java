package ru.netology.server.request;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Request implements RequestContext {
    private static final String GET = "GET";
    private static final String POST = "POST";
    private final String method;
    private final String path;
    private final List<String> headers;
    private final String contentType;
    private final byte[] body;
    private static final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
    private static final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
    private List<NameValuePair> queryParams;
    private List<NameValuePair> postParams;
    private List<FileItem> postParts;

    private Request(String method, String path, List<String> headers, String contentType, byte[] body) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.contentType = contentType;
        this.body = body;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<String> getHeaders() {
        return headers;
    }

    @Override
    public String getCharacterEncoding() {
        return StandardCharsets.UTF_8.toString();
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    @Deprecated
    public int getContentLength() {
        return 0;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(body);
    }

    public void setQueryParams(List<NameValuePair> queryParams) {
        this.queryParams = queryParams;
    }

    public List<NameValuePair> getQueryParams() {
        return queryParams;
    }

    public List<NameValuePair> getQueryParam(String name) {
        return queryParams.stream()
                .filter((queryParam) -> queryParam.getName().equals(name))
                .collect(Collectors.toList());
    }

    public List<NameValuePair> getPostParams() {
        return postParams;
    }

    public List<NameValuePair> getPostParam(String name) {
        return postParams.stream()
                .filter((postParam) -> postParam.getName().equals(name))
                .collect(Collectors.toList());
    }

    public List<FileItem> getPostParts() {
        return postParts;
    }

    public List<FileItem> getPostPart(String name) {
        return postParts.stream()
                .filter((postParam) -> postParam.getName().equals(name))
                .collect(Collectors.toList());
    }

    public static Request requestBuild(BufferedInputStream in) throws IOException {
        final var allowedMethods = List.of(GET, POST);
        final var limit = 4096;

        in.mark(limit);
        final var buffer = new byte[limit];
        final var read = in.read(buffer);

        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
        if (requestLineEnd == -1) {
            return null;
        }

        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            return null;
        }

        final var method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            return null;
        }

        final var path = requestLine[1];
        if (!path.startsWith("/")) {
            return null;
        }

        final var headersStart = requestLineEnd + requestLineDelimiter.length;
        final var headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
        if (headersEnd == -1) {
            return null;
        }
        in.reset();
        in.skip(headersStart);

        final var headersBytes = in.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));

        final var contentType = extractHeader(headers, "Content-Type").toString();

        byte[] body = null;
        in.skip(headersDelimiter.length);
        final var contentLength = extractHeader(headers, "Content-Length");
        if (contentLength.isPresent()) {
            final var length = Integer.parseInt(contentLength.get());
            body = in.readNBytes(length);
        }

        return new Request(method, path, headers, contentType, body);
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    public void setParams() throws URISyntaxException, IOException, FileUploadException {
        if (getMethod().equals(GET)) {
            setQueryParams(URLEncodedUtils.parse(new URI(path), StandardCharsets.UTF_8));
        }

        if (getMethod().equals(POST)) {
            if (contentType.equals("application/x-www-form-urlencoded")) {
                postParams = URLEncodedUtils.parse(new String(body), StandardCharsets.UTF_8);
            }

            if (contentType.contains("multipart/form-data")) {
                DiskFileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                postParts = upload.parseRequest(this);
            }
        }
    }
}