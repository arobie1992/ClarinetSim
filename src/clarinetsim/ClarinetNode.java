package clarinetsim;

import peersim.cdsim.CDProtocol;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.transport.Transport;
import peersim.vector.SingleValueHolder;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClarinetNode extends SingleValueHolder implements CDProtocol, EDProtocol {

    private final String prefix;

    private final Lock connectionLock = new ReentrantLock();

    private final int maxConnections = 5;
    private final HashMap<Long, Node> incomingConnections = new HashMap<>();
    private final HashMap<Long, Node> outgoingConnections = new HashMap<>();

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
            ConnectResponseMessage resp;
            synchronized (connectionLock) {
                boolean hasCapacity = !atMaxConnections();
                if(hasCapacity) {
                    incomingConnections.put(msg.getRequestor().getID(), msg.getRequestor());
                }
                resp = new ConnectResponseMessage(node, hasCapacity);
            }
            sendMessage(node, msg.getRequestor(), resp, pid);
        } else if(event instanceof ConnectResponseMessage) {
            ConnectResponseMessage msg = (ConnectResponseMessage) event;
            synchronized (connectionLock) {
                if(!msg.isAccepted()) {
                    outgoingConnections.remove(msg.getNeighbor().getID());
                }
            }
        }
    }

    @Override public void nextCycle(Node node, int protocolID) {
        requestConnection(node, protocolID);
        System.out.println(String.format("incoming: %s, outgoing: %s", incomingConnections.keySet(), outgoingConnections.keySet()));
    }

    private void requestConnection(Node node, int protocolID) {
        int linkableId = FastConfig.getLinkable(protocolID);
        Linkable linkable = (Linkable) node.getProtocol(linkableId);
        if (linkable.degree() > 0) {
            Node neighbor;
            ConnectRequestMessage msg;
            synchronized (connectionLock) {
                if(atMaxConnections()) {
                    return;
                }
                int neighborId = CommonState.r.nextInt(linkable.degree());
                neighbor = linkable.getNeighbor(neighborId);
                outgoingConnections.put(neighbor.getID(), neighbor);
                msg = new ConnectRequestMessage(node);
            }
            sendMessage(node, neighbor, msg, protocolID);
        }
    }

    private boolean atMaxConnections() {
        return incomingConnections.size() + outgoingConnections.size() == maxConnections;
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
