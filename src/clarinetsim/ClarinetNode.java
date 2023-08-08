package clarinetsim;

import clarinetsim.connection.CommunicationManager;
import clarinetsim.connection.ConnectionManager;
import clarinetsim.connection.Type;
import clarinetsim.message.ClarinetMessage;
import clarinetsim.message.EventContext;
import clarinetsim.message.MessageHandler;
import peersim.cdsim.CDProtocol;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.vector.SingleValueHolder;

public class ClarinetNode extends SingleValueHolder implements CDProtocol, EDProtocol {

    private final String prefix;
    private final MessageHandler messageHandler = new MessageHandler();
    private final ConnectionManager connectionManager = new ConnectionManager();
    private final CommunicationManager communicationManager = new CommunicationManager();

    public ClarinetNode(String prefix) {
        super(prefix);
        this.prefix = prefix;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override public Object clone() {
        return new ClarinetNode(prefix);
    }

    @Override public void nextCycle(Node node, int protocolId) {
        switch(CommonState.r.nextInt(2)) {
            case 0:
                NeighborUtils.selectRandomNeighbor(node, protocolId)
                        .ifPresent(receiver -> connectionManager.requestConnection(node, receiver, protocolId));
                break;
            case 1:
                connectionManager.selectRandom(Type.OUTGOING)
                        .map(connection -> communicationManager.send(node, connection, "Test message", protocolId))
                        .ifPresent(connectionManager::release);
                break;
        }
//        connectionManager.printConnections(node);
        communicationManager.printLog(node);
    }

    @Override public void processEvent(Node node, int protocolId, Object event) {
        ClarinetMessage message = (ClarinetMessage) event;
        message.accept(messageHandler, new EventContext(node, protocolId, connectionManager, communicationManager));
    }
}
