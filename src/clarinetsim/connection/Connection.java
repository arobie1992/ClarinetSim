package clarinetsim.connection;

import peersim.core.Node;

import java.util.*;

public class Connection {

    private final String connectionId;
    private final Node sender;
    private final Node target;
    private boolean targetConfirmed = false;
    private Node witness;
    private final List<Node> witnessCandidates = new ArrayList<>();

    public Connection(String connectionId, Node sender, Node target) {
        this.connectionId = Objects.requireNonNull(connectionId);
        this.sender = Objects.requireNonNull(sender);
        this.target = Objects.requireNonNull(target);
    }

    public String getConnectionId() {
        return connectionId;
    }

    public Node getSender() {
        return sender;
    }

    public Node getTarget() {
        return target;
    }

    public void confirmTarget() {
        targetConfirmed = true;
    }

    public boolean isConfirmed() {
        return targetConfirmed && witness != null;
    }

    public void addWitness(Node witness) {
        this.witness = Objects.requireNonNull(witness);
    }

    public void addWitnessCandidates(List<Node> candidates) {
        this.witnessCandidates.addAll(candidates);
    }

    public List<Node> getWitnessCandidates() {
        return Collections.unmodifiableList(witnessCandidates);
    }

    public void removeWitnessCandidate(long nodeId) {
        Iterator<Node> itr = witnessCandidates.iterator();
        while(itr.hasNext()) {
            Node n = itr.next();
            if(n.getID() == nodeId) {
                itr.remove();
            }
        }
    }

    public Optional<Node> getWitness() {
        return Optional.ofNullable(witness);
    }

    @Override public String toString() {
        Long witnessId = witness == null ? null : witness.getID();
        return "Connection { " +
                "connectionId: '" + connectionId + '\'' +
                ", sender: " + sender.getID() +
                ", target: " + target.getID() +
                ", witness: " + witnessId +
                " }";
    }
}
