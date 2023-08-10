package clarinetsim.context;

import clarinetsim.GlobalState;
import clarinetsim.connection.CommunicationManager;
import clarinetsim.connection.ConnectionManager;
import clarinetsim.connection.MaliciousCommunicationManager;
import clarinetsim.connection.MaliciousConnectionManager;
import clarinetsim.reputation.MaliciousReputationManager;
import clarinetsim.reputation.ReputationManager;
import peersim.core.Node;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EventContextFactory {

    private final int maxConnections;
    private final int initialReputation;
    private final int minTrustedReputation;
    private final int weakPenaltyValue;
    private final int strongPenaltyValue;
    private final Lock lock = new ReentrantLock();
    private volatile boolean initialized = false;
    private ConnectionManager connectionManager;
    private CommunicationManager communicationManager;
    private ReputationManager reputationManager;

    public EventContextFactory(
            int maxConnections,
            int initialReputation,
            int minTrustedReputation,
            int weakPenaltyValue,
            int strongPenaltyValue
    ) {
        this.maxConnections = maxConnections;
        this.initialReputation = initialReputation;
        this.minTrustedReputation = minTrustedReputation;
        this.weakPenaltyValue = weakPenaltyValue;
        this.strongPenaltyValue = strongPenaltyValue;
    }

    public void init(Node node) {
        // check once before obtaining the lock to avoid unnecessary locking
        if(initialized) {
            return;
        }
        synchronized(lock) {
            // check again to see if another thread got to the lock first
            if(initialized) {
                return;
            }
            if(GlobalState.isMalicious(node)) {
                this.connectionManager = new MaliciousConnectionManager(maxConnections);
                this.communicationManager = new MaliciousCommunicationManager();
                this.reputationManager = new MaliciousReputationManager(
                        initialReputation,
                        minTrustedReputation,
                        weakPenaltyValue,
                        strongPenaltyValue
                );
            } else {
                this.connectionManager = new ConnectionManager(maxConnections);
                this.communicationManager = new CommunicationManager();
                this.reputationManager = new ReputationManager(
                        initialReputation,
                        minTrustedReputation,
                        weakPenaltyValue,
                        strongPenaltyValue
                );
            }
            initialized = true;
        }
    }

    public EventContext create(Node node, int protocolId) {
        init(node);
        return new EventContext(
                Objects.requireNonNull(node),
                protocolId,
                connectionManager,
                communicationManager,
                reputationManager
        );
    }

    public ConnectionManager connectionManager() {
        return Objects.requireNonNull(connectionManager, "EventContextFactory was not initialized");
    }

    public CommunicationManager communicationManager() {
        return Objects.requireNonNull(communicationManager, "EventContextFactory was not initialized");
    }

    public ReputationManager reputationManager() {
        return Objects.requireNonNull(reputationManager, "EventContextFactory was not initialized");
    }

}
