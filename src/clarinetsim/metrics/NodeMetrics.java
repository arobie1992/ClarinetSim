package clarinetsim.metrics;

import java.util.Collection;

public record NodeMetrics(
        long id,
        NodeType type,
        ReputationInformation selfWithPeers,
        ReputationInformation allPeers,
        ReputationInformation cooperativePeers,
        ReputationInformation maliciousPeers,
        Collection<PeerInfo> individualPeers
) {
}
