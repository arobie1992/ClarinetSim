package clarinetsim.message;

import clarinetsim.context.EventContext;

public class MessageHandler {

    public void handle(ConnectionRequest msg, EventContext ctx) {
        ctx.connectionManager().tryAccept(msg, ctx);
    }

    public void handle(ConnectionResponse msg, EventContext ctx) {
        if(msg.accepted()) {
            ctx.connectionManager()
                    .initializeWitnessCandidates(msg, ctx)
                    .ifPresent(connection -> ctx.connectionManager().requestWitness(connection, ctx));
        } else {
            ctx.connectionManager().terminate(msg.connectionId(), ctx);
        }
    }

    public void handle(WitnessRequest msg, EventContext ctx) {
        ctx.connectionManager().tryWitness(msg, ctx);
    }

    public void handle(WitnessResponse msg, EventContext ctx) {
        if(msg.accepted()) {
            ctx.connectionManager().confirmWitness(msg, ctx);
        } else {
            ctx.connectionManager().requestWitness(msg.connectionId(), ctx);
        }
    }

    public void handle(WitnessSelection msg, EventContext ctx) {
        ctx.connectionManager().acceptWitness(msg, ctx);
    }

    public void handle(ConnectionTerminate msg, EventContext ctx) {
        ctx.connectionManager().teardown(msg.connectionId());
    }

    public void handle(Data msg, EventContext ctx) {
        ctx.connectionManager().get(msg.connectionId())
                .map(connection -> switch(connection.type()) {
                    // if it's outgoing, do don't anything, and just let it release subsequently
                    case OUTGOING -> connection;
                    case WITNESSING -> ctx.communicationManager().forward(connection, msg, ctx);
                    case INCOMING -> ctx.communicationManager().receive(connection, msg, ctx);
                })
                .ifPresent(ctx.connectionManager()::release);
    }

    public void handle(DataForward msg, EventContext ctx) {
        ctx.reputationManager().senderReview(msg, ctx);
    }

    public void handle(Query msg, EventContext ctx) {
        ctx.communicationManager().find(msg).ifPresent(logEntry -> ctx.communicationManager().reply(msg, logEntry, ctx));
    }

    public void handle(QueryResponse msg, EventContext ctx) {
        ctx.reputationManager().queryReview(msg, ctx).ifPresent(r -> ctx.communicationManager().forward(r, ctx));
    }

    public void handle(QueryForward msg, EventContext ctx) {
        ctx.reputationManager().queryForwardReview(msg, ctx);
    }
}
