package dispatcher;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Dispatcher {

    private final ExecutorService executorService;

    public Dispatcher() {
        var numCores = Runtime.getRuntime().availableProcessors();
        executorService = Executors.newFixedThreadPool(numCores);
    }

    public void run() {
        var configsDir = new File("configs");
        var configs = Arrays.stream(Objects.requireNonNull(configsDir.listFiles())).sorted(Comparator.comparing(File::getName)).toList();
        for(var config : configs) {
            executorService.submit(() -> runConfig(config));
        }
        executorService.shutdown();
    }

    private void runConfig(File config) {
        var outputName = makeOutputName(config.getName());
        System.out.println(outputName);
        var cmdFmt = "make run %s > %s";
        var cmd = String.format(cmdFmt, config.getAbsolutePath(), outputName);
        System.out.println(cmd);
        try {
            var p = Runtime.getRuntime().exec(cmd, null, new File(".."));
            p.waitFor();
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