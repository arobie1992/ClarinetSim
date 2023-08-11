package clarinetsim.metrics;

import java.util.StringJoiner;

class ReputationInformation<K extends Comparable<K>> {
    final ReputationStats<K> coop = new ReputationStats<>();

    final ReputationStats<K> mal = new ReputationStats<>();

    final ReputationStats<K> withNeighbors = new ReputationStats<>();

    void addCooperative(K neighborId, int reputation) {
        coop.add(neighborId, reputation);
    }

    void addMalicious(K neighborId, int reputation) {
        mal.add(neighborId, reputation);
    }

    @Override public String toString() {
        var sj = new StringJoiner(System.lineSeparator());
        sj.add("{");
        coop.addAggregated(sj, "coop");
        mal.addAggregated(sj, "mal");
        withNeighbors.addAggregated(sj, "repWithNeighbors");
        coop.addIndividuals(sj, "individualCoop");
        mal.addIndividuals(sj, "individualMal");
        withNeighbors.addIndividuals(sj, "individualRepWithNeighbors");
        sj.add("}");
        return sj.toString();
    }
}
