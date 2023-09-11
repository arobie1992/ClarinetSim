package clarinetsim;

import clarinetsim.connection.Type;
import clarinetsim.context.EventContextFactory;
import clarinetsim.message.ClarinetMessage;
import clarinetsim.message.MessageHandler;
import clarinetsim.metrics.MetricsAggregator;
import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
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
        this.eventContextFactory = new EventContextFactory(prefix);
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
        MetricsAggregator.init();

        switch(CommonState.r.nextInt(5)) {
            case 0 -> eventContextFactory.networkManager().selectRandomNetworkNode(node)
                        .ifPresent(receiver -> eventContextFactory.connectionManager().requestConnection(node, receiver, protocolId));
            case 1 -> eventContextFactory.connectionManager().selectRandom(Type.OUTGOING)
                        .map(connection -> eventContextFactory.communicationManager()
                                .send(node, connection, "Test message", protocolId)
                        )
                        .ifPresent(eventContextFactory.connectionManager()::release);
            case 2 -> eventContextFactory.communicationManager().selectRandom()
                        .ifPresent(message -> eventContextFactory.communicationManager().query(node, message, protocolId));
            case 3 -> eventContextFactory.connectionManager().selectRandom(Type.OUTGOING).ifPresent(connection ->
                        eventContextFactory.connectionManager().terminate(connection, eventContextFactory.create(node, protocolId)));
            case 4 -> {} // just idle
        }
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
        MetricsAggregator.tick(node, eventContextFactory);
    }

    @Override public void processEvent(Node node, int protocolId, Object event) {
        ClarinetMessage message = (ClarinetMessage) event;
        var ctx = eventContextFactory.create(node, protocolId);
        message.accept(messageHandler, ctx);
    }
}
