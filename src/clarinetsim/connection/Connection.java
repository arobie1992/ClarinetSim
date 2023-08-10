package clarinetsim.connection;

import peersim.core.CommonState;
import peersim.core.Node;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Connection {
    private final Lock lock = new ReentrantLock();
    private final String connectionId;
    private final Node sender;
    private Node witness;
    private final List<Node> witnessCandidates = new ArrayList<>();
    private final Node receiver;
    private final Type type;
    private State state;
    private boolean inUse = false;
    private final AtomicInteger seqNo = new AtomicInteger();

    Connection(String connectionId, Node sender, Node witness, Node receiver, Type type, State state) {
        this.connectionId = connectionId;
        this.sender = sender;
        this.witness = witness;
        this.receiver = receiver;
        this.type = type;
        this.state = state;
    }

    private void verifyAlive() {
        if(state == State.TERMINATED) {
            throw new UnsupportedOperationException("Cannot operate on terminated connections");
        }
    }

    private void verifyPermitted() {
        verifyAlive();
        if(!inUse) {
            throw new UnsupportedOperationException("Cannot perform operation on a connection that is not in use");
        }
    }

    public Optional<Node> witness() {
        verifyPermitted();
        return Optional.ofNullable(witness);
    }

    public String connectionId() {
        verifyAlive();
        return connectionId;
    }

    public Node sender() {
        verifyPermitted();
        return sender;
    }

    void addWitnessCandidates(Collection<Node> candidates) {
        verifyPermitted();
        if(state != State.REQUESTING_WITNESS) {
            throw new UnsupportedOperationException("Can only add candidates in " + State.REQUESTING_RECEIVER);
        }
        handleAddWitnessCandidates(candidates);
    }

    /**
     * Override entrypoint for {@link Connection#addWitnessCandidates(Collection)}.</br>
     * </br>
     * This should never be called by any other method and is only to allow {@link MaliciousConnection} to customize
     * the behavior.
     */
    void handleAddWitnessCandidates(Collection<Node> candidates) {
        this.witnessCandidates.addAll(candidates);
    }

    Optional<Node> selectCandidate() {
        verifyPermitted();
        if(state != State.REQUESTING_WITNESS) {
            throw new UnsupportedOperationException("Can only select witness candidates in " + State.REQUESTING_WITNESS);
        }
        return handleSelectCandidate();
    }

    /**
     * Override entrypoint for {@link Connection#selectCandidate()}.</br>
     * </br>
     * This should never be called by any other method and is only to allow {@link MaliciousConnection} to customize
     * the behavior.
     */
    Optional<Node> handleSelectCandidate() {
        if(witnessCandidates.isEmpty()) {
            return Optional.empty();
        } else {
            Node candidate = witnessCandidates.remove(CommonState.r.nextInt(witnessCandidates.size()));
            return Optional.of(candidate);
        }
    }

    void confirmWitness(Node witness) {
        verifyPermitted();
        if(state != State.REQUESTING_WITNESS) {
            throw new UnsupportedOperationException("Can only confirm witness in " + State.REQUESTING_WITNESS);
        }
        state = State.OPEN;
        this.witness = Objects.requireNonNull(witness);
    }

    public Node receiver() {
        verifyPermitted();
        return receiver;
    }

    public Type type() {
        verifyAlive();
        return type;
    }

    public boolean canWitness() {
        return state == State.OPEN && type == Type.WITNESSING;
    }

    void receiverConfirmed() {
        verifyPermitted();
        if(state != State.REQUESTING_RECEIVER) {
            throw new UnsupportedOperationException("Can only call receiverConfirmed in " + State.REQUESTING_RECEIVER);
        }
        state = State.REQUESTING_WITNESS;
    }

    int nextSeqNo() {
        verifyPermitted();
        return seqNo.getAndIncrement();
    }

    void lock() {
        verifyAlive();
        lock.lock();
        inUse = true;
    }

    void unlock() {
        verifyPermitted();
        inUse = false;
        lock.unlock();
    }

    void terminate() {
        verifyPermitted();
        state = State.TERMINATED;
        // can't just call unlock because unlock verifies that it's alive, and I want
        // to make sure the state gets updated before we release the lock
        inUse = false;
        lock.unlock();
    }

    @Override public String toString() {
        return "Connection { " +
                "connectionId: '" + connectionId + '\'' +
                ", sender: " + sender.getID() +
                ", witness: " + (witness == null ? null : witness.getID()) +
                ", receiver: " + receiver.getID() +
                ", type: " + type +
                ", state: " + state +
                " }";
    }
}
