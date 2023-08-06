package clarinetsim.message;

import clarinetsim.EventContext;
import peersim.core.Node;

public class WitnessNotificationMessage implements ClarinetMessage {

    private final String connectionId;
    private final Node witness;

    public WitnessNotificationMessage(String connectionId, Node witness) {
        this.connectionId = connectionId;
        this.witness = witness;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public Node getWitness() {
        return witness;
    }

    @Override public void visit(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }
}
