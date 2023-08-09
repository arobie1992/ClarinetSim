package clarinetsim.log;

import peersim.core.Node;

import java.util.Objects;

class NodeQueryInfo {
    final Node node;
    boolean queried = false;

    public NodeQueryInfo(Node node) {
        this.node = Objects.requireNonNull(node);
    }

    @Override public String toString() {
        return "NodeQueryInfo { node: " + node.getID() + ", queried: " + queried + " }";
    }
}
