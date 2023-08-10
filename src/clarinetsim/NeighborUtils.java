package clarinetsim;

import clarinetsim.message.ClarinetMessage;
import clarinetsim.context.EventContext;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.transport.Transport;

import java.util.*;

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

    public static Optional<Node> selectRandomNeighbor(Node node, int protocolId) {
        int linkableId = FastConfig.getLinkable(protocolId);
        Linkable linkable = (Linkable) node.getProtocol(linkableId);
        if (linkable.degree() == 0) {
            return Optional.empty();
        }

        int neighborId = CommonState.r.nextInt(linkable.degree());
        return Optional.ofNullable(linkable.getNeighbor(neighborId));
    }

    public static List<Node> getAllNeighbors(Node node, int protocolId) {
        int linkableId = FastConfig.getLinkable(protocolId);
        Linkable linkable = (Linkable) node.getProtocol(linkableId);
        List<Node> neighbors = new ArrayList<>();
        for(int i = 0; i < linkable.degree(); i++) {
            neighbors.add(linkable.getNeighbor(i));
        }
        return Collections.unmodifiableList(neighbors);
    }

}
