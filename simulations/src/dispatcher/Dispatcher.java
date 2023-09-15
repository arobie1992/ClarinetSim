package dispatcher;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

public class Dispatcher {

    private final DispatchClient dispatchClient = new DispatchClient();

    public void run() throws InterruptedException {
        var configsDir = new File("configs");
        var configs = Arrays.stream(Objects.requireNonNull(configsDir.listFiles())).sorted(Comparator.comparing(File::getName)).toList();
        var start = LocalDateTime.now();
        for(var config : configs) {
            dispatchClient.submitFile(config);
        }
        if(dispatchClient.shutdownAndWait(Duration.ofDays(3))) {
            System.out.println("All sims finished successfully in " + Duration.between(start, LocalDateTime.now()));
        } else {
            System.out.println("Timeout occurred before all sims finished");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new Dispatcher().run();
    }

}