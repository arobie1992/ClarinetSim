package clarinetsim.message;

import clarinetsim.reputation.Signature;

public class Data implements ClarinetMessage {

    private final String connectionId;
    private final int seqNo;
    private final String data;
    private final Signature senderSignature;
    private Signature witnessSignature;

    public Data(String connectionId, int seqNo, String data, Signature senderSignature) {
        this.connectionId = connectionId;
        this.seqNo = seqNo;
        this.data = data;
        this.senderSignature = senderSignature;
    }

    public String connectionId() {
        return connectionId;
    }

    public int seqNo() {
        return seqNo;
    }

    public String data() {
        return data;
    }

    public Signature senderSignature() {
        return senderSignature;
    }

    public Signature witnessSignature() {
        return witnessSignature;
    }

    public void witnessSign() {
        witnessSignature = Signature.VALID;
    }

    @Override public void accept(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }

    @Override public String toString() {
        return "Data { connectionId: '" + connectionId + '\''
                + ", seqNo: " + seqNo
                + ", data: '" + data + '\''
                + ", senderSignature: " + senderSignature
                + ", witnessSignature: " + witnessSignature
                + " }";
    }
}
