package clarinetsim.reputation;

import peersim.config.Configuration;
import peersim.core.Node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class Reputations {

    private final Map<Long, Reputation> reputations = new ConcurrentHashMap<>();
    private final Supplier<Reputation> reputationSupplier;
    private final int evalThreshold;
    private final int minTrusted;
    private final DistributionStats distributionStats;

    Reputations(String prefix) {
        var repTypeStr = Configuration.getString(prefix + ".reputation.type");
        this.reputationSupplier = switch(ReputationType.valueOf(repTypeStr)) {
            case SUBTRACTIVE -> () -> new SubtractiveReputation(prefix);
            case PROPORTIONAL -> () -> new ProportionalReputation(prefix);
        };
        this.evalThreshold = Configuration.getInt(prefix + ".reputation.proportional.eval_threshold");
        this.minTrusted = Configuration.getInt(prefix + ".reputation.min_trusted");
        this.distributionStats = new DistributionStats(prefix);
    }

    public boolean evaluate(Node node) {
        var reputation = reputations.computeIfAbsent(node.getID(), k -> reputationSupplier.get());
        if(reputation.interactions() < evalThreshold) {
            return true;
        }

        DistributionStats.Values statsValues = distributionStats.values();
        // if standard deviation is present, we want to use both; otherwise, just fall back to minTrusted
        if(statsValues.stdev().isPresent()) {
            return reputation.value() > minTrusted && reputation.value() > (statsValues.mean() - statsValues.stdev().get());
        } else {
            return reputation.value() > minTrusted;
        }
   }

    public void penalize(Node node, Penalty penalty) {
        performRepOp(node, rep -> rep.penalize(penalty));
    }

    public void reward(Node node) {
        performRepOp(node, Reputation::reward);
    }

    private void performRepOp(Node node, Consumer<Reputation> op) {
        reputations.compute(node.getID(), (k, v) -> {
            var rep = v == null ? reputationSupplier.get() : v;
            var oldReputation = rep.value();
            op.accept(rep);
            var newReputation = rep.value();
            distributionStats.update(k, oldReputation, newReputation);
            return rep;
        });
    }

    public Map<Long, Double> reputations() {
        return reputations.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().value()));
    }
}
