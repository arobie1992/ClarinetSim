package clarinetsim.reputation;

import clarinetsim.math.OnlineStandardDeviation;
import clarinetsim.math.TraditionalStandardDeviation;
import peersim.config.Configuration;

import java.util.HashSet;
import java.util.List;
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
            if(useOnlineStdev) {
                if(observedNodes.add(nodeId)) {
                    total += newReputation;
                } else {
                    total += (newReputation - oldReputation);
                }
                onlineStandardDeviation.update(newReputation);
            }
        }
    }

    double mean(List<Double> reputations) {
        synchronized(lock) {
            if(useOnlineStdev) {
                return total/observedNodes.size();
            } else {
                var total = reputations.stream().reduce(Double::sum).orElse(null);
                return total == null ? 0 : total/reputations.size();
            }
        }
    }

    Optional<Double> standardDeviation(List<Double> reputations) {
        synchronized(lock) {
            return useOnlineStdev ? onlineStandardDeviation.get() : traditionalStandardDeviation.calculate(reputations);
        }
    }

    Values values(List<Double> reputations) {
        synchronized(lock) {
            return new Values(mean(reputations), standardDeviation(reputations));
        }
    }

    record Values(double mean, Optional<Double> stdev) {}

    boolean requiresIndividualValues() {
        return !useOnlineStdev;
    }

}
