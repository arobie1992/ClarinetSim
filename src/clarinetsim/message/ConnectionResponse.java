package clarinetsim.message;

import clarinetsim.context.EventContext;
import peersim.core.Node;

public record ConnectionResponse(Node sender, String connectionId, boolean accepted) implements ClarinetMessage {
    public ConnectionResponse(ConnectionRequest request, boolean accepted) {
        this(request.sender(), request.connectionId(), accepted);
    }

    @Override public void accept(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }
}
