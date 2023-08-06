package clarinetsim.connection;

import peersim.core.CommonState;
import peersim.core.Node;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class Connections {
    private long sequence = 0;
    private long messageSeq = 0;
    private final Lock lock = new ReentrantLock();
    private final int max = 5;
    private final HashMap<String, Connection> incoming = new HashMap<>();
    private final HashMap<String, Connection> outgoing = new HashMap<>();
    private int cur = 0;

    public boolean atMax() {
        synchronized (lock) {
            return cur == max;
        }
    }

    public Optional<String> addOutgoing(Node self, Node node) {
        return addConnection(null, self, node, outgoing);
    }

    public Optional<String> addIncoming(String connectionId, Node self, Node node) {
        return addConnection(connectionId, node, self, incoming);
    }

    private Optional<String> addConnection(String connectionId, Node sender, Node target, HashMap<String, Connection> group) {
        Objects.requireNonNull(target);
        synchronized(lock) {
            if(atMax()) {
                return Optional.empty();
            }
            cur++;
            if(connectionId == null) {
                connectionId = sender.getID() + "-" + target.getID() + "-" + sequence++;
            }
            group.put(connectionId, new Connection(connectionId, sender, target));
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

    public boolean randomOutgoingSyncOp(Consumer<Map.Entry<String, Connection>> op) {
        synchronized(lock) {
            if(outgoing.isEmpty()) {
                return false;
            }
            int node = CommonState.r.nextInt(outgoing.size());
            Map.Entry<String, Connection> target = null;
            for(Map.Entry<String, Connection> e : outgoing.entrySet()) {
                if(node == 0) {
                    target = e;
                    break;
                }
                node--;
            }
            if(target == null) {
                throw new IllegalStateException();
            }
            op.accept(target);
            return true;
        }
    }
}
