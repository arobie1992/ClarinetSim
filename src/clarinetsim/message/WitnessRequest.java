package clarinetsim.message;

import clarinetsim.connection.Connection;
import clarinetsim.context.EventContext;
import peersim.core.Node;

public record WitnessRequest(Node sender, Node receiver, String connectionId) implements ClarinetMessage {
    public WitnessRequest(Connection connection) {
        this(connection.sender(), connection.receiver(), connection.connectionId());
    }
    @Override public void accept(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }
}
