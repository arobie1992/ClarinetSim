package clarinetsim.metrics;

import java.util.List;

public record NodeMetrics(
        long id,
        NodeType type,
        ReputationInformation selfWithPeers,
        ReputationInformation allPeers,
        ReputationInformation cooperativePeers,
        ReputationInformation maliciousPeers,
        List<PeerInfo> individualPeers
) {
}
