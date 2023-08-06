package clarinetsim;

import clarinetsim.connection.Connections;
import peersim.core.Node;

public class EventContext {
    private final Node node;
    private final int protocolId;
    private final Connections connections;

    public EventContext(Node node, int protocolId, Connections connections) {
        this.node = node;
        this.protocolId = protocolId;
        this.connections = connections;
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
}
