package clarinetsim.reputation;

import peersim.config.Configuration;
import peersim.core.Node;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProportionalReputations implements Reputations {

    private final Map<Long, RepStats> reputations = new ConcurrentHashMap<>();

    private final int minReputation;
    private final int evalThreshold;

    public ProportionalReputations(String prefix) {
        this.minReputation = Configuration.getInt(prefix + ".reputation.min_trusted", 50);
        this.evalThreshold = Configuration.getInt(prefix + ".reputation.proportional.eval_threshold", 50);
    }

    @Override public boolean evaluate(Node node) {
        var stats = reputations.computeIfAbsent(node.getID(), k -> new RepStats());
        return stats.total < evalThreshold || stats.calc() >= minReputation;
    }

    @Override public void penalize(Node node, Penalty penalty) {
        apply(node, 0, penalty.value());
    }

    @Override public void reward(Node node) {
        apply(node, 1, 1);
    }

    private void apply(Node node, int goodIncrease, int totalIncrease) {
        reputations.compute(node.getID(), (k, v) -> {
            var stats = v != null ? v : new RepStats();
            stats.good += goodIncrease;
            stats.total += totalIncrease;
            return stats;
        });
    }

    @Override public Map<Long, Integer> reputations() {
        var copy = Map.copyOf(reputations);
        var ret = new HashMap<Long, Integer>();
        for(var e : copy.entrySet()) {
            ret.put(e.getKey(), e.getValue().calc());
        }
        return Collections.unmodifiableMap(ret);
    }

    private static class RepStats {
        double good = 0;
        double total = 0;

        int calc() {
            return total == 0 ? 100 : (int) ((good/total) * 100);
        }
    }
}
