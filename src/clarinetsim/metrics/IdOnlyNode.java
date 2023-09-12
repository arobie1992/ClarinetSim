package clarinetsim.metrics;

import peersim.core.Node;
import peersim.core.Protocol;

/* The entire existence of this class is a bit of a kludge, but it lets us use ReputationManager.evaluate(Node)
   without having retrieve the node or introduce yet another method that's only used by the MetricsAggregator
 */
class IdOnlyNode implements Node {

    private final long id;

    public IdOnlyNode(long id) {
        this.id = id;
    }

    @Override public int getFailState() {
        throw new UnsupportedOperationException();
    }

    @Override public void setFailState(int failState) {
        throw new UnsupportedOperationException();
    }

    @Override public boolean isUp() {
        throw new UnsupportedOperationException();
    }

    @Override public Protocol getProtocol(int i) {
        throw new UnsupportedOperationException();
    }

    @Override public int protocolSize() {
        throw new UnsupportedOperationException();
    }

    @Override public void setIndex(int index) {
        throw new UnsupportedOperationException();
    }

    @Override public int getIndex() {
        throw new UnsupportedOperationException();
    }

    @Override public long getID() {
        return id;
    }

    @Override public Object clone() {
        throw new UnsupportedOperationException();
    }
}
