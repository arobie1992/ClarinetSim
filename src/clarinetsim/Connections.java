package clarinetsim;

import peersim.core.Node;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Connections {
    private long sequence = 0;
    private final Lock lock = new ReentrantLock();
    private final int max = 5;
    private final HashMap<String, Node> incoming = new HashMap<>();
    private final HashMap<String, Node> outgoing = new HashMap<>();
    private int cur = 0;

    public boolean atMax() {
        synchronized (lock) {
            return cur == max;
        }
    }

    public Optional<String> addOutgoing(Node self, Node node) {
        return addNode(null, self, node, outgoing);
    }

    public Optional<String> addIncoming(String connectionId, Node node) {
        return addNode(connectionId, null, node, incoming);
    }

    private Optional<String> addNode(String connectionId, Node self, Node node, HashMap<String, Node> group) {
        Objects.requireNonNull(node);
        synchronized(lock) {
            if(atMax()) {
                return Optional.empty();
            }
            cur++;
            if(connectionId == null) {
                connectionId = self.getID() + "-" + node.getID() + "-" + sequence++;
            }
            group.put(connectionId, node);
            return Optional.of(connectionId);
        }
    }

    public boolean removeOutgoing(String connectionId) {
        Objects.requireNonNull(connectionId);
        synchronized(lock) {
            return outgoing.remove(connectionId) != null;
        }
    }

    @Override public String toString() {
        return "Connections{ incoming: " + incoming.keySet() + ", outgoing: " + outgoing.keySet() + " }";
    }
}
