package clarinetsim.reputation;

import peersim.core.Node;

import java.util.Map;

interface Reputations {
    boolean evaluate(Node node);
    void penalize(Node node, Penalty penalty);
    Map<Long, Integer> reputations();
    void reward(Node node);
}
