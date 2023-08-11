package clarinetsim.metrics;

import peersim.config.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

class ReputationStats<K extends Comparable<K>> {
    Map<K, Integer> reputations = new HashMap<>();
    int min = Integer.MAX_VALUE;
    int max = Integer.MIN_VALUE;

    void add(K id, int reputation) {
        reputations.put(id, reputation);
        min = Math.min(min, reputation);
        max = Math.max(max, reputation);
    }

    Integer average() {
        var total = reputations.values().stream().reduce(Integer::sum).orElse(null);
        return total == null ? null : total/reputations.size();
    }

    Integer median() {
        var sorted = reputations.values().stream().sorted().toList();
        return sorted.isEmpty() ? null : sorted.get(sorted.size()/2);
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
        if(!Configuration.getBoolean("protocol.avg.metrics.print_individual", false)) {
            sj.add("    "+name+": <not printed>");
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

    private List<Map.Entry<K, Integer>> sorted(Map<K, Integer> map) {
        return map.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
    }
}
