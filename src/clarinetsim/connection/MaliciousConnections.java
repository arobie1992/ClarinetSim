package clarinetsim.connection;

import peersim.core.Node;

public class MaliciousConnections extends Connections {
    public MaliciousConnections(int max) {
        super(max);
    }

    @Override
    Connection createConnection(String connectionId, Node sender, Node witness, Node receiver, Type type, State state) {
        return new MaliciousConnection(connectionId, sender, witness, receiver, type, state);
    }

    @Override public String toString() {
        return "Malicious" + super.toString();
    }
}
