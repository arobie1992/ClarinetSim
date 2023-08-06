package clarinetsim.message;

import clarinetsim.EventContext;

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

    @Override public void visit(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }
}
