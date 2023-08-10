package clarinetsim.reputation;

import clarinetsim.connection.Connection;
import clarinetsim.context.EventContext;
import clarinetsim.message.Data;
import clarinetsim.message.QueryForward;
import clarinetsim.message.QueryResponse;
import peersim.core.Node;

import java.util.Optional;

public class MaliciousReputationManager extends ReputationManager {
    public MaliciousReputationManager(int initialReputation, int minTrustedReputation, int weakValue, int strongValue) {
        super(initialReputation, minTrustedReputation, weakValue, strongValue);
    }

    @Override public boolean evaluate(Node node) {
        // malicious nodes will do their own targeted witness selection elsewhere
        // in general, they prefer other malicious nodes, but will use a cooperative
        // witness if no malicious nodes are available
        return true;
    }

    // all no-ops, theory is that malicious nodes don't care about other nodes' reputations
    // since we're assuming one collusion group
    @Override public void witnessReview(Connection connection, Data message) {}
    @Override public void review(Connection connection, Data message) {}
    @Override public Optional<QueryResponse> review(QueryResponse queryResponse, EventContext ctx) {
        return Optional.empty();
    }
    @Override public void review(QueryForward queryForward, EventContext ctx) {}
}
