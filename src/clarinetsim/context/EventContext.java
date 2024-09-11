package clarinetsim.context;

import clarinetsim.network.NetworkManager;
import clarinetsim.connection.CommunicationManager;
import clarinetsim.connection.ConnectionManager;
import clarinetsim.reputation.ReputationManager;
import peersim.core.Node;

public record EventContext(
        Node self,
        int protocolId,
        ConnectionManager connectionManager,
        CommunicationManager communicationManager,
        ReputationManager reputationManager,
        NetworkManager networkManager
) {}
