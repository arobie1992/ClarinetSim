package clarinetsim.reputation;

import clarinetsim.connection.Connection;
import clarinetsim.context.EventContext;
import clarinetsim.log.LogEntry;
import clarinetsim.message.Data;
import clarinetsim.message.QueryForward;
import clarinetsim.message.QueryResponse;
import peersim.config.Configuration;
import peersim.core.Node;

import java.util.Map;
import java.util.Optional;

public class ReputationManager {

    private final Reputations reputations;
    private final Penalty weakPenalty;
    private final Penalty strongPenalty;

    public ReputationManager(String prefix) {
        var repTypeStr = Configuration.getString(prefix + ".reputation.type", ReputationType.SUBTRACTIVE.name());
        this.reputations = switch(ReputationType.valueOf(repTypeStr)) {
            case SUBTRACTIVE -> new SubtractiveReputations(prefix);
            case PROPORTIONAL -> new ProportionalReputations(prefix);
        };
        this.weakPenalty = new Penalty(Configuration.getInt(prefix + ".weak_penalty_value", 1));
        this.strongPenalty = new Penalty(Configuration.getInt(prefix + ".strong_penalty_value", 3));
    }

    /**
     * Evalute whether the provided node is an eligible witness
     * @param node the node to evaluate
     * @return true if eligible; false if not
     */
    public boolean evaluate(Node node) {
        return reputations.evaluate(node);
    }

    public void witnessReview(Connection connection, Data message) {
        if(message.senderSignature() != Signature.VALID) {
            reputations.penalize(connection.sender(), strongPenalty);
        } else {
            reputations.reward(connection.sender());
        }
    }

    public void review(Connection connection, Data message) {
        var witness = connection.witness().orElseThrow(IllegalStateException::new);
        boolean penalized = false;
        if(message.witnessSignature() != Signature.VALID) {
            reputations.penalize(witness, strongPenalty);
            penalized = true;
        }
        if(message.senderSignature() != Signature.VALID) {
            reputations.penalize(witness, weakPenalty);
            reputations.penalize(connection.sender(), weakPenalty);
            penalized = true;
        }
        if(!penalized) {
            reputations.reward(witness);
            reputations.reward(connection.sender());
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
            reputations.penalize(queryForward.forwarder(), strongPenalty);
            return;
        }
        reputations.reward(queryForward.forwarder());
        logEntryOpt.ifPresent(logEntry -> review(queryForward.queryResponse(), logEntry, ctx));
    }

    private void review(QueryResponse queryResponse, LogEntry logEntry, EventContext ctx) {
        if(queryResponse.signature() != Signature.VALID) {
            reputations.penalize(queryResponse.responder(), strongPenalty);
        } else if(!logEntry.message().data().equals(queryResponse.message().data())) {
            // only check the query response contents if the signature is valid
            if(directCommunication(ctx.self(), queryResponse.responder(), logEntry)) {
                reputations.penalize(queryResponse.responder(), strongPenalty);
            } else {
                logEntry.participants().stream()
                        .filter(n -> n.getID() != ctx.self().getID())
                        .forEach(n -> reputations.penalize(n, weakPenalty));
            }
        } else {
            reputations.reward(queryResponse.responder());
        }
    }

    private boolean directCommunication(Node self, Node responder, LogEntry logEntry) {
        int selfPos = -1;
        int responderPos = -1;
        var participants = logEntry.participants();
        for(int i = 0; i < participants.size() && (selfPos == -1 || responderPos == -1); i++) {
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

    public Map<Long, Integer> reputations() {
        return reputations.reputations();
    }

}
