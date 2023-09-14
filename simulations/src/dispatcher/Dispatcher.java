package dispatcher;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Dispatcher {

    private final ExecutorService executorService;

    public Dispatcher() {
        // making the relatively unsafe assumption that there are 2 logical processors for each physical core
        var numProcessors = Runtime.getRuntime().availableProcessors();
        var numCores = numProcessors/2;
        // leave one physical core to do other things
        executorService = Executors.newFixedThreadPool(numCores-1);
    }

    public void run() throws InterruptedException {
        var configsDir = new File("configs");
        var configs = Arrays.stream(Objects.requireNonNull(configsDir.listFiles())).sorted(Comparator.comparing(File::getName)).toList();
        var start = LocalDateTime.now();
        for(var config : configs) {
            executorService.submit(() -> {
                System.out.println(LocalDateTime.now() + ": starting " + config.getName());
                var duration = runConfig(config);
                System.out.println(LocalDateTime.now() + ": finished " + config.getName() + " in " + duration);
            });
        }
        executorService.shutdown();
        if(executorService.awaitTermination(3, TimeUnit.DAYS)) {
            System.out.println("All sims finished successfully in " + Duration.between(start, LocalDateTime.now()));
        } else {
            System.out.println("Timeout occurred before all sims finished");
        }
    }

    private Duration runConfig(File config) {
        var outputName = makeOutputName(config.getName());
        var cmdFmt = "make run %s > %s 2>&1";
        var cmd = String.format(cmdFmt, config.getAbsolutePath(), outputName);
        try {
            var start = LocalDateTime.now();
            var p = Runtime.getRuntime().exec(cmd, null, new File(".."));
            p.waitFor();
            return Duration.between(start, LocalDateTime.now());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String makeOutputName(String configName) {
        var f = new File("");
        return f.getAbsolutePath() + "/output/results" + configName.substring(configName.indexOf('-'));
    }

    public static void main(String[] args) throws InterruptedException {
        new Dispatcher().run();
    }

}