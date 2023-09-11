package clarinetsim.connection;

import clarinetsim.NeighborUtils;
import clarinetsim.context.EventContext;
import clarinetsim.message.*;
import peersim.core.Node;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConnectionManager {

    private final Connections connections;

    public ConnectionManager(int maxConnections) {
        this(new Connections(maxConnections));
    }

    ConnectionManager(Connections connections) {
        this.connections = connections;
    }

    public void requestConnection(Node sender, Node receiver, int protocolId) {
        // if it's present, we do the operations
        // if it's empty, that means we were at max, which is fine
        connections.addConnection(sender, receiver).ifPresent(connection -> {
            // notify the tentative receiver to see if they're willing to accept
            ConnectionRequest message = new ConnectionRequest(sender, connection.connectionId());
            NeighborUtils.send(sender, receiver, message, protocolId);
            connections.release(connection);
        });
    }

    public void tryAccept(ConnectionRequest request, EventContext ctx) {
        connections.accept(request.sender(), ctx.self(), request.connectionId())
                .map(connection -> {
                    // we don't make further updates so it's okay to release it now
                    connections.release(connection);
                    return true;
                })
                .or(() -> Optional.of(false))
                .ifPresent(accepted -> {
                    ConnectionResponse resp = new ConnectionResponse(request, accepted);
                    NeighborUtils.send(request.sender(), resp, ctx);
                });
    }

    public void tryWitness(WitnessRequest request, EventContext ctx) {
        connections.witness(request.sender(), ctx.self(), request.receiver(), request.connectionId())
                .map(connection -> {
                    // we don't make further updates so it's okay to release it now
                    connections.release(connection);
                    return true;
                })
                .or(() -> Optional.of(false))
                .ifPresent(accepted -> {
                    WitnessResponse resp = new WitnessResponse(ctx.self(), request.connectionId(), accepted);
                    NeighborUtils.send(request.sender(), resp, ctx);
                });
    }

    public Optional<Connection> initializeWitnessCandidates(ConnectionResponse response, EventContext ctx) {
        // if we didn't get the connection, then it's been terminated so we can just pass the buck up
        return connections.get(response.connectionId()).map(connection -> {
            connection.receiverConfirmed();
            List<Node> candidates = ctx.networkManager().peers(ctx.self(), ctx.protocolId()).stream()
                    .filter(n -> ctx.reputationManager().evaluate(n))
                    .filter(n -> n.getID() != connection.receiver().getID())
                    .collect(Collectors.toList());
            connection.addWitnessCandidates(candidates);
            return connection;
        });
    }

    public void requestWitness(String connectionId, EventContext ctx) {
        connections.get(connectionId).ifPresent(connection -> requestWitness(connection, ctx));
    }

    public void requestWitness(Connection connection, EventContext ctx) {
        connection.selectCandidate().ifPresentOrElse(
                candidate -> {
                    WitnessRequest msg = new WitnessRequest(connection);
                    NeighborUtils.send(candidate, msg, ctx);
                    connections.release(connection);
                },
                // terminate the connection if we're out of candidates
                () -> terminate(connection.connectionId(), ctx)
        );
    }

    public void confirmWitness(WitnessResponse response, EventContext ctx) {
        Optional<Connection> connectionOpt = connections.get(response.connectionId());
        if(connectionOpt.isEmpty()) {
            // the connection was shutdown before the witness could be confirmed
            // let the responding witness know to close its connection
            ConnectionTerminate msg = new ConnectionTerminate(response.connectionId());
            NeighborUtils.send(response.witness(), msg, ctx);
            return;
        }
        Connection connection = connectionOpt.get();
        if(connection.witness().isPresent()) {
            // a different requested witness beat it in accepting so honor that
            // let the responding witness know to close its connection
            // I don't think this should happen, but it doesn't hurt to cover it
            ConnectionTerminate msg = new ConnectionTerminate(response.connectionId());
            NeighborUtils.send(response.witness(), msg, ctx);
        } else {
            connection.confirmWitness(response.witness());
            WitnessSelection msg = new WitnessSelection(response);
            NeighborUtils.send(connection.receiver(), msg, ctx);
        }
        connections.release(connection);
    }

    public void acceptWitness(WitnessSelection selection) {
        connections.get(selection.connectionId()).ifPresent(connection -> {
            connection.confirmWitness(selection.witness());
            connections.release(connection);
        });
    }

    public Optional<Connection> selectRandom(Type type) {
        return connections.selectRandom(type);
    }

    public Optional<Connection> get(String connectionId) {
        return connections.get(connectionId);
    }

    public void release(Connection connection) {
        connections.release(connection);
    }

    /**
     * Notify nodes involved in the connection of plans to terminate and then remove the connection. Only the sender
     * should call this method. Other participants in the connection should use
     * {@link ConnectionManager#teardown(String)}.
     *
     * @param connectionId The ID of the connection to terminate.
     * @param ctx The context of the event that led to termination.
     */
    public void terminate(String connectionId, EventContext ctx) {
        connections.get(connectionId).ifPresent(connection -> terminate(connection, ctx));
    }

    /**
     * Notify nodes involved in the connection of plans to terminate and then remove the connection. Only the sender
     * should call this method. Other participants in the connection should use
     * {@link ConnectionManager#teardown(String)}.
     *
     * @param connection The connection to terminate.
     * @param ctx The context of the event that led to termination.
     */
    public void terminate(Connection connection, EventContext ctx) {
        ConnectionTerminate msg = new ConnectionTerminate(connection.connectionId());
        NeighborUtils.send(connection.receiver(), msg, ctx);
        connection.witness().ifPresent(witness -> NeighborUtils.send(witness, msg, ctx));
        connections.remove(connection);
    }

    /**
     * Removes the connection without notifying other participants. Only witness or receiver should call this. Sender
     * should call {@link ConnectionManager#terminate(String, EventContext)}.
     *
     * @param connectionId The ID of the connection to teardown.
     */
    public void teardown(String connectionId) {
        connections.get(connectionId).ifPresent(connections::remove);
    }

    public void printConnections(Node node) {
        System.out.println("Node " + node.getID() + " " + connections);
    }
}
