package clarinetsim.message;

import clarinetsim.ClarinetNode;
import clarinetsim.EventContext;

public interface ClarinetMessage {
    void visit(ClarinetNode node, EventContext ctx);
}
