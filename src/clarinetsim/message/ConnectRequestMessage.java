package clarinetsim.message;

import clarinetsim.ClarinetNode;
import clarinetsim.EventContext;
import peersim.core.Node;

public class ConnectRequestMessage implements ClarinetMessage {

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

    @Override public void visit(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }
}
