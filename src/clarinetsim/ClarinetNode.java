package clarinetsim;

import clarinetsim.connection.Connections;
import clarinetsim.message.ClarinetMessage;
import clarinetsim.message.ConnectRequestMessage;
import clarinetsim.message.MessageHandler;
import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.vector.SingleValueHolder;

import java.util.Optional;

public class ClarinetNode extends SingleValueHolder implements CDProtocol, EDProtocol {

    private final String prefix;
    private final Connections connections = new Connections();
    private final MessageHandler messageHandler = new MessageHandler();

    public ClarinetNode(String prefix) {
        super(prefix);
        this.prefix = prefix;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override public Object clone() {
        return new ClarinetNode(prefix);
    }

    @Override public void processEvent(Node node, int pid, Object event) {
        ClarinetMessage msg = (ClarinetMessage) event;
        msg.visit(messageHandler, new EventContext(node, pid, connections));
    }

    @Override public void nextCycle(Node node, int protocolID) {
        switch(CommonState.r.nextInt(2)) {
            case 0:
                // if connections are full, attempt a connection
                // otherwise continue on to send a message
                if(connect(node, protocolID)) {
                    break;
                }
            case 1:
                // send message
                connections.sendToRandomTarget(node, protocolID);
                break;
        }
    }

    private Optional<String> requestConnection(Node node, int protocolId) {
        if(connections.atMax()) {
            return Optional.empty();
        }

        int linkableId = FastConfig.getLinkable(protocolId);
        Linkable linkable = (Linkable) node.getProtocol(linkableId);
        if (linkable.degree() == 0) {
            return Optional.empty();
        }

        int neighborId = CommonState.r.nextInt(linkable.degree());
        Node neighbor = linkable.getNeighbor(neighborId);
        // add the node to connections to reserve space
        // this gets rolled back if it gets rejected
        String connectionId = connections.addOutgoing(node, neighbor, protocolId).orElseThrow(IllegalStateException::new);
        ConnectRequestMessage msg = new ConnectRequestMessage(connectionId, node);
        CommunicationUtils.sendMessage(node, neighbor, msg, protocolId);
        return Optional.ofNullable(connectionId);
    }

    private boolean connect(Node node, int protocolId) {
        Optional<String> connectionIdOpt = requestConnection(node, protocolId);
        connectionIdOpt.ifPresent(connectionId -> connections.tryWitness(connectionId, node, protocolId));
        return connectionIdOpt.isPresent();
    }
}
