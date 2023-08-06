package clarinetsim;

import peersim.config.FastConfig;
import peersim.core.Node;
import peersim.transport.Transport;

public class CommunicationUtils {
    private CommunicationUtils() {}

    public static boolean sendMessage(Node node, Node neighbor, Object message, int protocolID) {
        // XXX quick and dirty handling of failures
        // (message would be lost anyway, we save time)
        if(!neighbor.isUp()) {
            return false;
        }

        int transportId = FastConfig.getTransport(protocolID);
        Transport transport = (Transport) node.getProtocol(transportId);
        transport.send(node, neighbor, message, protocolID);
        return true;
    }
}
