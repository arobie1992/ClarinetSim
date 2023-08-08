package clarinetsim.connection;

import peersim.core.CommonState;
import peersim.core.Node;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Connection {
    private final Lock lock = new ReentrantLock();
    private final String connectionId;
    private final Node sender;
    private Node witness;
    private final List<Node> witnessCandidates = new ArrayList<>();
    private final Node receiver;
    private State state;

    Connection(String connectionId, Node sender, Node witness, Node receiver, State state) {
        this.connectionId = connectionId;
        this.sender = sender;
        this.witness = witness;
        this.receiver = receiver;
        this.state = state;
    }

    private void verifyAlive() {
        if(state == State.TERMINATED) {
            throw new UnsupportedOperationException("Cannot operate on terminated connections");
        }
    }

    public Optional<Node> witness() {
        return Optional.ofNullable(witness);
    }

    public String connectionId() {
        verifyAlive();
        return connectionId;
    }

    public Node sender() {
        verifyAlive();
        return sender;
    }

    void addWitnessCandidates(Collection<Node> candidates) {
        verifyAlive();
        if(state != State.REQUESTING_WITNESS) {
            throw new UnsupportedOperationException("Can only add candidates in " + State.REQUESTING_RECEIVER);
        }
        this.witnessCandidates.addAll(candidates);
    }

    Optional<Node> selectCandidate() {
        verifyAlive();
        if(state != State.REQUESTING_WITNESS) {
            throw new UnsupportedOperationException("Can only select witness candidates in " + State.REQUESTING_WITNESS);
        }
        if(witnessCandidates.isEmpty()) {
            return Optional.empty();
        } else {
            Node candidate = witnessCandidates.remove(CommonState.r.nextInt(witnessCandidates.size()));
            return Optional.of(candidate);
        }
    }

    void confirmWitness(Node witness) {
        verifyAlive();
        if(state != State.REQUESTING_WITNESS) {
            throw new UnsupportedOperationException("Can only confirm witness in " + State.REQUESTING_WITNESS);
        }
        state = State.OPEN;
        this.witness = Objects.requireNonNull(witness);
    }

    public Node receiver() {
        verifyAlive();
        return receiver;
    }

    void receiverConfirmed() {
        verifyAlive();
        if(state != State.REQUESTING_RECEIVER) {
            throw new UnsupportedOperationException("Can only call receiverConfirmed in " + State.REQUESTING_RECEIVER);
        }
        state = State.REQUESTING_WITNESS;
    }

    void lock() {
        verifyAlive();
        lock.lock();
    }

    void unlock() {
        verifyAlive();
        lock.unlock();
    }

    void terminate() {
        state = State.TERMINATED;
    }

    @Override public String toString() {
        return "Connection { " +
                "connectionId: '" + connectionId + '\'' +
                ", sender: " + sender.getID() +
                ", witness: " + (witness == null ? null : witness.getID()) +
                ", receiver: " + receiver.getID() +
                ", state: " + state +
                " }";
    }
}
