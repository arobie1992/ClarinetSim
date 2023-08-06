package clarinetsim.message;

import clarinetsim.CommunicationUtils;
import clarinetsim.EventContext;
import clarinetsim.Penalty;

import java.util.Optional;
import java.util.function.Consumer;

public class MessageHandler {

    public void handle(ConnectRequestMessage msg, EventContext ctx) {
        Optional<String> connectionIdOpt = ctx.getConnections().addIncoming(msg.getConnectionId(), ctx.getNode(), msg.getRequestor());
        ConnectResponseMessage resp = new ConnectResponseMessage(msg.getConnectionId(), connectionIdOpt.isPresent());
        CommunicationUtils.sendMessage(ctx.getNode(), msg.getRequestor(), resp, ctx.getProtocolId());
    }

    public void handle(ConnectResponseMessage msg, EventContext ctx) {
        Consumer<String> action = msg.isAccepted() ? ctx.getConnections()::confirmOutgoingTarget : ctx.getConnections()::removeOutgoing;
        action.accept(msg.getConnectionId());
    }

    public void handle(DataMessage msg, EventContext ctx) {
        if(msg.getTarget().getID() != ctx.getNode().getID()) {
            System.out.println("Node " + ctx.getNode().getID() + " forwarding message: " + msg.getData());
            ctx.getConnections().forward(ctx.getNode(), msg, ctx.getProtocolId());
            if(msg.getSignature() != DataMessage.Signature.VALID) {
                ctx.getClarinetNode().penalize(msg.getSender(), Penalty.STRONG);
            }
        } else {
            if(msg.getSignature() != DataMessage.Signature.VALID) {
                ctx.getClarinetNode().penalize(msg.getSender(), Penalty.STRONG);
            }
            DataMessage content = (DataMessage) msg.getData();
            if(content.getSignature() != DataMessage.Signature.VALID) {
                ctx.getClarinetNode().penalize(msg.getSender(), Penalty.WEAK);
                ctx.getClarinetNode().penalize(content.getSender(), Penalty.WEAK);
            }
            System.out.println("Node " + ctx.getNode().getID() + " received data: " + content.getData());
        }
    }

    public void handle(WitnessRequestMessage msg, EventContext ctx) {
        Optional<String> connectionIdOpt = ctx.getConnections().addWitnessing(msg, ctx.getNode());
        WitnessResponseMessage resp = new WitnessResponseMessage(msg.getConnectionId(), ctx.getNode(), connectionIdOpt.isPresent());
        CommunicationUtils.sendMessage(ctx.getNode(), msg.getSender(), resp, ctx.getProtocolId());
    }

    public void handle(WitnessResponseMessage msg, EventContext ctx) {
        if(msg.isAccepted()) {
            ctx.getConnections().confirmWitness(msg.getConnectionId(), msg.getWitness());
            ctx.getConnections().notifyTargetOfWitness(ctx.getNode(), ctx.getProtocolId(), msg);
        } else {
            boolean remaining = ctx.getConnections().tryWitness(msg.getConnectionId(), ctx.getNode(), ctx.getProtocolId());
            if(!remaining) {
                ctx.getConnections().close(msg.getConnectionId(), ctx.getNode(), ctx.getProtocolId());
            }
        }
    }

    public void handle(WitnessNotificationMessage msg, EventContext ctx) {
        ctx.getConnections().addWitness(msg.getConnectionId(), msg.getWitness());
    }

    public void handle(ConnectionCloseMessage msg, EventContext ctx) {
        ctx.getConnections().close(msg.getConnectionId(), ctx.getNode(), ctx.getProtocolId());
    }
}
