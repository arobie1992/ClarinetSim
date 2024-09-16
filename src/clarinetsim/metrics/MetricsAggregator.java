package clarinetsim.metrics;

import clarinetsim.GlobalState;
import clarinetsim.context.EventContextFactory;
import clarinetsim.math.MathUtils;
import clarinetsim.reputation.MessageAssessment;
import clarinetsim.reputation.ReputationManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import peersim.config.Configuration;
import peersim.core.Node;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
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
                    try {
                        printMetrics();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
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
        return ctx.reputationManager().getMessageAssessments().get(nodeId);
    }

    private static ReputationInformation createRepInfo(Collection<PeerInfo> peers) {
        if(peers.isEmpty()) {
            return null;
        }
        var reps = peers.stream().map(PeerInfo::reputation).toList();
        var trusted = peers.stream().filter(PeerInfo::trusted).toList();
        var untrusted = peers.stream().filter(p -> !p.trusted()).toList();
        return new ReputationInformation(
                MathUtils.mean(reps),
                reps.stream().min(Double::compareTo).orElse(-1.0),
                reps.stream().max(Double::compareTo).orElse(-1.0),
                MathUtils.standardDeviation(reps),
                peers.size(),
                trusted.size(),
                untrusted.size(),
                peers.stream().map(PeerInfo::numMessages).reduce(0L, Long::sum),
                trusted.stream().map(PeerInfo::numMessages).reduce(0L, Long::sum),
                untrusted.stream().map(PeerInfo::numMessages).reduce(0L, Long::sum),
                peers.stream().map(PeerInfo::numAssessments).reduce(0L, Long::sum),
                trusted.stream().map(PeerInfo::numAssessments).reduce(0L, Long::sum),
                untrusted.stream().map(PeerInfo::numAssessments).reduce(0L, Long::sum)
        );
    }

    private static Set<Long> getTrusted(Collection<Long> peers, ReputationManager reputationManager) {
        return reputationManager.trusted(peers.stream().map(id -> (Node) new IdOnlyNode(id)).toList())
                .stream()
                .map(Node::getID)
                .collect(Collectors.toSet());
    }

    private static List<PeerInfo> createPeerInfos(Collection<Long> peers, ReputationManager reputationManager, EventContextFactory ctx) {
        var trustedPeers = getTrusted(peers, reputationManager);
        var messages = ctx.communicationManager().allMessages();
        var peerMessages = new HashMap<Long, List<MessageRecord>>();
        for(var e : messages) {
            var msg = e.message();
            var rec = new MessageRecord(
                    msg.connectionId(),
                    msg.seqNo(),
                    msg.data(),
                    msg.senderSignature(),
                    msg.witnessSignature(),
                    e.sender().getID(),
                    e.witness().getID(),
                    e.receiver().getID()
            );
            for(var p : e.participants()) {
                peerMessages.computeIfAbsent(p.getID(), k -> new ArrayList<>()).add(rec);
            }
        }
        var includeMessages = Configuration.getBoolean("protocol.clarinet.metrics.include_messages", false);
        var includeAssessments = Configuration.getBoolean("protocol.clarinet.metrics.include_assessments", false);
        return peers.stream().map(p -> {
            var pm = peerMessages.get(p);
            var assessments = getAssessments(ctx, p);
            return new PeerInfo(
                    p,
                    getType(p),
                    reputationManager.getReputation(p),
                    trustedPeers.contains(p),
                    pm.size(),
                    includeMessages ? pm : null,
                    assessments.size(),
                    includeAssessments ? assessments : null
            );
        }).toList();
    }

    private static void printMetrics() throws IOException {

        var nodeReps = new LinkedHashMap<Long, Map<Long, PeerInfo>>();
        var nodeWithPeers = new LinkedHashMap<Long, Map<Long, PeerInfo>>();
        for(int i = 0; i < eventContextFactories.length; i++) {
            var eventContextFactory = eventContextFactories[i];
            var repMan = eventContextFactory.reputationManager();
            var peers = repMan.assessedPeers();
            var peerInfos = createPeerInfos(peers, repMan, eventContextFactory);
            for(var pi : peerInfos) {
                nodeWithPeers.computeIfAbsent(pi.id(), k -> new LinkedHashMap<>()).put((long) i, pi);
            }
            var map = peerInfos.stream().collect(Collectors.toMap(
                    PeerInfo::id,
                    Function.identity(),
                    (e1, e2) -> {throw new RuntimeException();},
                    LinkedHashMap::new
            ));
            nodeReps.put((long) i, map);
        }

        var nodeMetrics = new ArrayList<NodeMetrics>(eventContextFactories.length);
        for(var entry : nodeReps.entrySet()) {
            var id = entry.getKey();
            var peerInfos = entry.getValue().values();
            var includeIndividual = Configuration.getBoolean("protocol.clarinet.metrics.include_individual", false);
            var withPeers = nodeWithPeers.get(id);
            nodeMetrics.add(new NodeMetrics(
                    id,
                    getType(id),
                    createRepInfo(withPeers == null ? Collections.emptyList() : withPeers.values()),
                    createRepInfo(peerInfos),
                    createRepInfo(peerInfos.stream().filter(pi -> pi.type() == NodeType.COOPERATIVE).toList()),
                    createRepInfo(peerInfos.stream().filter(pi -> pi.type() == NodeType.MALICIOUS).toList()),
                    includeIndividual ? peerInfos: Collections.emptyList()
            ));
        }

        var om = new ObjectMapper().registerModule(new Jdk8Module());
        String stringified;
        try {
            var prettyPrint = Configuration.getBoolean("protocol.clarinet.metrics.pretty_print", false);
            if(prettyPrint) {
                stringified = om.writerWithDefaultPrettyPrinter().writeValueAsString(nodeMetrics);
            } else {
                stringified = om.writeValueAsString(nodeMetrics);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var writeTargetsRaw = Configuration.getString("protocol.clarinet.metrics.write_targets", "stdout");
        var writeTargets = Arrays.asList(writeTargetsRaw.split(","));
        if(writeTargets.contains("stdout")) {
            System.out.println(stringified);
        }
        if(writeTargets.contains("file")) {
            var outputPath = Configuration.getString("protocol.clarinet.metrics.output_path", "");
            outputPath = outputPath.isEmpty() || outputPath.endsWith("/") ? outputPath : outputPath + "/";
            var numMalicious = Configuration.getInt("protocol.clarinet.num_malicious");
            var malActThresh = Configuration.getInt("protocol.clarinet.malicious_action_threshold");
            var malActPct = Configuration.getDouble("protocol.clarinet.malicious_action_percentage");
            var time = LocalDateTime.now().toString().replaceAll(":", "_");
            var fileName = String.format("simulationOutput-count%d-cycles%d-mal%d-thresh%d-pct%f-time%s.json",
                    numNodes, numCycles, numMalicious, malActThresh, malActPct, time);
            var outputFile = outputPath + fileName;
            try(var writer = new PrintWriter(outputFile, StandardCharsets.UTF_8)) {
                writer.println(stringified);
            }
        }
    }

}
