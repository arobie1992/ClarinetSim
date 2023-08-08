package clarinetsim.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Log {

    private final Map<Key, List<Data>> log = new ConcurrentHashMap<>();

    private record Key(String connectionId, int seqNo) {
        private Key(Data message) {
            this(message.connectionId(), message.seqNo());
        }
    }

    public void add(Data message) {
        log.compute(new Key(message), (k, v) -> {
           if(v == null) {
               v = new ArrayList<>();
           }
           v.add(message);
           return v;
        });
    }

    public static void main(String[] args) {
        System.out.println(Map.of("a", "b"));
    }

    @Override public String toString() {
        return "Log " + log;
    }
}
