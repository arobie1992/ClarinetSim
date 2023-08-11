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
