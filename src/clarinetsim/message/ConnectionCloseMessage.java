package clarinetsim.message;

import clarinetsim.EventContext;

public class ConnectionCloseMessage implements ClarinetMessage {

    private final String connectionId;

    public ConnectionCloseMessage(String connectionId) {
        this.connectionId = connectionId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    @Override public void visit(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }
}
