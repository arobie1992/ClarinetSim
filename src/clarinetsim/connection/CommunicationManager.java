package clarinetsim.connection;

import clarinetsim.NeighborUtils;
import clarinetsim.context.EventContext;
import clarinetsim.log.Log;
import clarinetsim.log.LogEntry;
import clarinetsim.message.*;
import clarinetsim.reputation.Signature;
import peersim.core.Node;

import java.util.List;
import java.util.Optional;

public class CommunicationManager {

    private final Log log = new Log();

    public Connection send(Node self, Connection connection, String message, int protocolId) {
        Node witness = connection.witness()
                .orElseThrow(() -> new UnsupportedOperationException("Cannot send on connection without witness selected"));
        Data msg = new Data(connection.connectionId(), connection.nextSeqNo(), message, Signature.VALID);
        NeighborUtils.send(self, witness, msg, protocolId);
        log.add(self, connection, msg);
        return connection;
    }

    public Connection forward(Connection connection, Data message, EventContext ctx) {
        // we want to log the message even if we don't forward it as a record of us getting it
        log.add(ctx.self(), connection, message);
        ctx.reputationManager().witnessReview(connection, message);
        if(connection.canWitness()) {
            message.witnessSign();
            NeighborUtils.send(connection.receiver(), message, ctx);
        }
        return connection;
    }

    public void forward(QueryResponse queryResponse, EventContext ctx) {
        log.find(queryResponse.message().connectionId(), queryResponse.message().seqNo()).ifPresent(logEntry -> {
            if(queryResponse.signature() != Signature.VALID) {
                // only forward if the signature is valid
                return;
            }
            var recipient = logEntry.participants().stream()
                    .filter(node -> node.getID() != queryResponse.responder().getID())
                    .filter(node -> node.getID() != ctx.self().getID())
                    .toList();
            if(recipient.size() != 1) {
                // received a query response from someone who wasn't involved in the connection
                // right now disregard it, but might be good case for penalization
                return;
            }
            var fwd = new QueryForward(queryResponse, ctx.self(), Signature.VALID);
            NeighborUtils.send(recipient.getFirst(), fwd, ctx);
            log.add(fwd);
        });
    }

    public Connection receive(Connection connection, Data message, EventContext ctx) {
        log.add(ctx.self(), connection, message);
        if(ctx.reputationManager().receiverReview(connection, message)) {
            var messageForward = new DataForward(message, Signature.VALID);
            NeighborUtils.send(connection.sender(), messageForward, ctx);
        }
        return connection;
    }

    public Optional<LogEntry> selectRandom() {
        return log.random();
    }

    public void markQueried(LogEntry logEntry, Node node) {
        log.markQueried(logEntry, node);
    }

    public void query(Node self, LogEntry logEntry, int protocolId) {
        logEntry.selectQueryTarget(self).ifPresent(candidate -> {
            var msg = new Query(logEntry, self);
            NeighborUtils.send(self, candidate, msg, protocolId);
            log.add(msg);
        });
    }

    public Optional<LogEntry> find(Query query) {
        log.add(query);
        return log.find(query.connectionId(), query.seqNo());
    }

    public Optional<LogEntry> find(QueryResponse queryResponse) {
        log.add(queryResponse);
        return log.find(queryResponse.message().connectionId(), queryResponse.message().seqNo());
    }

    public Optional<LogEntry> find(QueryForward queryForward) {
        log.add(queryForward);
        return find(queryForward.queryResponse());
    }

    public void reply(Query query, LogEntry logEntry, EventContext ctx) {
        var resp = new QueryResponse(logEntry.message(), ctx.self(), Signature.VALID);
        NeighborUtils.send(query.querier(), resp, ctx);
        log.add(resp);
    }

    /**
     * Access point to the log for the subclass so it can log as well
     * @return the instance's log
     */
    Log log() {
        return log;
    }

    public List<LogEntry> allMessages() {
        return log.all();
    }

    public void printLog(Node node) {
        System.out.println("Node " + node.getID() + " " + log);
    }
}
