package clarinetsim.reputation;

import clarinetsim.connection.Connection;
import clarinetsim.context.EventContext;
import clarinetsim.log.LogEntry;
import clarinetsim.math.MathUtils;
import clarinetsim.message.Data;
import clarinetsim.message.DataForward;
import clarinetsim.message.QueryForward;
import clarinetsim.message.QueryResponse;
import peersim.core.Node;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import static clarinetsim.reputation.MessageAssessment.Status.*;

public class ReputationManager {

    private final MessageAssessmentStore messageAssessmentStore = new MessageAssessmentStore();
    private final ReputationAlg algorithm = assessments -> {
        record Counts(double good, double total) {
            Counts add(Counts other) {
                return new Counts(good + other.good, total + other.total);
            }
            double rep() {
                return good/total;
            }
        }
        return assessments.stream()
                .filter(a -> a.getStatus() != MessageAssessment.Status.NONE)
                .map(a -> switch(a.getStatus()) {
                    case NONE -> throw new IllegalStateException();
                    case REWARD -> new Counts(1, 1);
                    case WEAK_PENALTY -> new Counts(0, 1);
                    case STRONG_PENALTY -> new Counts(0, 3);
                })
                .reduce(new Counts(1, 1), Counts::add)
                .rep();
    };

    public List<Long> assessedPeers() {
        return messageAssessmentStore.assessedPeers();
    }

    public double getReputation(long nodeId) {
        return algorithm.calculate(messageAssessmentStore.getForPeer(nodeId));
    }

    private Map<Long, Double> getReputations(Collection<Node> peers) {
        return peers.stream().collect(Collectors.toMap(Node::getID, n -> getReputation(n.getID())));
    }

    public List<Node> trusted(Collection<Node> peers) {
        var peerReps = getReputations(peers);
        var mean = MathUtils.standardDeviation(peerReps.values());
        var std = MathUtils.standardDeviation(peerReps.values());
        var threshold = mean - std;
        var trusted = peerReps.entrySet().stream()
                .filter(entry -> entry.getValue() >= threshold && entry.getValue() >= 0.5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        return peers.stream().filter(p -> trusted.contains(p.getID())).toList();
    }

    public void witnessReview(Connection connection, Data message) {
        if(message.senderSignature() == Signature.INVALID) {
            var senderId = connection.sender().getID();
            messageAssessmentStore.upsert(senderId, message.connectionId(), message.seqNo(), MessageAssessment.Status.STRONG_PENALTY);
        }
    }

    public boolean receiverReview(Connection connection, Data message) {
        var senderId = connection.sender().getID();
        var witnessId = connection.witness().orElseThrow().getID();
        if(message.witnessSignature() == Signature.INVALID) {
            messageAssessmentStore.upsert(witnessId, message.connectionId(), message.seqNo(), MessageAssessment.Status.STRONG_PENALTY);
            return false;
        }
        if(message.senderSignature() == Signature.INVALID) {
            messageAssessmentStore.upsert(senderId, message.connectionId(), message.seqNo(), MessageAssessment.Status.WEAK_PENALTY);
            messageAssessmentStore.upsert(witnessId, message.connectionId(), message.seqNo(), MessageAssessment.Status.WEAK_PENALTY);
            return true;
        }
        messageAssessmentStore.upsert(senderId, message.connectionId(), message.seqNo(), MessageAssessment.Status.REWARD);
        messageAssessmentStore.upsert(witnessId, message.connectionId(), message.seqNo(), MessageAssessment.Status.REWARD);
        return false;
    }

    public void senderReview(DataForward messageForward, EventContext ctx) {
        var message = messageForward.getData();
        ctx.connectionManager().get(message.connectionId()).map(connection -> {
            if(messageForward.getReceiverSignature() == Signature.INVALID || message.witnessSignature() == Signature.INVALID) {
                var receiverId = connection.receiver().getID();
                messageAssessmentStore.upsert(
                        receiverId,
                        message.connectionId(),
                        message.seqNo(),
                        MessageAssessment.Status.STRONG_PENALTY
                );
            } else if(message.senderSignature() == Signature.INVALID) {
                // it's fine to only check senderSignature because malicious nodes always render it invalid if they did
                // anything malicious
                connection.witness().ifPresent(w -> messageAssessmentStore.upsert(
                        w.getID(),
                        message.connectionId(),
                        message.seqNo(),
                        MessageAssessment.Status.STRONG_PENALTY
                ));
            }
            return connection;
        }).ifPresent(ctx.connectionManager()::release);
    }

    public Optional<QueryResponse> queryReview(QueryResponse queryResponse, EventContext ctx) {
        var message = queryResponse.message();
        var responderId = queryResponse.responder().getID();
        if(queryResponse.signature() == Signature.INVALID) {
            messageAssessmentStore.upsert(responderId, message.connectionId(), message.seqNo(), STRONG_PENALTY);
            return Optional.empty();
        }
        queryResponseReview(queryResponse, ctx);
        return Optional.of(queryResponse);
    }

    public void queryForwardReview(QueryForward queryForward, EventContext ctx) {
        var resp = queryForward.queryResponse();
        var message = resp.message();
        if(queryForward.signature() == Signature.INVALID || resp.signature() == Signature.INVALID) {
            // here we can assume that F is sure of the ID
            messageAssessmentStore.upsert(queryForward.forwarder().getID(), message.connectionId(), message.seqNo(), STRONG_PENALTY);
            return;
        }
        queryResponseReview(resp, ctx);
    }

    private void queryResponseReview(QueryResponse queryResponse, EventContext ctx) {
        var responderId = queryResponse.responder().getID();
        var message = queryResponse.message();
        ctx.communicationManager().find(queryResponse).ifPresent(logEntry -> {
            var refMessage = logEntry.message();
            // this is equivalent to checking the hash
            // hash is to cut down on data sent over the network which isn't a concern here
            if(refMessage.data().equals(message.data()) && message.senderSignature() == refMessage.senderSignature()) {
                messageAssessmentStore.upsert(queryResponse.responder().getID(), message.connectionId(), message.seqNo(), REWARD);
            } else {
                if (directCommunication(ctx.self(), queryResponse.responder(), logEntry)) {
                    messageAssessmentStore.upsert(responderId, message.connectionId(), message.seqNo(), STRONG_PENALTY);
                } else {
                    logEntry.participants().stream()
                            .filter(n -> n.getID() != ctx.self().getID())
                            .forEach(n -> messageAssessmentStore.upsert(
                                    responderId,
                                    message.connectionId(),
                                    message.seqNo(),
                                    WEAK_PENALTY
                            ));
                }
            }
            ctx.communicationManager().markQueried(logEntry, queryResponse.responder());
        });
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
        var peerReps = messageAssessmentStore.assessedPeers().stream().collect(Collectors.toMap(
                Function.identity(),
                id -> algorithm.calculate(messageAssessmentStore.getForPeer(id))
        ));
        System.out.println("Node " + node.getID() + " peer reputations: " + peerReps);
    }

    public Map<Long, List<MessageAssessment>> getMessageAssessments() {
        return messageAssessmentStore.assessedPeers().stream().collect(Collectors.toMap(
                Function.identity(),
                messageAssessmentStore::getForPeer
        ));
    }

}
