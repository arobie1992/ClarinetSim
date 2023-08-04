package clarinetsim;

import peersim.core.Node;

public class ConnectRequestMessage {

    private final String connectionId;
    private final Node requestor;

    public ConnectRequestMessage(String connectionId, Node requestor) {
        this.connectionId = connectionId;
        this.requestor = requestor;
    }

    public Node getRequestor() {
        return requestor;
    }

    public String getConnectionId() {
        return connectionId;
    }
}
