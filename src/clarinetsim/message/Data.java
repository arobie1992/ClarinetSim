package clarinetsim.message;

public record Data(String connectionId, int seqNo, String data) implements ClarinetMessage {
    @Override public void accept(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }
}
