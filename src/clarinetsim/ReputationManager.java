package clarinetsim;

import clarinetsim.message.EventContext;
import clarinetsim.message.QueryForward;
import clarinetsim.message.QueryResponse;

import java.util.Optional;

public class ReputationManager {

    public Optional<QueryResponse> review(QueryResponse queryResponse, EventContext ctx) {
        return ctx.communicationManager()
                .find(queryResponse)
                // if we didn't find a matching log entry, someone sent us one erroneously so don't return anything
                .map(logEntry -> {
                    // this is idempotent, so it doesn't matter if multiple threads perform it simultaneously
                    logEntry.markQueried(queryResponse.responder());
                    return queryResponse;
                });
    }

    public void review(QueryForward queryForward, EventContext ctx) {
        // at the moment nothing further to do than log the queryForward
        ctx.communicationManager().find(queryForward);
    }

}
