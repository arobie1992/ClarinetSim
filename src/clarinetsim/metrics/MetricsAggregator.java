package clarinetsim.metrics;

import clarinetsim.GlobalState;
import clarinetsim.context.EventContextFactory;
import peersim.config.Configuration;
import peersim.core.Node;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

public class MetricsAggregator {

    private static final Lock LOCK = new ReentrantLock();
    private static volatile boolean initialized = false;
    private static volatile int numNodes;
    private static volatile int numCycles;
    private static final Set<Long> seenThisCycle = new HashSet<>();
    private static volatile int currentCycle = 0;
    private static volatile EventContextFactory[] eventContextFactories;

    private MetricsAggregator() {}

    public static void init() {
        if(initialized) {
            return;
        }
        synchronized(LOCK) {
            if(initialized) {
                return;
            }
            numNodes = Configuration.getInt("SIZE");
            numCycles = Configuration.getInt("CYCLES");
            initialized = true;
        }
    }

    public static void tick(Node node, EventContextFactory eventContextFactory) {
        synchronized(LOCK) {
            addEventContextFactory(node.getID(), eventContextFactory);
            seenThisCycle.add(node.getID());
            if(seenThisCycle.size() == numNodes) {
                currentCycle++;
                seenThisCycle.clear();
                if(currentCycle == numCycles) {
                    printMetrics();
                }
            }
        }
    }

    private static void addEventContextFactory(long nodeId, EventContextFactory eventContextFactory) {
        if(eventContextFactories == null) {
            eventContextFactories = new EventContextFactory[numNodes];
        }
        eventContextFactories[Math.toIntExact(nodeId)] = eventContextFactory;
    }

    private static class ReputationStats<K extends Comparable<K>> {
        Map<K, Integer> reputations = new HashMap<>();
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        void add(K id, int reputation) {
            reputations.put(id, reputation);
            min = Math.min(min, reputation);
            max = Math.max(max, reputation);
        }

        private Integer average() {
            var total = reputations.values().stream().reduce(Integer::sum).orElse(null);
            return total == null ? null : total/reputations.size();
        }

        private Integer median() {
            var sorted = reputations.values().stream().sorted().toList();
            return sorted.isEmpty() ? null : sorted.get(sorted.size()/2);
        }

        private void addAggregated(StringJoiner sj, String name) {
            if(!reputations.isEmpty()) {
                sj.add("    "+name+": {");
                sj.add("        average: " + average());
                sj.add("        median: " + median());
                sj.add("        min: " + min);
                sj.add("        max: " + max);
                sj.add("    }");
            } else {
                sj.add("    "+name+": {}");
            }
        }

        private void addIndividuals(StringJoiner sj, String name) {
            if(!Configuration.getBoolean("protocol.avg.metrics.print_individual", false)) {
                sj.add("    "+name+": <not printed>");
            } else if(reputations.isEmpty()) {
                sj.add("    "+name+": []");
            } else {
                sj.add("    "+name+": [");
                var sorted = sorted(reputations);
                for(var e : sorted) {
                    sj.add("        node " + e.getKey() + ": " + e.getValue());
                }
                sj.add("    ]");
            }
        }

        private List<Map.Entry<K, Integer>> sorted(Map<K, Integer> map) {
            return map.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
        }
    }

    private static class ReputationInformation<K extends Comparable<K>> {
        final ReputationStats<K> coop = new ReputationStats<>();

        final ReputationStats<K> mal = new ReputationStats<>();

        final ReputationStats<K> withNeighbors = new ReputationStats<>();

        void addCooperative(K neighborId, int reputation) {
            coop.add(neighborId, reputation);
        }

        void addMalicious(K neighborId, int reputation) {
            mal.add(neighborId, reputation);
        }

        @Override public String toString() {
            var sj = new StringJoiner(System.lineSeparator());
            sj.add("{");
            coop.addAggregated(sj, "coop");
            mal.addAggregated(sj, "mal");
            withNeighbors.addAggregated(sj, "repWithNeighbors");
            coop.addIndividuals(sj, "individualCoop");
            mal.addIndividuals(sj, "individualMal");
            withNeighbors.addIndividuals(sj, "repWithNeighbors");
            sj.add("}");
            return sj.toString();
        }
    }

    private static class IndividualReputationInformation extends ReputationInformation<Long> {}

    private static class AggregatedReputationInformation extends ReputationInformation<String> {}

    private static void printMetrics() {
        var totalReputationInfo = new AggregatedReputationInformation();
        var reputationInfos = new IndividualReputationInformation[eventContextFactories.length];

        for(int i = 0; i < eventContextFactories.length; i++) {
            reputationInfos[i] = new IndividualReputationInformation();
        }

        for(int i = 0; i < eventContextFactories.length; i++) {
            var curr = reputationInfos[i];
            for(var e : eventContextFactories[i].reputationManager().reputations().entrySet()) {
                long neighborId = e.getKey();
                int reputation = e.getValue();
                BiConsumer<Long, Integer> op;
                BiConsumer<String, Integer> totalOp;
                if(GlobalState.isMalicious(neighborId)) {
                    op = curr::addMalicious;
                    totalOp = totalReputationInfo::addMalicious;
                } else {
                    op = curr::addCooperative;
                    totalOp = totalReputationInfo::addCooperative;
                }
                op.accept(neighborId, reputation);
                totalOp.accept(i + "-" + neighborId, reputation);
                reputationInfos[Math.toIntExact(neighborId)].withNeighbors.add((long) i, reputation);
            }
        }

        for(int i = 0; i < reputationInfos.length; i++) {
            var repInfo = reputationInfos[i];
            System.out.println("Node " + i + " " + repInfo);
        }
        System.out.println("Grand Total " + totalReputationInfo);

    }

}
