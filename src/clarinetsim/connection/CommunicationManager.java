package clarinetsim.connection;

import clarinetsim.NeighborUtils;
import clarinetsim.message.Data;
import clarinetsim.message.EventContext;
import clarinetsim.message.Log;
import peersim.core.Node;

public class CommunicationManager {

    private final Log log = new Log();

    public Connection send(Node self, Connection connection, String message, int protocolId) {
        Node witness = connection.witness()
                .orElseThrow(() -> new UnsupportedOperationException("Cannot send on connection without witness selected"));
        Data msg = new Data(connection.connectionId(), connection.nextSeqNo(), message);
        NeighborUtils.send(self, witness, msg, protocolId);
        log.add(msg);
        return connection;
    }

    public Connection forward(Connection connection, Data message, EventContext ctx) {
        // we want to log the message even if we don't forward it as a record of us getting it
        log.add(message);
        if(connection.canWitness()) {
            NeighborUtils.send(connection.receiver(), message, ctx);
        }
        return connection;
    }

    public Connection receive(Connection connection, Data message) {
        log.add(message);
        // at the moment no reason to need to check if it can receive
        return connection;
    }

    public void printLog(Node node) {
        System.out.println("Node " + node.getID() + " " + log);
    }
}
