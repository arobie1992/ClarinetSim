package clarinetsim;

import peersim.config.Configuration;
import peersim.core.Node;

public class GlobalState {

    private static volatile int numMalicious = -1;

    private GlobalState() {}

    public static boolean isMalicious(Node node) {
        return isMalicious(node.getID());
    }

    public static boolean isMalicious(long nodeId) {
        // this is idempotent, so it's fine if multiple threads write it
        if(numMalicious == -1) {
            numMalicious = Configuration.getInt("protocol.avg.num_malicious", 0);
        }
        return nodeId < numMalicious;
    }

}
