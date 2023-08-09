package clarinetsim.message;

import clarinetsim.log.LogEntry;
import peersim.core.Node;

public record Query(String connectionId, int seqNo, Node querier) implements ClarinetMessage {
    public Query(LogEntry logEntry, Node querier) {
        this(logEntry.message().connectionId(), logEntry.message().seqNo(), querier);
    }

    @Override public void accept(MessageHandler messageHandler, EventContext ctx) {
        messageHandler.handle(this, ctx);
    }

    @Override public String toString() {
        return "Query { connectionId: '" + connectionId + '\'' + ", seqNo: " + seqNo + ", querier: " + querier.getID() + " }";
    }
}
