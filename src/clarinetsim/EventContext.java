package clarinetsim;

import clarinetsim.connection.Connections;
import peersim.core.Node;

public class EventContext {
    private final Node node;
    private final int processId;
    private final Connections connections;

    public EventContext(Node node, int processId, Connections connections) {
        this.node = node;
        this.processId = processId;
        this.connections = connections;
    }

    public Node getNode() {
        return node;
    }

    public int getProcessId() {
        return processId;
    }

    public Connections getConnections() {
        return connections;
    }
}
