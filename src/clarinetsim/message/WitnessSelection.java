package clarinetsim.message;

import peersim.core.Node;

public record WitnessSelection(Node witness, String connectionId) implements ClarinetMessage {
    public WitnessSelection(WitnessResponse response) {
        this(response.witness(), response.connectionId());
    }

    @Override public void accept(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }
}
