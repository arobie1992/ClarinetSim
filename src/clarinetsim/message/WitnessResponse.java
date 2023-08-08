package clarinetsim.message;

import peersim.core.Node;

public record WitnessResponse(Node witness, String connectionId, boolean accepted) implements ClarinetMessage {
    @Override public void accept(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }

    @Override public String toString() {
        return "WitnessResponse { " +
                "witness: " + witness.getID() +
                ", connectionId: '" + connectionId + '\'' +
                ", accepted: " + accepted +
                " }";
    }
}
