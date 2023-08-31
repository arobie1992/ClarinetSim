package clarinetsim.connection;

import clarinetsim.GlobalState;
import clarinetsim.NeighborUtils;
import clarinetsim.context.EventContext;
import clarinetsim.log.LogEntry;
import clarinetsim.message.Data;
import clarinetsim.message.Query;
import clarinetsim.message.QueryResponse;
import clarinetsim.reputation.Signature;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;

import java.util.concurrent.atomic.AtomicInteger;

public class MaliciousCommunicationManager extends CommunicationManager {

    private final int maliceActionThreshold;
    private final AtomicInteger counter = new AtomicInteger();
    // between 0 and 1
    private final double maliceActionPercentage;

    public MaliciousCommunicationManager(String prefix) {
        this.maliceActionThreshold = Configuration.getInt(prefix + ".malicious_action_threshold");
        this.maliceActionPercentage = Configuration.getDouble(prefix + ".malicious_action_percentage");
    }

    private boolean checkActMaliciously() {
        return counter.incrementAndGet() > maliceActionThreshold && CommonState.r.nextDouble() < maliceActionPercentage;
    }

    @Override public Connection send(Node self, Connection connection, String message, int protocolId) {
        if(!checkActMaliciously()) {
            return super.send(self, connection, message, protocolId);
        }
        Node witness = connection.witness()
                .orElseThrow(() -> new UnsupportedOperationException("Cannot send on connection without witness selected"));
        Node receiver = connection.receiver();
        Data msg;
        if(GlobalState.isMalicious(receiver) || GlobalState.isMalicious(witness)) {
            /*
            receiver - a malicious sender has no reason to deceive a malicious receiver
                       currently assuming all malicious nodes are part of the same collusion group
            witness - a malicious witness will cover for the malicious sender
                      send a valid sig so the witness can make the determination
             */
            msg = new Data(connection.connectionId(), connection.nextSeqNo(), message, Signature.VALID);
        } else {
            msg = new Data(connection.connectionId(), connection.nextSeqNo(), message, Signature.INVALID);
        }
        NeighborUtils.send(self, witness, msg, protocolId);
        log().add(self, connection, msg);
        return connection;
    }

    @Override public Connection forward(Connection connection, Data message, EventContext ctx) {
        if(!checkActMaliciously()) {
            return super.forward(connection, message, ctx);
        }
        // we want to log the message even if we don't forward it as a record of us getting it
        log().add(ctx.self(), connection, message);
        ctx.reputationManager().witnessReview(connection, message);
        if(connection.canWitness()) {
            // if the receiver is malicious, then we just want to pass the message on
            if(!GlobalState.isMalicious(connection.receiver())) {
                /*
                if the sender is also malicious, we want to forward the "right" data but give the sender
                deniability by altering the signature
                if the sender is cooperative, we want to alter the message to try to turn the cooperative nodes
                against each other
                */
                var data = GlobalState.isMalicious(connection.sender()) ? message.data() : "falsified";
                message = new Data(message.connectionId(), message.seqNo(), data, Signature.INVALID);
            }
            message.witnessSign();
            NeighborUtils.send(connection.receiver(), message, ctx);
        }
        return connection;
    }

    public void reply(Query query, LogEntry logEntry, EventContext ctx) {
        if(!checkActMaliciously()) {
            super.reply(query, logEntry, ctx);
            return;
        }
        QueryResponse resp;
        if(GlobalState.isMalicious(query.querier())
                || (GlobalState.isMalicious(logEntry.sender()) && GlobalState.isMalicious(logEntry.receiver()))
        ) {
            resp = new QueryResponse(logEntry.message(), ctx.self(), Signature.VALID);
        } else {
            var message = logEntry.message();
            // the malicious node can generate a valid signature for the message if it was the sender
            var signature = logEntry.sender().getID() == ctx.self().getID() ? Signature.VALID : Signature.INVALID;
            var falsified = new Data(message.connectionId(), message.seqNo(), "falsified", signature);
            resp = new QueryResponse(falsified, ctx.self(), Signature.VALID);
        }
        NeighborUtils.send(query.querier(), resp, ctx);
        log().add(resp);
    }

}
