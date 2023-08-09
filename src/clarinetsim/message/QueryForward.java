package clarinetsim.message;

import clarinetsim.reputation.Signature;
import peersim.core.Node;

public record QueryForward(QueryResponse queryResponse, Node forwarder, Signature signature) implements ClarinetMessage {
    @Override public void accept(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }

    @Override public String toString() {
        return "QueryForward { queryResponse: " + queryResponse + ", forwarder: " + forwarder.getID() + " }";
    }
}
