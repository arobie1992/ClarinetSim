package clarinetsim.message;

import peersim.core.Node;

public record QueryForward(QueryResponse queryResponse, Node forwarder) implements ClarinetMessage {
    @Override public void accept(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }

    @Override public String toString() {
        return "QueryForward { queryResponse: " + queryResponse + ", forwarder: " + forwarder.getID() + " }";
    }
}
