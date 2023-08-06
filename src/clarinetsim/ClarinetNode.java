package clarinetsim;

import clarinetsim.connection.Connections;
import clarinetsim.message.ClarinetMessage;
import clarinetsim.message.ConnectRequestMessage;
import clarinetsim.message.ConnectResponseMessage;
import clarinetsim.message.DataMessage;
import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;
import peersim.vector.SingleValueHolder;

import java.util.*;

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
        ClarinetMessage msg = (ClarinetMessage) event;
        msg.visit(this, new EventContext(node, pid));
    }

    public void handle(ConnectRequestMessage msg, EventContext ctx) {
        Optional<String> connectionIdOpt = connections.addIncoming(msg.getConnectionId(), ctx.getNode(), msg.getRequestor());
        ConnectResponseMessage resp = new ConnectResponseMessage(msg.getConnectionId(), connectionIdOpt.isPresent());
        sendMessage(ctx.getNode(), msg.getRequestor(), resp, ctx.getProcessId());
    }

    public void handle(ConnectResponseMessage msg, EventContext ctx) {
        if(!msg.isAccepted()) {
            // connection request failed so rollback the connection
            connections.removeOutgoing(msg.getConnectionId());
        }
    }

    public void handle(DataMessage msg, EventContext ctx) {
        System.out.println(msg.getData());
    }

    @Override public void nextCycle(Node node, int protocolID) {
        switch(CommonState.r.nextInt(2)) {
            case 0:
                // if connections are full, attempt a connection
                // otherwise continue on to send a message
                if(requestConnection(node, protocolID)) {
                    break;
                }
            case 1:
                // send message
                sendDataMessage(node, protocolID);
                break;
        }
    }

    private void sendDataMessage(Node node, int protocolId) {
        connections.randomOutgoingSyncOp(e -> {
            DataMessage msg = new DataMessage(e.getKey(), node, "Test message " + CommonState.r.nextInt());
            sendMessage(node, e.getValue().getTarget(), msg, protocolId);
        });
    }

    private boolean requestConnection(Node node, int protocolId) {
        if(connections.atMax()) {
            return false;
        }

        int linkableId = FastConfig.getLinkable(protocolId);
        Linkable linkable = (Linkable) node.getProtocol(linkableId);
        if (linkable.degree() == 0) {
            return false;
        }

        int neighborId = CommonState.r.nextInt(linkable.degree());
        Node neighbor = linkable.getNeighbor(neighborId);
        // add the node to connections to reserve space
        // this gets rolled back if it gets rejected
        String connectionId = connections.addOutgoing(node, neighbor).orElseThrow(IllegalStateException::new);
        ConnectRequestMessage msg = new ConnectRequestMessage(connectionId, node);
        sendMessage(node, neighbor, msg, protocolId);
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
