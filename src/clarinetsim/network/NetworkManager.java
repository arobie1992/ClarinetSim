package clarinetsim.network;

import clarinetsim.context.EventContext;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Network;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NetworkManager {

    // right now, we're assuming network is static so it's fine to cache the entire list of nodes
    // I really ought to look into if PeerSim supports something like this more directly, but meh
    private List<Node> networkNodes;
    private final Lock lock = new ReentrantLock();
    private volatile boolean initialized = false;

    private void init(Node self) {
        if(initialized) {
            return;
        }
        synchronized(lock) {
            if(initialized) {
                return;
            }
            var working = new ArrayList<Node>(Network.size()-1);
            // if this changes while we're iterating, then things get weird, but as above, network is static for now, so
            // we'll deal with that if it comes up
            for(int i = 0; i < Network.size(); i++) {
                if(i == self.getID()) {
                    continue;
                }
                working.add(Network.get(i));
            }
            networkNodes = Collections.unmodifiableList(working);
            initialized = true;
        }
    }

    public Optional<Node> selectRandomNetworkNode(Node node) {
        init(node);
        int neighborIndex = CommonState.r.nextInt(networkNodes.size());
        return Optional.of(networkNodes.get(neighborIndex));
    }

    public List<Node> peers(Node node, int protocolId) {
        int linkableId = FastConfig.getLinkable(protocolId);
        Linkable linkable = (Linkable) node.getProtocol(linkableId);
        List<Node> neighbors = new ArrayList<>();
        for(int i = 0; i < linkable.degree(); i++) {
            neighbors.add(linkable.getNeighbor(i));
        }
        return Collections.unmodifiableList(neighbors);
    }

    public void addPeer(Node self, int protocolId, Node nodeToAdd) {
        int linkableId = FastConfig.getLinkable(protocolId);
        Linkable linkable = (Linkable) self.getProtocol(linkableId);
        /* The two implementations of this method existing in the project already do the contains check.
           See: example.newscast.SimpleNewscast.addNeighbor and peersim.core.IdleProtocol.addNeighbor.
           The third, peersim.core.OracleIdleProtocol.addNeighbor, does not support this operation so it's moot.
        */
        linkable.addNeighbor(nodeToAdd);
    }

    public void addPeer(EventContext ctx, Node nodeToAdd) {
        addPeer(ctx.self(), ctx.protocolId(), nodeToAdd);
    }

}
