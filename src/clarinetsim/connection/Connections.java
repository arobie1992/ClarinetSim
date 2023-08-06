package clarinetsim.connection;

import clarinetsim.CommunicationUtils;
import clarinetsim.message.*;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Connections {
    private long sequence = 0;
    private long messageSeq = 0;
    private final Lock lock = new ReentrantLock();
    private final int max = 5;
    private final HashMap<String, Connection> incoming = new HashMap<>();
    private final HashMap<String, Connection> outgoing = new HashMap<>();
    private final HashMap<String, Connection> witnessing = new HashMap<>();

    public boolean atMax() {
        synchronized (lock) {
            return incoming.size() + outgoing.size() + witnessing.size() == max;
        }
    }

    public Optional<String> addOutgoing(Node self, Node node, int protocolId) {
        synchronized(lock) {
            return addConnection(null, self, node, outgoing).map(id -> {
                addWitnessCandidates(id, self, protocolId);
                return id;
            });
        }
    }

    public Optional<String> addIncoming(String connectionId, Node self, Node node) {
        synchronized(lock) {
            return addConnection(connectionId, node, self, incoming).map(id -> {
                Connection connection = incoming.get(connectionId);
                connection.confirmTarget();
                return id;
            });
        }
    }

    public Optional<String> addWitnessing(WitnessRequestMessage witnessRequestMessage, Node self) {
        synchronized(lock) {
            String connectionId = witnessRequestMessage.getConnectionId();
            Node sender = witnessRequestMessage.getSender();
            Node target = witnessRequestMessage.getTarget();
            return addConnection(connectionId, sender, target, witnessing).map(id -> {
                Connection connection = witnessing.get(id);
                connection.addWitness(self);
                return id;
            });
        }
    }

    private Optional<String> addConnection(String connectionId, Node sender, Node target, HashMap<String, Connection> group) {
        Objects.requireNonNull(target);
        if(atMax()) {
            return Optional.empty();
        }
        if(connectionId == null) {
            connectionId = sender.getID() + "-" + target.getID() + "-" + sequence++;
        }
        group.put(connectionId, new Connection(connectionId, sender, target));
        return Optional.of(connectionId);
    }

    public boolean removeOutgoing(String connectionId) {
        Objects.requireNonNull(connectionId);
        synchronized(lock) {
            return outgoing.remove(connectionId) != null;
        }
    }

    @Override public String toString() {
        return "Connections { " +
                "incoming: " + incoming.values() +
                ", outgoing: " + outgoing.values() +
                ", witnessing: " + witnessing.values() +
                " }";
    }

    public boolean sendToRandomTarget(Node self, int protocolId) {
        synchronized(lock) {
            if(outgoing.isEmpty()) {
                return false;
            }
            List<Connection> candidates = outgoing.values().stream().filter(c -> c.isConfirmed()).collect(Collectors.toList());
            Connection conn = candidates.get(CommonState.r.nextInt(outgoing.size()));

            DataMessage msg = new DataMessage(
                    conn.getConnectionId(),
                    self,
                    conn.getTarget(),
                    "Test message " + CommonState.r.nextInt()
            );
            System.out.println("Node " + self.getID() + " sending data: " + msg.getData());
            CommunicationUtils.sendMessage(self, conn.getWitness().get(), msg, protocolId);
            return true;
        }
    }

    public void confirmOutgoingTarget(String connectionId) {
        synchronized(lock) {
            Connection connection = outgoing.get(connectionId);
            connection.confirmTarget();
        }
    }

    public void addWitness(String connectionId, Node witness) {
        Objects.requireNonNull(connectionId);
        synchronized(lock) {
            Connection connection = outgoing.get(connectionId);
            if(connection == null) {
                connection = incoming.get(connectionId);
            }
            if(connection != null) {
                connection.addWitness(witness);
            }
        }
    }

    private void addWitnessCandidates(String connectionId, Node self, int protocolId) {
        synchronized(lock) {
            Connection connection = outgoing.get(Objects.requireNonNull(connectionId));
            int linkableId = FastConfig.getLinkable(protocolId);
            Linkable linkable = (Linkable) self.getProtocol(linkableId);
            List<Node> candidates = new ArrayList<>();
            for(int i = 0; i < linkable.degree(); i++) {
                Node neighbor = linkable.getNeighbor(i);
                if(neighbor.getID() != connection.getTarget().getID()) {
                    candidates.add(neighbor);
                }
            }
            connection.addWitnessCandidates(candidates);
        }
    }

    public boolean tryWitness(String connectionId, Node self, int protocolId) {
        synchronized(lock) {
            Connection connection = outgoing.get(connectionId);
            if(connection == null) {
                return false;
            }
            List<Node> candidates = connection.getWitnessCandidates();
            if(candidates.isEmpty()) {
                return false;
            }
            Node candidate = candidates.get(CommonState.r.nextInt(candidates.size()));
            WitnessRequestMessage msg = new WitnessRequestMessage(connectionId, connection.getSender(), connection.getTarget());
            CommunicationUtils.sendMessage(self, candidate, msg, protocolId);
            return true;
        }
    }

    public void confirmWitness(String connectionId, Node witness) {
        synchronized(lock) {
            Connection connection = outgoing.get(connectionId);
            if(connection == null) {
                connection = incoming.get(connectionId);
            }
            connection.addWitness(witness);
        }
    }

    public void notifyTargetOfWitness(Node self, int protocolId, WitnessResponseMessage msg) {
        synchronized(lock) {
            Connection connection = outgoing.get(msg.getConnectionId());
            WitnessNotificationMessage notification = new WitnessNotificationMessage(msg.getConnectionId(), msg.getWitness());
            CommunicationUtils.sendMessage(self, connection.getTarget(), notification, protocolId);
        }
    }

    public void close(String connectionId, Node self, int protocolId) {
        synchronized(lock) {
            Connection connection = outgoing.remove(connectionId);
            if(connection != null) {
                // notify witness and target
                ConnectionCloseMessage msg = new ConnectionCloseMessage(connectionId);
                CommunicationUtils.sendMessage(self, connection.getTarget(), msg, protocolId);
                connection.getWitness().ifPresent(witness -> CommunicationUtils.sendMessage(self, witness, msg, protocolId));
                return;
            }
            if(witnessing.remove(connectionId) != null) {
                return;
            }
            incoming.remove(connectionId);
        }
    }

    public void forward(Node self, DataMessage msg, int protocolId) {
        synchronized(lock) {
            Connection connection = witnessing.get(msg.getConnectionId());
            if(connection != null) {
                DataMessage forwarding = new DataMessage(msg.getConnectionId(), self, connection.getTarget(), msg);
                CommunicationUtils.sendMessage(self, connection.getTarget(), forwarding, protocolId);
            }
        }
    }
}
