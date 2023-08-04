package clarinetsim;

import peersim.core.Node;

public class ConnectRequestMessage {
    private final Node requestor;

    public ConnectRequestMessage(Node requestor) {
        this.requestor = requestor;
    }

    public Node getRequestor() {
        return requestor;
    }
}
