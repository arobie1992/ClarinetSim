package clarinetsim.message;

import clarinetsim.CommunicationUtils;
import clarinetsim.EventContext;

import java.util.Optional;
import java.util.function.Consumer;

public class MessageHandler {

    public void handle(ConnectRequestMessage msg, EventContext ctx) {
        Optional<String> connectionIdOpt = ctx.getConnections().addIncoming(msg.getConnectionId(), ctx.getNode(), msg.getRequestor());
        ConnectResponseMessage resp = new ConnectResponseMessage(msg.getConnectionId(), connectionIdOpt.isPresent());
        CommunicationUtils.sendMessage(ctx.getNode(), msg.getRequestor(), resp, ctx.getProcessId());
    }

    public void handle(ConnectResponseMessage msg, EventContext ctx) {
        Consumer<String> action = msg.isAccepted() ? ctx.getConnections()::confirmTarget : ctx.getConnections()::removeOutgoing;
        action.accept(msg.getConnectionId());
    }

    public void handle(DataMessage msg, EventContext ctx) {
        System.out.println(msg.getData());
    }

}
