package clarinetsim.reputation;

import peersim.config.Configuration;
import peersim.core.Node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class SubtractiveReputations implements Reputations {

    private final Map<Long, Integer> reputations = new ConcurrentHashMap<>();
    private final int initialReputation;
    private final int minTrustedReputation;

    SubtractiveReputations(String prefix) {
        this.initialReputation = Configuration.getInt(prefix + "reputation.subtractive.initial", 100);
        this.minTrustedReputation = Configuration.getInt(prefix + "reputation.min_trusted", 0);
    }

    @Override public boolean evaluate(Node node) {
        return reputations.computeIfAbsent(node.getID(), k -> initialReputation) >= minTrustedReputation;
    }

    @Override public void penalize(Node node, Penalty penalty) {
        reputations.compute(node.getID(), (k, v) -> {
            int reputation = v == null ? initialReputation : v;
            return reputation - penalty.value();
        });
    }

    // No-op
    @Override public void reward(Node node) {}

    @Override public Map<Long, Integer> reputations() {
        return Map.copyOf(reputations);
    }
}
