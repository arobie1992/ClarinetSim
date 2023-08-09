package clarinetsim.message;

import clarinetsim.reputation.ReputationManager;
import clarinetsim.connection.CommunicationManager;
import clarinetsim.connection.ConnectionManager;
import peersim.core.Node;

public record EventContext(
        Node self,
        int protocolId,
        ConnectionManager connectionManager,
        CommunicationManager communicationManager,
        ReputationManager reputationManager
) {}
