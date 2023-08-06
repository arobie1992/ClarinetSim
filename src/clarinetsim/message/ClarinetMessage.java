package clarinetsim.message;

import clarinetsim.EventContext;

public interface ClarinetMessage {
    void visit(MessageHandler messageHandler, EventContext ctx);
}
