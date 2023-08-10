package clarinetsim.reputation;

import clarinetsim.connection.Connection;
import clarinetsim.log.LogEntry;
import clarinetsim.message.Data;
import clarinetsim.context.EventContext;
import clarinetsim.message.QueryForward;
import clarinetsim.message.QueryResponse;
import peersim.core.Node;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ReputationManager {

    private final Map<Long, Integer> reputations = new ConcurrentHashMap<>();
    private final int initialReputation;
    private final int minTrustedReputation;
    private final Penalty weakPenalty;
    private final Penalty strongPenalty;

    public ReputationManager(int initialReputation, int minTrustedReputation, int weakValue, int strongValue) {
        this.initialReputation = initialReputation;
        this.minTrustedReputation = minTrustedReputation;
        this.weakPenalty = new Penalty(weakValue);
        this.strongPenalty = new Penalty(strongValue);
    }

    /**
     * Evalute whether the provided node is an eligible witness
     * @param node the node to evaluate
     * @return true if eligible; false if not
     */
    public boolean evaluate(Node node) {
        return reputations.computeIfAbsent(node.getID(), k -> initialReputation) >= minTrustedReputation;
    }

    private void penalize(Node node, Penalty penalty) {
        reputations.compute(node.getID(), (k, v) -> {
            int reputation = v == null ? initialReputation : v;
            return reputation - penalty.value();
        });
    }

    public void witnessReview(Connection connection, Data message) {
        if(message.senderSignature() != Signature.VALID) {
            penalize(connection.sender(), strongPenalty);
        }
    }

    public void review(Connection connection, Data message) {
        var witness = connection.witness().orElseThrow(IllegalStateException::new);
        if(message.witnessSignature() != Signature.VALID) {
            penalize(witness, strongPenalty);
        }
        if(message.senderSignature() != Signature.VALID) {
            penalize(witness, weakPenalty);
            penalize(connection.sender(), weakPenalty);
        }
    }

    public Optional<QueryResponse> review(QueryResponse queryResponse, EventContext ctx) {
        return ctx.communicationManager()
                .find(queryResponse)
                // if we didn't find a matching log entry, someone sent us one erroneously so don't return anything
                .map(logEntry -> {
                    review(queryResponse, logEntry, ctx);
                    // this is idempotent, so it doesn't matter if multiple threads perform it simultaneously
                    logEntry.markQueried(queryResponse.responder());
                    return queryResponse;
                });
    }

    public void review(QueryForward queryForward, EventContext ctx) {
        var logEntryOpt = ctx.communicationManager().find(queryForward);
        if(queryForward.signature() != Signature.VALID) {
            penalize(queryForward.forwarder(), strongPenalty);
            return;
        }
        logEntryOpt.ifPresent(logEntry -> review(queryForward.queryResponse(), logEntry, ctx));
    }

    private void review(QueryResponse queryResponse, LogEntry logEntry, EventContext ctx) {
        if(queryResponse.signature() != Signature.VALID) {
            penalize(queryResponse.responder(), strongPenalty);
        } else if(!logEntry.message().data().equals(queryResponse.message().data())) {
            // only check the query response contents if the signature is valid
            if(directCommunication(ctx.self(), queryResponse.responder(), logEntry)) {
                penalize(queryResponse.responder(), strongPenalty);
            } else {
                logEntry.participants().stream()
                        .filter(n -> n.getID() != ctx.self().getID())
                        .forEach(n -> penalize(n, weakPenalty));
            }
        }
    }

    private boolean directCommunication(Node self, Node responder, LogEntry logEntry) {
        int selfPos = -1;
        int responderPos = -1;
        var participants = logEntry.participants();
        for(int i = 0; i < participants.size() && selfPos == -1 && responderPos == -1; i++) {
            var participant = participants.get(i);
            if(participant.getID() == self.getID()) {
                selfPos = i;
            } else if(participant.getID() == responder.getID()) {
                responderPos = i;
            }
        }
        return Math.abs(selfPos - responderPos) == 1;
    }

    public void printReputations(Node node) {
        System.out.println("Node " + node.getID() + " " + reputations);
    }

}
