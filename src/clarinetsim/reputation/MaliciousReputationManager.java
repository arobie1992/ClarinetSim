package clarinetsim.reputation;

import clarinetsim.connection.Connection;
import clarinetsim.context.EventContext;
import clarinetsim.message.Data;
import clarinetsim.message.DataForward;
import clarinetsim.message.QueryForward;
import clarinetsim.message.QueryResponse;
import peersim.core.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class MaliciousReputationManager extends ReputationManager {
    @Override
    public List<Node> trusted(Collection<Node> peers) {
        return new ArrayList<>(peers);
    }

    // noop
    @Override
    public void witnessReview(Connection connection, Data message) {}

    // noop and never want to forward
    @Override
    public boolean receiverReview(Connection connection, Data message) {
        return false;
    }

    @Override
    public void senderReview(DataForward dataForward, EventContext ctx) {}

    // noop and never want to forward
    @Override
    public Optional<QueryResponse> queryReview(QueryResponse queryResponse, EventContext ctx) {
        return Optional.empty();
    }

    // noop
    @Override
    public void queryForwardReview(QueryForward queryForward, EventContext ctx) {}
}