package clarinetsim;

import peersim.core.Node;

public class DataMessage {

    private final String connectionId;
    private final Node sender;
    private final String data;
    private final String signature;

    public DataMessage(String connectionId, Node sender, String data) {
        this.connectionId = connectionId;
        this.sender = sender;
        this.data = data;
        this.signature = this.connectionId + "-" + this.sender.getID() + "-" + data;
    }

    public String getData() {
        return data;
    }

    public String getSignature() {
        return signature;
    }
}
