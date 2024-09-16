package clarinetsim.metrics;

import clarinetsim.reputation.Signature;

public record MessageRecord(
        String connectionId,
        int seqNo,
        String data,
        Signature senderSignature,
        Signature witnessSignature,
        long sender,
        long witness,
        long receiver
) {
}
