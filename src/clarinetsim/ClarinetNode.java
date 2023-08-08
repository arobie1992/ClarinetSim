package clarinetsim;

import clarinetsim.connection.ConnectionManager;
import clarinetsim.message.ClarinetMessage;
import clarinetsim.message.EventContext;
import clarinetsim.message.MessageHandler;
import peersim.cdsim.CDProtocol;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.vector.SingleValueHolder;

public class ClarinetNode extends SingleValueHolder implements CDProtocol, EDProtocol {

    private final String prefix;
    private final MessageHandler messageHandler = new MessageHandler();
    private final ConnectionManager connectionManager = new ConnectionManager();
    public ClarinetNode(String prefix) {
        super(prefix);
        this.prefix = prefix;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override public Object clone() {
        return new ClarinetNode(prefix);
    }

    @Override public void nextCycle(Node node, int protocolId) {
        NeighborUtils.selectRandomNeighbor(node, protocolId)
                .ifPresent(receiver -> connectionManager.requestConnection(node, receiver, protocolId));
        connectionManager.printConnections(node);
    }

    @Override public void processEvent(Node node, int protocolId, Object event) {
        ClarinetMessage message = (ClarinetMessage) event;
        message.accept(messageHandler, new EventContext(node, protocolId, connectionManager));
    }
}
