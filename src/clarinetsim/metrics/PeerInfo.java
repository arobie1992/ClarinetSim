package clarinetsim.metrics;

import clarinetsim.reputation.MessageAssessment;

import java.util.List;

public record PeerInfo(
        long id,
        NodeType type,
        double reputation,
        boolean trusted,
        List<MessageAssessment> assessments
) {
}
