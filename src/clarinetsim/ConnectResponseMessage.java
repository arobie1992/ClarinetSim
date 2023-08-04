package clarinetsim;

import peersim.core.Node;

public class ConnectResponseMessage {

    private final Node neighbor;
    private final boolean accepted;

    public ConnectResponseMessage(Node neighbor, boolean accepted) {
        this.neighbor = neighbor;
        this.accepted = accepted;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public Node getNeighbor() {
        return neighbor;
    }
}
