package clarinetsim.message;

public record ConnectionTerminate(String connectionId) implements ClarinetMessage {
    @Override public void accept(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }
}
