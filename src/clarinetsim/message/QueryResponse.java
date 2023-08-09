package clarinetsim.message;

import peersim.core.Node;

public record QueryResponse(Data message, Node responder) implements ClarinetMessage {
    @Override public void accept(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }

    @Override public String toString() {
        return "QueryResponse { message: " + message + ", responder: " + responder.getID() + " }";
    }
}
