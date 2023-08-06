package clarinetsim;

import peersim.core.Node;

public class EventContext {
    private final Node node;
    private final int processId;

    public EventContext(Node node, int processId) {
        this.node = node;
        this.processId = processId;
    }

    public Node getNode() {
        return node;
    }

    public int getProcessId() {
        return processId;
    }
}
