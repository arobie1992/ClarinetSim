package clarinetsim.message;

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
        ctx.connectionManager().acceptWitness(msg);
    }

    public void handle(ConnectionTerminate msg, EventContext ctx) {
        ctx.connectionManager().teardown(msg.connectionId());
    }
}
