package clarinetsim.message;

import clarinetsim.ClarinetNode;
import clarinetsim.EventContext;
import peersim.core.Node;

public class DataMessage implements ClarinetMessage {

    @Override public void visit(ClarinetNode node, EventContext ctx) {
        node.handle(this, ctx);
    }

    public enum Signature {
        VALID,
        INVALID;
        public boolean isValid() {
            return this == Signature.VALID;
        }
    }

    private final String connectionId;
    private final Node sender;
    private final String data;
    private final Signature signature;

    public DataMessage(String connectionId, Node sender, String data) {
        this.connectionId = connectionId;
        this.sender = sender;
        this.data = data;
        this.signature = Signature.VALID;
    }

    public String getData() {
        return data;
    }

    public Signature getSignature() {
        return signature;
    }
}
