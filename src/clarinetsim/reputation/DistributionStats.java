package clarinetsim.reputation;

import clarinetsim.math.OnlineStandardDeviation;
import clarinetsim.math.TraditionalStandardDeviation;
import peersim.config.Configuration;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class DistributionStats {

    private double total;
    private final Set<Long> observedNodes = new HashSet<>();
    private final OnlineStandardDeviation onlineStandardDeviation = new OnlineStandardDeviation();
    private final TraditionalStandardDeviation traditionalStandardDeviation = new TraditionalStandardDeviation();
    private final boolean useOnlineStdev;
    private final Lock lock = new ReentrantLock();

    DistributionStats(String prefix) {
        useOnlineStdev = Configuration.getBoolean(prefix + ".reputation.use_online_stdev");
    }

    void update(long nodeId, double oldReputation, double newReputation) {
        synchronized(lock) {
            if(observedNodes.add(nodeId)) {
                total += newReputation;
            } else {
                total += (newReputation - oldReputation);
            }
            if(useOnlineStdev) {
                onlineStandardDeviation.update(newReputation);
            } else {
                traditionalStandardDeviation.update(nodeId, newReputation);
            }
        }
    }

    double mean() {
        synchronized(lock) {
            return total/observedNodes.size();
        }
    }

    Optional<Double> standardDeviation() {
        synchronized(lock) {
            return useOnlineStdev ? onlineStandardDeviation.get() : traditionalStandardDeviation.get();
        }
    }

    Values values() {
        synchronized(lock) {
            return new Values(mean(), standardDeviation());
        }
    }

    record Values(double mean, Optional<Double> stdev) {}
}
