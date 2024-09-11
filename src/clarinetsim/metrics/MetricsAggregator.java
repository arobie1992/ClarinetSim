package clarinetsim.metrics;

import clarinetsim.GlobalState;
import clarinetsim.MathUtils;
import clarinetsim.context.EventContextFactory;
import clarinetsim.reputation.MessageAssessment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import peersim.config.Configuration;
import peersim.core.Node;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

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

    private static NodeType getType(long nodeId) {
        return GlobalState.isMalicious(nodeId) ? NodeType.MALICIOUS : NodeType.COOPERATIVE;
    }

    private static List<MessageAssessment> getAssessments(EventContextFactory ctx, long nodeId) {
        if(!Configuration.getBoolean("protocol.clarinet.metrics.include_assessments", false)) {
            return Collections.emptyList();
        } else {
            return ctx.reputationManager().getMessageAssessments().get(nodeId);
        }
    }

    private static ReputationInformation createRepInfo(Collection<PeerInfo> peers) {
        if(peers.isEmpty()) {
            return null;
        }
        var reps = peers.stream().map(PeerInfo::reputation).toList();
        return new ReputationInformation(
                MathUtils.average(reps),
                reps.stream().min(Double::compareTo).orElse(-1.0),
                reps.stream().max(Double::compareTo).orElse(-1.0),
                MathUtils.stdev(reps),
                peers.size(),
                peers.stream().filter(PeerInfo::trusted).count(),
                peers.stream().filter(pi -> !pi.trusted()).count()
        );
    }

    private static void printMetrics() {
        var om = new ObjectMapper().registerModule(new Jdk8Module());
        var nodeMetrics = new NodeMetrics[eventContextFactories.length];
        for(int i = 0; i < eventContextFactories.length; i++) {
            var eventContextFactory = eventContextFactories[i];
            var repMan = eventContextFactory.reputationManager();
            var peers = repMan.assessedPeers();
            var trustedPeers = repMan.trusted(peers.stream().map(id -> (Node) new IdOnlyNode(id)).toList())
                    .stream()
                    .map(Node::getID)
                    .collect(Collectors.toSet());
            var peerInfos = peers.stream().map(p -> new PeerInfo(
                    p, getType(p), repMan.getReputation(p), trustedPeers.contains(p), getAssessments(eventContextFactory, p)
            )).toList();
            var includeIndividual = Configuration.getBoolean("protocol.clarinet.metrics.include_individual", false);
            nodeMetrics[i] = new NodeMetrics(
                    i,
                    getType(i),
                    null, // TODO actually implement this
                    createRepInfo(peerInfos),
                    createRepInfo(peerInfos.stream().filter(pi -> pi.type() == NodeType.COOPERATIVE).toList()),
                    createRepInfo(peerInfos.stream().filter(pi -> pi.type() == NodeType.MALICIOUS).toList()),
                    includeIndividual ? peerInfos: Collections.emptyList()
            );
        }
        try {
            System.out.println(om.writeValueAsString(nodeMetrics));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
