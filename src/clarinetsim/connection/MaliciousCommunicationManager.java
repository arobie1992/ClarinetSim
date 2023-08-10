package clarinetsim.connection;

import clarinetsim.GlobalState;
import clarinetsim.NeighborUtils;
import clarinetsim.context.EventContext;
import clarinetsim.message.Data;
import clarinetsim.reputation.Signature;
import peersim.core.Node;

public class MaliciousCommunicationManager extends CommunicationManager {

    @Override public Connection send(Node self, Connection connection, String message, int protocolId) {
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
        // we want to log the message even if we don't forward it as a record of us getting it
        log().add(ctx.self(), connection, message);
        ctx.reputationManager().witnessReview(connection, message);
        if(connection.canWitness()) {
            if(!GlobalState.isMalicious(connection.receiver())) {
                /*
                if the sender is also malicious, we cover for them
                if the sender is not malicious, we want to try to turn them against each other
                if the receiver is malicious, then we just want to pass the message on; will handle logging and querying later
                 */
                message = new Data(message.connectionId(), message.seqNo(), "falsified", Signature.INVALID);
            }
            message.witnessSign();
            NeighborUtils.send(connection.receiver(), message, ctx);
        }
        return connection;
    }

}
