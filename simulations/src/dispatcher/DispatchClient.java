package dispatcher;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DispatchClient {

    private final ExecutorService executorService;
    private final int numThreads;
    private final AtomicInteger numRunning = new AtomicInteger();

    public DispatchClient() {
        // making the relatively unsafe assumption that there are 2 logical processors for each physical core
        var numProcessors = Runtime.getRuntime().availableProcessors();
        var numCores = numProcessors/2;
        // leave one physical core to do other things
        numThreads = numCores-1;
        executorService = Executors.newFixedThreadPool(numThreads);
    }

    public void submitFile(File config) throws InterruptedException {
        synchronized(numRunning) {
            while(numRunning.get() == numThreads) {
                numRunning.wait();
            }
            numRunning.incrementAndGet();
            executorService.submit(() -> {
                System.out.println(LocalDateTime.now() + ": starting " + config.getName());
                var duration = runConfig(config);
                System.out.println(LocalDateTime.now() + ": finished " + config.getName() + " in " + duration);
            });
            numRunning.decrementAndGet();
            numRunning.notify();
        }
    }

    public boolean shutdownAndWait(Duration duration) throws InterruptedException {
        executorService.shutdown();
        return executorService.awaitTermination(duration.toNanos(), TimeUnit.NANOSECONDS);
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

}
