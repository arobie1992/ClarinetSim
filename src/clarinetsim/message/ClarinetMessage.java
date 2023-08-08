package clarinetsim.message;

public interface ClarinetMessage {
    void accept(MessageHandler messageHandler, EventContext ctx);
}
