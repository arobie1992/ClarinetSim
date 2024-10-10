package dispatcher;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Dispatcher {

    private static FileWriter logFile;

    private final ExecutorService executorService;

    public Dispatcher() {
        // making the relatively unsafe assumption that there are 2 logical processors for each physical core
        var numProcessors = Runtime.getRuntime().availableProcessors();
        var numCores = numProcessors/2;
        // leave one physical core to do other things
        executorService = Executors.newFixedThreadPool(numCores-1);
    }

    public void run() {
        var configsDir = new File("configs");
        var configs = Arrays.stream(Objects.requireNonNull(configsDir.listFiles())).sorted(Comparator.comparing(File::getName)).toList();
        var start = LocalDateTime.now();
        for(var config : configs) {
            executorService.submit(() -> {
                log(LocalDateTime.now() + ": starting " + config.getName());
                var result = runConfig(config);
                var runStatus = result.exitCode == 0 ? " succeeded " : " failed " + result.exitCode;
                log(LocalDateTime.now() + ": finished " + config.getName() + ": " + runStatus + " in " + result.duration);
            });
        }
        executorService.shutdown();
        try {
            if(executorService.awaitTermination(3, TimeUnit.DAYS)) {
                log("All sims finished successfully in " + Duration.between(start, LocalDateTime.now()));
            } else {
                log("Timeout occurred before all sims finished");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log(e);
        }
    }

    record RunResult(int exitCode, Duration duration) {}

    private RunResult runConfig(File config) {
        var outputName = makeLogName(config.getName());
        var cmdFmt = "make run %s > %s 2>&1";
        var cmd = String.format(cmdFmt, config.getAbsolutePath(), outputName);
        try {
            var start = LocalDateTime.now();
            var p = Runtime.getRuntime().exec(cmd, null, new File(".."));
            var exitCode = p.waitFor();
            return new RunResult(exitCode, Duration.between(start, LocalDateTime.now()));
        } catch (Exception e) {
            log(e);
            throw new RuntimeException(e);
        }
    }

    private String makeLogName(String configName) {
        var f = new File("");
        return f.getAbsolutePath() + "/logs/log" + configName.substring(configName.indexOf('-'));
    }

    private static void log(String message) {
        try {
            logFile.append(message).append('\n');
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    private static void log(Throwable t) {
        if(logFile == null) {
            t.printStackTrace(System.err);
        } else {
            t.printStackTrace(new PrintWriter(logFile, true));
        }
    }

    public static void main(String[] args) throws IOException {
        try {
            logFile = new FileWriter("run" + LocalDateTime.now().toString().replaceAll("[:|.]", "_") + ".log", true);
            new Dispatcher().run();
        } catch (Throwable e) {
            log(e);
        }
    }

}