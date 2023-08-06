package clarinetsim.message;

import clarinetsim.EventContext;
import peersim.core.Node;

public class WitnessResponseMessage implements ClarinetMessage {

    private final String connectionId;
    private final Node witness;
    private final boolean accepted;

    public WitnessResponseMessage(String connectionId, Node witness, boolean accepted) {
        this.connectionId = connectionId;
        this.witness = witness;
        this.accepted = accepted;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public Node getWitness() {
        return witness;
    }

    public boolean isAccepted() {
        return accepted;
    }

    @Override public void visit(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }
}
