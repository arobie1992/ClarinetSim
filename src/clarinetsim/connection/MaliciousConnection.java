package clarinetsim.connection;

import clarinetsim.GlobalState;
import peersim.core.CommonState;
import peersim.core.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class MaliciousConnection extends Connection {

    private final List<Node> cooperativeWitnessCandidates = new ArrayList<>();
    private final List<Node> maliciousWitnessCandidates = new ArrayList<>();

    MaliciousConnection(String connectionId, Node sender, Node witness, Node receiver, Type type, State state) {
        super(connectionId, sender, witness, receiver, type, state);
    }

    @Override void handleAddWitnessCandidates(Collection<Node> candidates) {
        for(Node candidate : candidates) {
            if(GlobalState.isMalicious(candidate)) {
                maliciousWitnessCandidates.add(candidate);
            } else {
                cooperativeWitnessCandidates.add(candidate);
            }
        }
    }

    @Override Optional<Node> handleSelectCandidate() {
        Node candidate;
        if(!maliciousWitnessCandidates.isEmpty()) {
            candidate = maliciousWitnessCandidates.remove(CommonState.r.nextInt(maliciousWitnessCandidates.size()));
        } else {
            candidate = cooperativeWitnessCandidates.remove(CommonState.r.nextInt(cooperativeWitnessCandidates.size()));
        }
        return Optional.ofNullable(candidate);
    }

    @Override public String toString() {
        return "Malicious" + super.toString();
    }
}
