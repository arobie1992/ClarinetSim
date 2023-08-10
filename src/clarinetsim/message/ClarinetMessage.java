package clarinetsim.message;

import clarinetsim.context.EventContext;

public interface ClarinetMessage {
    void accept(MessageHandler messageHandler, EventContext ctx);
}
