package clarinetsim.message;

import clarinetsim.EventContext;
import peersim.core.Node;

public class WitnessRequestMessage implements ClarinetMessage {

    private final String connectionId;
    private final Node sender;
    private final Node target;

    public WitnessRequestMessage(String connectionId, Node sender, Node target) {
        this.connectionId = connectionId;
        this.sender = sender;
        this.target = target;
    }

    @Override public void visit(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }

    public String getConnectionId() {
        return connectionId;
    }

    public Node getSender() {
        return sender;
    }

    public Node getTarget() {
        return target;
    }
}
