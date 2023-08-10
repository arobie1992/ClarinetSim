package clarinetsim.log;

import clarinetsim.connection.Connection;
import clarinetsim.message.Data;
import peersim.core.CommonState;
import peersim.core.Node;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class LogEntry {
    private final Data message;
    private final NodeQueryInfo sender;
    private final NodeQueryInfo witness;
    private final NodeQueryInfo receiver;

    LogEntry(Connection connection, Data message) {
        this.message = Objects.requireNonNull(message);
        this.sender = new NodeQueryInfo(connection.sender());
        // ISE because if we've reached this point and witness is null, then something is wrong elsewhere
        this.witness = new NodeQueryInfo(connection.witness().orElseThrow(IllegalStateException::new));
        this.receiver = new NodeQueryInfo(connection.receiver());
    }

    public Data message() {
        return message;
    }

    public Node sender() {
        return sender.node;
    }

    public Node receiver() {
        return receiver.node;
    }

    boolean queryCandidate() {
        return !(sender.queried && witness.queried && receiver.queried);
    }

    public Optional<Node> selectQueryTarget(Node self) {
        var candidates = Stream.of(sender, witness, receiver)
                .filter(n -> !n.queried)
                .map(n -> n.node)
                .filter(n -> n.getID() != self.getID())
                .toList();
        if(candidates.isEmpty()) {
            return Optional.empty();
        }
        var index = CommonState.r.nextInt(candidates.size());
        return Optional.of(candidates.get(index));
    }

    public void markQueried(Node node) {
        if(sender.node.getID() == node.getID()) {
            sender.queried = true;
            return;
        }
        if(witness.node.getID() == node.getID()) {
            witness.queried = true;
            return;
        }
        if(receiver.node.getID() == node.getID()) {
            receiver.queried = true;
        }
    }

    /**
     * Get a list of all the participants in the communication.</br>
     * </br>
     * Sender will be in position 0, witness in position 1, and receiver in position 2.
     * @return the {@code List} of the participants.
     */
    public List<Node> participants() {
        return List.of(sender.node, witness.node, receiver.node);
    }

    @Override public String toString() {
        return "LogEntry { " +
                "message: " + message +
                ", sender: " + sender +
                ", witness: " + witness +
                ", receiver: " + receiver +
                " }";
    }
}
