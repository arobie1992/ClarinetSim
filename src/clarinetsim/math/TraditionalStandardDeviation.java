package clarinetsim.math;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TraditionalStandardDeviation {

    private final Map<Long, Double> values = new HashMap<>();
    private double stdev = 0;
    private final Lock lock = new ReentrantLock();

    public void update(long nodeId, double reputation) {
        synchronized(lock) {
            values.put(nodeId, reputation);
            stdev = calculate(values.values());
        }
    }

    public Optional<Double> get() {
        synchronized(lock) {
            return values.size() < 2 ? Optional.empty() : Optional.of(stdev);
        }
    }

    private double calculate(Collection<Double> values) {
        var mean = 0.0;
        for(var i : values) {
            mean += i;
        }
        mean /= values.size();

        // The variance
        var variance = 0.0;
        for(var i : values) {
            variance += Math.pow(i-mean, 2);
        }
        variance /= (values.size()-1);

        // Standard Deviation
        return Math.sqrt(variance);
    }

}
