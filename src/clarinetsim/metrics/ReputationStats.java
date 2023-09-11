package clarinetsim.metrics;

import peersim.config.Configuration;

import java.util.*;

class ReputationStats<K extends Comparable<K>> {
    Map<K, Double> reputations = new HashMap<>();
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;

    void add(K id, double reputation) {
        reputations.put(id, reputation);
        min = Math.min(min, reputation);
        max = Math.max(max, reputation);
    }

    Double average() {
        var total = reputations.values().stream().reduce(Double::sum).orElse(null);
        return total == null ? null : total/reputations.size();
    }

    Double median() {
        var sorted = reputations.values().stream().sorted().toList();
        return sorted.isEmpty() ? null : sorted.get(sorted.size()/2);
    }

    long numBelowStdev(double stdev, double average) {
        double threshold = average - stdev;
        return reputations.values().stream().filter(rep -> rep < threshold).count();
    }

    void addAggregated(StringJoiner sj, String name) {
        if(!reputations.isEmpty()) {
            sj.add("    "+name+": {");
            sj.add("        average: " + average());
            sj.add("        median: " + median());
            sj.add("        min: " + min);
            sj.add("        max: " + max);
            sj.add("    }");
        } else {
            sj.add("    "+name+": {}");
        }
    }

    void addIndividuals(StringJoiner sj, String name) {
        if(!Configuration.getBoolean("protocol.clarinet.metrics.print_individual", false)) {
            sj.add("    "+name+": <omitted>");
        } else if(reputations.isEmpty()) {
            sj.add("    "+name+": []");
        } else {
            sj.add("    "+name+": [");
            var sorted = sorted(reputations);
            for(var e : sorted) {
                sj.add("        node " + e.getKey() + ": " + e.getValue());
            }
            sj.add("    ]");
        }
    }

    private List<Map.Entry<K, Double>> sorted(Map<K, Double> map) {
        return map.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
    }

    Collection<Double> reputations() {
        return reputations.values();
    }
}
