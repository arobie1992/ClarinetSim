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

    private static abstract class ReputationInformation<K extends Comparable<K>> {
        Map<K, Integer> coopReputations = new HashMap<>();
        int coopMin = Integer.MAX_VALUE;
        int coopMax = Integer.MIN_VALUE;

        Map<K, Integer> malReputations = new HashMap<>();
        int malMin = Integer.MAX_VALUE;
        int malMax = Integer.MIN_VALUE;

        final Map<K, Integer> reputationWithOtherNodes = new HashMap<>();

        void addCooperative(K neighborId, int reputation) {
            coopReputations.put(neighborId, reputation);
            coopMin = Math.min(coopMin, reputation);
            coopMax = Math.max(coopMax, reputation);
        }

        void addMalicious(K neighborId, int reputation) {
            malReputations.put(neighborId, reputation);
            malMin = Math.min(malMin, reputation);
            malMax = Math.max(malMax, reputation);
        }

        @Override public String toString() {
            var sj = new StringJoiner(System.lineSeparator());
            sj.add("{");
            sj.add("\tcoopAverage: " + average(coopReputations));
            sj.add("\tcoopMedian: " + median(coopReputations));
            sj.add("\tcoopMin: " + coopMin);
            sj.add("\tcoopMax: " + coopMax);
            sj.add("\tmalAverage: " + average(malReputations));
            sj.add("\tmalMedian: " + median(malReputations));
            sj.add("\tmalMin: " + malMin);
            sj.add("\tmalMax: " + malMax);
            addIndividuals(sj, "repWithPeers: ", reputationWithOtherNodes);
            addIndividuals(sj, "individualCoop", coopReputations);
            addIndividuals(sj, "individualMal", malReputations);
            sj.add("}");
            return sj.toString();
        }

        private Integer average(Map<K, Integer> reputations) {
            var total = reputations.values().stream().reduce(Integer::sum).orElse(null);
            return total == null ? null : total/reputations.size();
        }

        private Integer median(Map<K, Integer> reputations) {
            var sorted = reputations.values().stream().sorted().toList();
            return sorted.isEmpty() ? null : sorted.get(sorted.size()/2);
        }

        private void addIndividuals(StringJoiner sj, String name, Map<K, Integer> reputations) {
            if(reputations.isEmpty()) {
                sj.add("\t"+name+": []");
            } else {
                sj.add("\t"+name+": [");
                var sorted = sorted(reputations);
                for(var e : sorted) {
                    sj.add("\t\tnode " + e.getKey() + ": " + e.getValue());
                }
                sj.add("\t]");
            }
        }

        private List<Map.Entry<K, Integer>> sorted(Map<K, Integer> map) {
            return map.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
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

        int numMalicious = Configuration.getInt("protocol.avg.num_malicious", 0);
        for(int i = numMalicious; i < eventContextFactories.length; i++) {
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
                reputationInfos[Math.toIntExact(neighborId)].reputationWithOtherNodes.put((long) i, reputation);
            }
        }

        for(int i = numMalicious; i < reputationInfos.length; i++) {
            var repInfo = reputationInfos[i];
            System.out.println("Node " + i + " " + repInfo);
        }
        System.out.println("Grand Total " + totalReputationInfo);

    }

}
