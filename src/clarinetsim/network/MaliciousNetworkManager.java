package clarinetsim.network;

import clarinetsim.GlobalState;
import peersim.config.FastConfig;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MaliciousNetworkManager extends NetworkManager {

    private volatile boolean initialized = false;
    private final Lock lock = new ReentrantLock();

    private void init(Node self, int protocolId) {
        if(initialized) {
            return;
        }
        synchronized(lock) {
            if(initialized) {
                return;
            }
            int linkableId = FastConfig.getLinkable(protocolId);
            Linkable linkable = (Linkable) self.getProtocol(linkableId);
            for(int i = 0; i < Network.size(); i++) {
                var node = Network.get(i);
                // all malicious nodes know about each other
                if(GlobalState.isMalicious(node) && node.getID() != self.getID() && !linkable.contains(node)) {
                    linkable.addNeighbor(node);
                }
            }
            initialized = true;
        }
    }

    @Override public List<Node> peers(Node node, int protocolId) {
        init(node, protocolId);
        return super.peers(node, protocolId);
    }
}
