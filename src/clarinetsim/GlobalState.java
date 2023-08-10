package clarinetsim;

import peersim.config.Configuration;
import peersim.core.Node;

public class GlobalState {

    private static int numMalicious = -1;

    private GlobalState() {}

    public static boolean isMalicious(Node node) {
        // this is idempotent, so it's fine if multiple threads write it
        if(numMalicious == -1) {
            Configuration.getInt("protocol.avg.num_malicious", 0);
        }
        return node.getID() < numMalicious;
    }

}
