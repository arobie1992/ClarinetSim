package distributor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private final List<File> configFiles;
    private final AtomicInteger position = new AtomicInteger();
    private final AtomicBoolean finished = new AtomicBoolean(false);

    private Server(List<File> configFiles) {
        this.configFiles = configFiles;
    }

    public static Server create() throws IOException {
        var configsDir = new File("configs");
        var configFiles = Arrays.stream(Objects.requireNonNull(configsDir.listFiles()))
                .sorted(Comparator.comparing(File::getName))
                .toList();
        return new Server(configFiles);
    }

    public void serve() throws IOException {
        var server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/next-config", this::handleConfigRequest);
        server.start();
    }

    private void handleConfigRequest(HttpExchange exchange) throws IOException {
        var responseBodyStream = exchange.getResponseBody();
        // POST because there is state change
        if(!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, 0);
            responseBodyStream.write(new byte[]{});
            exchange.getResponseBody().close();
            return;
        }
        if(finished.get()) {
            exchange.sendResponseHeaders(200, 0);
            responseBodyStream.write(new byte[]{});
            exchange.getResponseBody().close();
            return;
        }
        var configIndex = position.getAndIncrement();
        if(configIndex >= configFiles.size()) {
            finished.compareAndExchange(false, true);
            exchange.sendResponseHeaders(200, 0);
            responseBodyStream.write(new byte[]{});
            exchange.getResponseBody().close();
            return;
        }
        var responseBody = serializeFile(configFiles.get(configIndex));
        exchange.sendResponseHeaders(200, responseBody.length);
        responseBodyStream.write(responseBody);
        responseBodyStream.close();
    }

    /*
    Body format is
    {file name}crlf
    {file contents}
     */
    private byte[] serializeFile(File configFile) throws IOException {
        var fileName = configFile.getName().getBytes();
        var fileContents = Files.readAllBytes(configFile.toPath());
        var response = new byte[fileName.length + fileContents.length + 2];
        int i = 0;
        for(; i < fileName.length; i++) {
            response[i] = fileName[i];
        }
        response[i++] = '\r';
        response[i++] = '\n';
        for(int j = 0; j < fileContents.length; j++) {
            response[i+j] = fileContents[j];
        }
        return response;
    }

    public static void main(String[] args) throws IOException {
        var server = Server.create();
        server.serve();
    }

}