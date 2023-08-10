package clarinetsim.connection;

public class MaliciousConnectionManager extends ConnectionManager {
    public MaliciousConnectionManager(int maxConnections) {
        super(new MaliciousConnections(maxConnections));
    }
}
