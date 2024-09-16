package clarinetsim.reputation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MessageAssessmentStore {

    private final Map<Long, List<MessageAssessment>> byPeer = new HashMap<>();
    private final Map<MessageAssessment.ID, MessageAssessment> byId = new HashMap<>();

    public void upsert(Long nodeId, String connectionId, int seqNo, MessageAssessment.Status status) {
        var id = new MessageAssessment.ID(nodeId, connectionId, seqNo);
        byId.computeIfAbsent(id, k -> {
            var assessment = new MessageAssessment(id);
            byPeer.computeIfAbsent(nodeId, nid -> new ArrayList<>()).add(assessment);
            return assessment;
        }).setStatus(status);
    }

    public List<MessageAssessment> getForPeer(Long nodeId) {
        return byPeer.getOrDefault(nodeId, new ArrayList<>());
    }

    public List<Long> assessedPeers() {
        return new ArrayList<>(byPeer.keySet());
    }

}
