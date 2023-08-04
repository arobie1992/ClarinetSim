package clarinetsim;

import peersim.core.Node;

public class ConnectResponseMessage {

    private final String connectionId;
    private final boolean accepted;

    public ConnectResponseMessage(String connectionId, boolean accepted) {
        this.connectionId = connectionId;
        this.accepted = accepted;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getConnectionId() {
        return connectionId;
    }
}
