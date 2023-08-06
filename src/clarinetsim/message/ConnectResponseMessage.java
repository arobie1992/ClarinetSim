package clarinetsim.message;

import clarinetsim.ClarinetNode;
import clarinetsim.EventContext;
import peersim.core.Node;

public class ConnectResponseMessage implements ClarinetMessage {

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

    @Override public void visit(ClarinetNode node, EventContext ctx) {
        node.handle(this, ctx);
    }
}
