package clarinetsim.metrics;

import clarinetsim.GlobalState;
import clarinetsim.context.EventContextFactory;
import peersim.config.Configuration;
import peersim.core.Node;

import java.util.HashSet;
import java.util.Set;
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

    private static void printMetrics() {
        for(int i = 0; i < eventContextFactories.length; i++) {
            EventContextFactory eventContextFactory = eventContextFactories[i];
            var messageAssessments = eventContextFactory.reputationManager().getMessageAssessments();
            System.out.println("Node " + i + "(" + (GlobalState.isMalicious(i) ? "mal" : "coop") + ") message assessments: " + messageAssessments);
            eventContextFactory.connectionManager().printConnections(new IdOnlyNode(i));
        }
    }

}
