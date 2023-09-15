package distributor;

import dispatcher.DispatchClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

public class Client {

    private final InetAddress serverAddress;
    private final DispatchClient dispatchClient = new DispatchClient();

    private Client(InetAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    public static Client create(InetAddress serverAddress) {
        return new Client(serverAddress);
    }

    public void run() throws IOException, InterruptedException {
        for(var configOpt = getNextConfig(); configOpt.isPresent(); configOpt = getNextConfig()) {
            dispatchClient.submitFile(writeFile(configOpt.get()));
        }
        dispatchClient.shutdownAndWait(Duration.ofMinutes(1));
    }

    private File writeFile(ConfigFile configFile) throws IOException {
        var file = new File("client-configs/" + configFile.name);
        file.createNewFile();
        try(var writer = new FileOutputStream(file, false)) {
            writer.write(configFile.contents);
        }
        return file;
    }

    private record ConfigFile(String name, byte[] contents) {}

    private Optional<ConfigFile> getNextConfig() throws IOException, InterruptedException {
        var httpClient = HttpClient.newBuilder().build();
        var request = HttpRequest.newBuilder(URI.create("http://" + serverAddress.getHostAddress() + ":8080/next-config"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        return deserializeBody(response.body());
    }

    private Optional<ConfigFile> deserializeBody(byte[] body) {
        if(body.length == 0) {
            return Optional.empty();
        }
        int i = 0;

        for(; i < body.length; i++) {
            var b = body[i];
            if(((char) b) == '\r') {
                var b2 = body[i+1];
                if(((char) b2) == '\n') {
                    break;
                }
            }
        }
        if(i >= body.length) {
            throw new IllegalStateException("Invalid config file contents");
        }
        return Optional.of(new ConfigFile(
                new String(Arrays.copyOfRange(body, 0, i)),
                Arrays.copyOfRange(body, i+2, body.length)
        ));
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if(args.length != 1) {
            throw new IllegalArgumentException("Must provide server address");
        }
        var serverAddress = InetAddress.getByName(args[0]);
        Client.create(serverAddress).run();
    }
}
