package clarinetsim.metrics;

import clarinetsim.reputation.MessageAssessment;

import java.util.List;

public record PeerInfo(
        long id,
        NodeType type,
        double reputation,
        boolean trusted,
        long numMessages,
        List<MessageRecord> messages,
        long numAssessments,
        List<MessageAssessment> assessments
) {
}
