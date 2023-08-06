package clarinetsim.message;

import clarinetsim.EventContext;
import peersim.core.Node;

public class DataMessage implements ClarinetMessage {

    public enum Signature {
        VALID,
        INVALID;
        public boolean isValid() {
            return this == Signature.VALID;
        }
    }

    private final String connectionId;
    private final Node sender;
    private final Node target;
    private final Object data;
    private final Signature signature;

    public DataMessage(String connectionId, Node sender, Node target, Object data) {
        this.connectionId = connectionId;
        this.sender = sender;
        this.target = target;
        this.data = data;
        this.signature = Signature.VALID;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public Node getSender() {
        return sender;
    }

    public Object getData() {
        return data;
    }

    public Signature getSignature() {
        return signature;
    }

    public Node getTarget() {
        return target;
    }

    @Override public void visit(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }
}
