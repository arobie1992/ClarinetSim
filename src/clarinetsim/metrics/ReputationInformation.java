package clarinetsim.metrics;

import clarinetsim.MathUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.StringJoiner;

class ReputationInformation<K extends Comparable<K>> {
    private final ReputationStats<K> coop = new ReputationStats<>();

    private final ReputationStats<K> mal = new ReputationStats<>();

    final ReputationStats<K> withNeighbors = new ReputationStats<>();

    void addCooperative(K neighborId, int reputation) {
        coop.add(neighborId, reputation);
    }

    void addMalicious(K neighborId, int reputation) {
        mal.add(neighborId, reputation);
    }

    private Collection<Integer> reputations() {
        var values = new ArrayList<>(coop.reputations());
        values.addAll(mal.reputations());
        return values;
    }

    private int stdev() {
        return MathUtils.stdev(reputations());
    }

    private int avg() {
        var reputations = reputations();
        var total = reputations.stream().reduce(Integer::sum).orElse(null);
        return total == null ? 0 : total/reputations.size();
    }

    private int median() {
        var sorted = reputations().stream().sorted().toList();
        return sorted.isEmpty() ? 0 : sorted.get(sorted.size()/2);
    }

    @Override public String toString() {
        var sj = new StringJoiner(System.lineSeparator());
        sj.add("{");
        coop.addAggregated(sj, "coop");
        mal.addAggregated(sj, "mal");
        withNeighbors.addAggregated(sj, "repWithNeighbors");

        var stdev = stdev();
        var avg = avg();
        sj.add("    total: {");
        sj.add("        average: " + avg);
        sj.add("        median: " + median());
        sj.add("        stdev: " + stdev);
        sj.add("        numCoopBelow: " + coop.numBelowStdev(stdev, avg));
        sj.add("        numMalBelow: " + mal.numBelowStdev(stdev, avg));
        sj.add("        numMalActedMaliciously: " + mal.reputations().stream().filter(i -> i < 100).count());
        sj.add("    }");

        coop.addIndividuals(sj, "individualCoop");
        mal.addIndividuals(sj, "individualMal");
        withNeighbors.addIndividuals(sj, "individualRepWithNeighbors");
        sj.add("}");
        return sj.toString();
    }
}
