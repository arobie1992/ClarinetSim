package clarinetsim.reputation;

import clarinetsim.connection.Connection;
import clarinetsim.message.Data;
import clarinetsim.message.EventContext;
import clarinetsim.message.QueryForward;
import clarinetsim.message.QueryResponse;
import peersim.core.Node;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ReputationManager {

    private final Map<Long, Integer> reputations = new ConcurrentHashMap<>();

    /*
    TODO:
     1. Query reputation
     2. Witness selection based on reputation
     */

    private void penalize(Node node, Penalty penalty) {
        reputations.compute(node.getID(), (k, v) -> {
            int reputation = v == null ? 100 : v;
            return reputation - penalty.value();
        });
    }

    public void witnessReview(Connection connection, Data message) {
        if(message.senderSignature() != Signature.VALID) {
            penalize(connection.sender(), Penalty.STRONG);
        }
    }

    public void review(Connection connection, Data message) {
        var witness = connection.witness().orElseThrow(IllegalStateException::new);
        if(message.witnessSignature() != Signature.VALID) {
            penalize(witness, Penalty.STRONG);
        }
        if(message.senderSignature() != Signature.VALID) {
            penalize(witness, Penalty.WEAK);
            penalize(connection.sender(), Penalty.WEAK);
        }
    }

    public Optional<QueryResponse> review(QueryResponse queryResponse, EventContext ctx) {
        return ctx.communicationManager()
                .find(queryResponse)
                // if we didn't find a matching log entry, someone sent us one erroneously so don't return anything
                .map(logEntry -> {
                    // this is idempotent, so it doesn't matter if multiple threads perform it simultaneously
                    logEntry.markQueried(queryResponse.responder());
                    return queryResponse;
                });
    }

    public void review(QueryForward queryForward, EventContext ctx) {
        // at the moment nothing further to do than log the queryForward
        ctx.communicationManager().find(queryForward);
    }

    public void printReputations(Node node) {
        System.out.println("Node " + node.getID() + " " + reputations);
    }

}
