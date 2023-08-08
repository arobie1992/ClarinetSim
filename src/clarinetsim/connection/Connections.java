package clarinetsim.connection;

import peersim.core.CommonState;
import peersim.core.Node;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Connections {

    private final int max = 1;
    private final Lock globalLock = new ReentrantLock();
    private final Map<String, Connection> connections = new HashMap<>();

    public Optional<Connection> addConnection(Node sender, Node receiver) {
        return insertConnection(sender, null, receiver, null, Type.OUTGOING, State.REQUESTING_RECEIVER);
    }

    public Optional<Connection> accept(Node sender, Node receiver, String connectionId) {
        return insertConnection(
                sender,
                null,
                receiver,
                Objects.requireNonNull(connectionId),
                Type.INCOMING,
                State.REQUESTING_WITNESS
        );
    }

    public Optional<Connection> witness(Node sender, Node witness, Node receiver, String connectionId) {
        return insertConnection(
                sender,
                Objects.requireNonNull(witness),
                receiver,
                Objects.requireNonNull(connectionId),
                Type.WITNESSING,
                State.OPEN
        );
    }

    private Optional<Connection> insertConnection(
            Node sender,
            Node witness,
            Node receiver,
            String connectionId,
            Type type,
            State state
    ) {
        Objects.requireNonNull(sender);
        Objects.requireNonNull(receiver);
        Objects.requireNonNull(type);
        Objects.requireNonNull(state);
        synchronized(globalLock) {
            if(connections.size() == max) {
                return Optional.empty();
            }

            switch(type) {
                case OUTGOING -> {
                    if(state != State.REQUESTING_RECEIVER) {
                        throw new IllegalArgumentException(type + " must be associated with " + State.REQUESTING_RECEIVER);
                    }
                }
                case INCOMING -> {
                    if(state != State.REQUESTING_WITNESS) {
                        throw new IllegalArgumentException(type + " must be associated with " + State.REQUESTING_WITNESS);
                    }
                }
                case WITNESSING -> {
                    if(state != State.OPEN) {
                        throw new IllegalArgumentException(type + " must be associated with " + State.OPEN);
                    }
                }
            }

            if(connectionId == null) {
                if(state != State.REQUESTING_RECEIVER) {
                    String err = "null connectionId can only be provided with state " + State.REQUESTING_RECEIVER;
                    throw new IllegalArgumentException(err);
                }
                connectionId = String.format("%s-%s-%s", sender.getID(), receiver.getID(), UUID.randomUUID());
            }
            if(witness != null && state != State.OPEN) {
                throw new IllegalArgumentException("witness may only be provided with state " + State.OPEN);
            }
            Connection connection = new Connection(connectionId, sender, witness, receiver, type, state);
            connections.put(connectionId, connection);
            return get(connectionId);
        }
    }

    /**
     * Removes the connection from the list of connections and releases the lock. After calling this method, absolutely
     * no further action with the connection is permitted. Doing so is undefined behavior. Only
     * {@link ConnectionManager#teardown(String)} or {@link ConnectionManager#teardown(String)} should call this
     * method.</br>
     * </br>
     * Callers are responsible for notifying other nodes involved in the connection of plans to terminate prior to
     * calling this method.
     *
     * @param connection The connection to be removed.
     */
    void remove(Connection connection) {
        Objects.requireNonNull(connection);
        synchronized(globalLock) {
            // to get the connection, the caller has to have locked it, so they'll have the lock on it
            // this means we can safely remove the connection without someone else grabbing it
            Connection removed = connections.remove(connection.connectionId());
            if(removed != connection) {
                // this should never happen; if it does there's a serious bug
                throw new IllegalStateException(String.format(
                        "Different connection removed than passed for ID %s! Passed: %s, Removed: %s",
                        connection.connectionId(),
                        connection,
                        removed
                ));
            }
            connection.unlock();
            connection.terminate();
        }
    }

    /**
     * Get the {@link Connection}. This operation locks the connection, so you <i>must</i> call
     * {@link Connections#release(Connection)} with the connection when you are finished. This is
     * a read/write lock, so only one thread may access the connection at a time.
     * @param connectionId The ID of the connection to get.
     * @return the {@link Connection} with its lock obtained.
     */
    Optional<Connection> get(String connectionId) {
        Objects.requireNonNull(connectionId);
        Connection connection = connections.get(connectionId);
        if(connection == null) {
            return Optional.empty();
        }
        connection.lock();
        if(connections.containsKey(connectionId)) {
            return Optional.of(connection);
        } else {
            // someone removed the connection between us grabbing it and obtaining the lock
            // release the lock and return empty to honor that removal
            connection.unlock();
            return Optional.empty();
        }
    }

    Optional<Connection> selectRandom(Type type) {
        synchronized(globalLock) {
            var outgoingConnections = connections.values().stream().filter(c -> c.type() == type).toList();
            if(outgoingConnections.isEmpty()) {
                return Optional.empty();
            }
            int connectionIndex = CommonState.r.nextInt(outgoingConnections.size());
            Connection outgoing = outgoingConnections.get(connectionIndex);
            return get(outgoing.connectionId());
        }
    }

    void release(Connection connection) {
        connection.unlock();
    }

    @Override public String toString() {
        return "Connections " + connections.values();
    }
}
