package clarinetsim;

import clarinetsim.context.EventContext;
import clarinetsim.message.ClarinetMessage;
import peersim.config.FastConfig;
import peersim.core.Node;
import peersim.transport.Transport;

public class NeighborUtils {

    private NeighborUtils() {}

    public static void send(Node destination, ClarinetMessage message, EventContext ctx) {
        send(ctx.self(), destination, message, ctx.protocolId());
    }

    public static void send(Node self, Node destination, ClarinetMessage message, int protocolId) {
        if(!destination.isUp()) {
            return;
        }
        int transportId = FastConfig.getTransport(protocolId);
        Transport transport = (Transport) self.getProtocol(transportId);
        transport.send(self, destination, message, protocolId);
    }

}
