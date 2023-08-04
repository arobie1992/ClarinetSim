package clarinetsim;

import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;
import peersim.vector.SingleValueHolder;

import java.util.Optional;

public class ClarinetNode extends SingleValueHolder implements CDProtocol, EDProtocol {

    private final String prefix;

    private final Connections connections = new Connections();

    public ClarinetNode(String prefix) {
        super(prefix);
        this.prefix = prefix;
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override public Object clone() {
        return new ClarinetNode(prefix);
    }

    @Override public void processEvent(Node node, int pid, Object event) {
        if(event instanceof ConnectRequestMessage) {
            ConnectRequestMessage msg = (ConnectRequestMessage) event;
            Optional<String> connectionIdOpt = connections.addIncoming(msg.getConnectionId(), msg.getRequestor());
            ConnectResponseMessage resp = new ConnectResponseMessage(msg.getConnectionId(), connectionIdOpt.isPresent());
            sendMessage(node, msg.getRequestor(), resp, pid);
        } else if(event instanceof ConnectResponseMessage) {
            ConnectResponseMessage msg = (ConnectResponseMessage) event;
            if(!msg.isAccepted()) {
                // connection request failed so rollback the connection
                connections.removeOutgoing(msg.getConnectionId());
            }
        } else if(event instanceof DataMessage) {
            DataMessage msg = (DataMessage) event;
            System.out.println(msg.getData());
        }
    }

    @Override public void nextCycle(Node node, int protocolID) {
        switch(CommonState.r.nextInt(2)) {
            case 1:
                // if connections are full, attempt a connection
                // otherwise continue on to send a message
                if(requestConnection(node, protocolID)) {
                    break;
                }
            case 2:
                // send message
                sendDataMessage(node, protocolID);
                break;
        }
    }

    private void sendDataMessage(Node node, int protocolId) {
        connections.randomSyncOp(e -> {
            DataMessage msg = new DataMessage(e.getKey(), node, "Test message " + CommonState.r.nextInt());
            sendMessage(node, e.getValue(), msg, protocolId);
        });
    }

    private boolean requestConnection(Node node, int protocolID) {
        if(connections.atMax()) {
            return false;
        }

        int linkableId = FastConfig.getLinkable(protocolID);
        Linkable linkable = (Linkable) node.getProtocol(linkableId);
        if (linkable.degree() > 0) {
            int neighborId = CommonState.r.nextInt(linkable.degree());
            Node neighbor = linkable.getNeighbor(neighborId);
            // add the node to connections to reserve space
            // this gets rolled back if it gets rejected
            String connectionId = connections.addOutgoing(node, neighbor).orElseThrow(IllegalStateException::new);
            ConnectRequestMessage msg = new ConnectRequestMessage(connectionId, node);
            sendMessage(node, neighbor, msg, protocolID);
        }
        return true;
    }

    private boolean sendMessage(Node node, Node neighbor, Object message, int protocolID) {
        // XXX quick and dirty handling of failures
        // (message would be lost anyway, we save time)
        if(!neighbor.isUp()) {
            return false;
        }

        int transportId = FastConfig.getTransport(protocolID);
        Transport transport = (Transport) node.getProtocol(transportId);
        transport.send(node, neighbor, message, protocolID);
        return true;
    }
}
