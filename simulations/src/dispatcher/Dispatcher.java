package dispatcher;

import java.io.*;
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
        var numCores = Runtime.getRuntime().availableProcessors();
        executorService = Executors.newFixedThreadPool(numCores);
    }

    public void run() {
        var configsDir = new File("configs");
        var configs = Arrays.stream(Objects.requireNonNull(configsDir.listFiles())).sorted(Comparator.comparing(File::getName)).toList();
        try(
            var startingWriter = new BufferedWriter(new FileWriter("status/starting.txt"));
            var completedWriter = new BufferedWriter(new FileWriter("status/completed.txt"))
        ) {
            for(var config : configs) {
                executorService.submit(() -> {
                    synchronized(startingWriter) {
                        try {
                            startingWriter.write(LocalDateTime.now() + ": starting config " + config.getName() + System.lineSeparator());
                            startingWriter.flush();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                    var duration = runConfig(config);
                    synchronized(completedWriter) {
                        try {
                            completedWriter.write(LocalDateTime.now() + ": finished " + config.getName() + " in " + duration + System.lineSeparator());
                            completedWriter.flush();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(3, TimeUnit.DAYS);

        } catch (IOException|InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Duration runConfig(File config) {
        var outputName = makeOutputName(config.getName());
        System.out.println(outputName);
        var cmdFmt = "make run %s > %s";
        var cmd = String.format(cmdFmt, config.getAbsolutePath(), outputName);
        System.out.println(cmd);
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

    public static void main(String[] args) {
        new Dispatcher().run();
    }

}