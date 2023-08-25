package clarinetsim.log;

import clarinetsim.connection.Connection;
import clarinetsim.message.Data;
import clarinetsim.message.Query;
import clarinetsim.message.QueryForward;
import clarinetsim.message.QueryResponse;
import peersim.core.CommonState;
import peersim.core.Node;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Log {

    private final Map<Key, LogEntry> log = new ConcurrentHashMap<>();
    private final List<Key> queryCandidates = new ArrayList<>();
    private final List<Query> queryLog = Collections.synchronizedList(new ArrayList<>());
    private final List<QueryResponse> responseLog = Collections.synchronizedList(new ArrayList<>());
    private final List<QueryForward> forwardLog = Collections.synchronizedList(new ArrayList<>());

    private record Key(String connectionId, int seqNo) {
        private Key(Data message) {
            this(message.connectionId(), message.seqNo());
        }
    }

    public void add(Node self, Connection connection, Data message) {
        log.compute(new Key(message), (k, v) -> {
           if(v != null) {
               // this should never happen, but if it does, we need to revisit the key generation
               throw new IllegalStateException("Encountered duplicate key for messages " + v + " and " + message);
           }
           var logEntry = new LogEntry(connection, message);
           logEntry.markQueried(self);
            synchronized(queryCandidates) {
                queryCandidates.add(k);
            }
           return logEntry;
        });
    }

    public void add(Query query) {
        queryLog.add(query);
    }

    public void add(QueryResponse queryResponse) {
        responseLog.add(queryResponse);
    }

    public void add(QueryForward queryForward) {
        forwardLog.add(queryForward);
    }

    public Optional<LogEntry> find(String connectionId, int seqNo) {
        return Optional.ofNullable(log.get(new Key(connectionId, seqNo)));
    }

    public Optional<LogEntry> random() {
        synchronized(queryCandidates) {
            if(queryCandidates.isEmpty()) {
                return Optional.empty();
            }
            var index = CommonState.r.nextInt(queryCandidates.size());
            var key = queryCandidates.get(index);
            return Optional.ofNullable(log.get(key));
        }
    }

    public void markQueried(LogEntry logEntry, Node node) {
        logEntry.markQueried(node);
        if(!logEntry.queryCandidate()) {
            synchronized(queryCandidates) {
                queryCandidates.remove(new Key(logEntry.message()));
            }
        }
    }

    @Override public String toString() {
        return "Log { data: " + log
                + ", queries: " + queryLog
                + ", responses: " + responseLog
                + ", forwards: " + forwardLog
                + " }";
    }
}
