package clarinetsim.context;

import clarinetsim.GlobalState;
import clarinetsim.connection.CommunicationManager;
import clarinetsim.connection.ConnectionManager;
import clarinetsim.connection.MaliciousCommunicationManager;
import clarinetsim.connection.MaliciousConnectionManager;
import clarinetsim.network.MaliciousNetworkManager;
import clarinetsim.network.NetworkManager;
import clarinetsim.reputation.MaliciousReputationManager;
import clarinetsim.reputation.ReputationManager;
import peersim.config.Configuration;
import peersim.core.Node;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EventContextFactory {

    private final String prefix;
    private final Lock lock = new ReentrantLock();
    private volatile boolean initialized = false;
    private ConnectionManager connectionManager;
    private CommunicationManager communicationManager;
    private ReputationManager reputationManager;
    private NetworkManager networkManager;

    public EventContextFactory(String prefix) {
        this.prefix = prefix;
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
            int maxConnections = Configuration.getInt(prefix + ".max_connections", 1);
            if(GlobalState.isMalicious(node)) {
                this.connectionManager = new MaliciousConnectionManager(maxConnections);
                this.communicationManager = new MaliciousCommunicationManager(prefix);
                this.reputationManager = new MaliciousReputationManager(prefix);
                this.networkManager = new MaliciousNetworkManager();
            } else {
                this.connectionManager = new ConnectionManager(maxConnections);
                this.communicationManager = new CommunicationManager();
                this.reputationManager = new ReputationManager(prefix);
                this.networkManager = new NetworkManager();
            }
            initialized = true;
        }
    }

    public EventContext create(Node node, int protocolId) {
        init(node);
        return new EventContext(
                Objects.requireNonNull(node),
                protocolId,
                validateInited(connectionManager),
                validateInited(communicationManager),
                validateInited(reputationManager),
                validateInited(networkManager)
        );
    }

    public ConnectionManager connectionManager() {
        return validateInited(connectionManager);
    }

    public CommunicationManager communicationManager() {
        return validateInited(communicationManager);
    }

    public ReputationManager reputationManager() {
        return validateInited(reputationManager);
    }

    public NetworkManager networkManager() {
        return validateInited(networkManager);
    }

    private <T> T validateInited(T val) {
        return Objects.requireNonNull(val, "EventContextFactory was not initialized");
    }

}
