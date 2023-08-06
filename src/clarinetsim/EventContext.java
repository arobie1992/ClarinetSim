package clarinetsim;

import clarinetsim.connection.Connections;
import peersim.core.Node;

public class EventContext {
    private final Node node;
    private final int protocolId;
    private final Connections connections;
    private final ClarinetNode clarinetNode;

    public EventContext(Node node, int protocolId, Connections connections, ClarinetNode clarinetNode) {
        this.node = node;
        this.protocolId = protocolId;
        this.connections = connections;
        this.clarinetNode = clarinetNode;
    }

    public Node getNode() {
        return node;
    }

    public int getProtocolId() {
        return protocolId;
    }

    public Connections getConnections() {
        return connections;
    }

    public ClarinetNode getClarinetNode() {
        return clarinetNode;
    }
}
