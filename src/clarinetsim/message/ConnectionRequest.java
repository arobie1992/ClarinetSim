package clarinetsim.message;

import peersim.core.Node;

public record ConnectionRequest(Node sender, String connectionId) implements ClarinetMessage {
    @Override public void accept(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }
}
