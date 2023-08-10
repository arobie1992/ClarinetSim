package clarinetsim;

import clarinetsim.connection.CommunicationManager;
import clarinetsim.connection.ConnectionManager;
import clarinetsim.message.ClarinetMessage;
import clarinetsim.context.EventContext;
import clarinetsim.message.MessageHandler;
import clarinetsim.reputation.ReputationManager;
import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.vector.SingleValueHolder;

import java.util.concurrent.atomic.AtomicInteger;

public class ClarinetNode extends SingleValueHolder implements CDProtocol, EDProtocol {

    private final String prefix;
    private final MessageHandler messageHandler = new MessageHandler();
    private final ConnectionManager connectionManager;
    private final CommunicationManager communicationManager = new CommunicationManager();
    private final ReputationManager reputationManager;
    private final int printInterval;
    private final AtomicInteger printCounter = new AtomicInteger();
    private final boolean printConnections;
    private final boolean printLog;
    private final boolean printReputations;

    public ClarinetNode(String prefix) {
        super(prefix);
        this.prefix = prefix;
        this.connectionManager = new ConnectionManager(Configuration.getInt(prefix + ".max_connections", 1));
        this.reputationManager = new ReputationManager(
                Configuration.getInt(prefix + ".initial_reputation", 100),
                Configuration.getInt(prefix + ".min_trusted_reputation", 0),
                Configuration.getInt(prefix + ".weak_penalty_value", 1),
                Configuration.getInt(prefix + ".strong_penalty_value", 3)
        );
        this.printInterval = Configuration.getInt(prefix + ".print_interval", 1);
        this.printConnections = Configuration.getBoolean(prefix + ".print_connections", false);
        this.printLog = Configuration.getBoolean(prefix + ".print_log", false);
        this.printReputations = Configuration.getBoolean(prefix + ".print_reputations", false);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override public Object clone() {
        return new ClarinetNode(prefix);
    }

    @Override public void nextCycle(Node node, int protocolId) {
        // Node 0 is malicious


//        switch(CommonState.r.nextInt(3)) {
//            case 0 -> NeighborUtils.selectRandomNeighbor(node, protocolId)
//                        .ifPresent(receiver -> connectionManager.requestConnection(node, receiver, protocolId));
//            case 1 -> connectionManager.selectRandom(Type.OUTGOING)
//                        .map(connection -> communicationManager.send(node, connection, "Test message", protocolId))
//                        .ifPresent(connectionManager::release);
//            case 2 -> communicationManager.selectRandom()
//                        .ifPresent(message -> communicationManager.query(node, message, protocolId));
//        }
        if(printCounter.incrementAndGet() % printInterval == 0) {
            if(printConnections) {
                connectionManager.printConnections(node);
            }
            if(printLog) {
                communicationManager.printLog(node);
            }
            if(printReputations) {
                reputationManager.printReputations(node);
            }
        }
    }

    @Override public void processEvent(Node node, int protocolId, Object event) {
        ClarinetMessage message = (ClarinetMessage) event;
        var ctx = new EventContext(node, protocolId, connectionManager, communicationManager, reputationManager);
        message.accept(messageHandler, ctx);
    }
}
