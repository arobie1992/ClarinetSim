package clarinetsim.message;

import clarinetsim.context.EventContext;
import clarinetsim.reputation.Signature;

public class DataForward implements ClarinetMessage {
    private final Data data;
    private final Signature receiverSignature;

    public DataForward(Data data, Signature receiverSignature) {
        this.data = data;
        this.receiverSignature = receiverSignature;
    }

    public Data getData() {
        return data;
    }

    public Signature getReceiverSignature() {
        return receiverSignature;
    }

    @Override
    public void accept(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }
}
