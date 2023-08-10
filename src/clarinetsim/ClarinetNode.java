package clarinetsim;

import clarinetsim.connection.Type;
import clarinetsim.context.EventContextFactory;
import clarinetsim.message.ClarinetMessage;
import clarinetsim.message.MessageHandler;
import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.vector.SingleValueHolder;

import java.util.concurrent.atomic.AtomicInteger;

public class ClarinetNode extends SingleValueHolder implements CDProtocol, EDProtocol {

    private final String prefix;
    private final MessageHandler messageHandler = new MessageHandler();
    private final EventContextFactory eventContextFactory;
    private final int printInterval;
    private final AtomicInteger printCounter = new AtomicInteger();
    private final boolean printConnections;
    private final boolean printLog;
    private final boolean printReputations;

    public ClarinetNode(String prefix) {
        super(prefix);
        this.prefix = prefix;
        this.eventContextFactory = new EventContextFactory(
                Configuration.getInt(prefix + ".max_connections", 1),
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
        eventContextFactory.init(node);

        int round = printCounter.get();
        // Node 0 is malicious
        if(node.getID() == 1) {
            switch(round) {
                case 0 -> NeighborUtils.getNeighbor(node, protocolId, 0).ifPresent(receiver ->
                            eventContextFactory.connectionManager().requestConnection(node, receiver, protocolId));
                case 1 -> eventContextFactory.connectionManager().selectRandom(Type.OUTGOING)
                            .map(connection -> eventContextFactory.communicationManager()
                                    .send(node, connection, "Test message", protocolId)
                            )
                            .ifPresent(eventContextFactory.connectionManager()::release);
            }
        }
        if((round == 3 || round == 4) && node.getID() != 0) {
            // let the cooperative nodes query on rounds 3 and 4 so they query all their peers
            eventContextFactory.communicationManager().selectRandom()
                    .ifPresent(logEntry -> eventContextFactory.communicationManager().query(node, logEntry, protocolId));
        }

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
                eventContextFactory.connectionManager().printConnections(node);
            }
            if(printLog) {
                eventContextFactory.communicationManager().printLog(node);
            }
            if(printReputations) {
                eventContextFactory.reputationManager().printReputations(node);
            }
        }
    }

    @Override public void processEvent(Node node, int protocolId, Object event) {
        ClarinetMessage message = (ClarinetMessage) event;
        var ctx = eventContextFactory.create(node, protocolId);
        message.accept(messageHandler, ctx);
    }
}
