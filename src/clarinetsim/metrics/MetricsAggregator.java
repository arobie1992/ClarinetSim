package clarinetsim.metrics;

import clarinetsim.GlobalState;
import clarinetsim.context.EventContextFactory;
import peersim.config.Configuration;
import peersim.core.Node;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
            eventContextFactories = new EventContextFactory[numNodes];
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
        if(eventContextFactories[Math.toIntExact(nodeId)] == null) {
            eventContextFactories[Math.toIntExact(nodeId)] = eventContextFactory;
        }
    }

    private static class IndividualReputationInformation extends ReputationInformation<Long> {}

    private static class AggregatedReputationInformation extends ReputationInformation<String> {
        @Override public String toString() {
            var sj = new StringJoiner(System.lineSeparator());
            sj.add("{");
            coop.addAggregated(sj, "coop");
            mal.addAggregated(sj, "mal");
            withNeighbors.addAggregated(sj, "repWithNeighbors");

            var avg = avg();
            sj.add("    total: {");
            sj.add("        average: " + avg);
            sj.add("        median: " + median());
            // the standard deviation calculations weren't correct and can calulate it with fields in results.ts, so
            // just do it there instead and print filler values here so the parsing doesn't break
            sj.add("        stdev: -1");
            sj.add("        numCoopBelow: -1");
            sj.add("        numMalBelow: -1");
            sj.add("        numMalActedMaliciously: -1");
            sj.add("    }");

            coop.addIndividuals(sj, "individualCoop");
            mal.addIndividuals(sj, "individualMal");
            withNeighbors.addIndividuals(sj, "individualRepWithNeighbors");
            sj.add("}");
            return sj.toString();
        }
    }

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
                double reputation = e.getValue();
                var totalId = i + "-" + neighborId;
                var trusted = eventContextFactories[i].reputationManager().evaluate(new IdOnlyNode(neighborId));
                if(GlobalState.isMalicious(neighborId)) {
                    curr.addMalicious(neighborId, reputation, trusted);
                    totalReputationInfo.addMalicious(totalId, reputation, trusted);
                } else {
                    curr.addCooperative(neighborId, reputation, trusted);
                    totalReputationInfo.addCooperative(totalId, reputation, trusted);
                }
                var peerTrusts = eventContextFactories[Math.toIntExact(neighborId)].reputationManager().evaluate(new IdOnlyNode(i));
                reputationInfos[Math.toIntExact(neighborId)].withNeighbors.add((long) i, reputation, peerTrusts);
            }
        }

        for(int i = 0; i < reputationInfos.length; i++) {
            var repInfo = reputationInfos[i];
            var malInfo = GlobalState.isMalicious(i) ? " (malicious) " : " (cooperative) ";
            System.out.println("Node " + i + malInfo + repInfo);
        }
        System.out.println("Grand Total " + totalReputationInfo);

    }

}
