package clarinetsim.connection;

import peersim.core.Node;

import java.util.Objects;

public class Connection {

    private final String connectionId;
    private final Node sender;
    private final Node target;
    private boolean targetConfirmed = false;

    public Connection(String connectionId, Node sender, Node target) {
        this.connectionId = Objects.requireNonNull(connectionId);
        this.sender = Objects.requireNonNull(sender);
        this.target = Objects.requireNonNull(target);
    }

    public String getConnectionId() {
        return connectionId;
    }

    public Node getSender() {
        return sender;
    }

    public Node getTarget() {
        return target;
    }

    public void confirmTarget() {
        targetConfirmed = true;
    }

    public boolean isConfirmed() {
        return targetConfirmed;
    }
}
