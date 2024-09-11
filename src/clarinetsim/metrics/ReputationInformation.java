package clarinetsim.metrics;

public record ReputationInformation(
        double average,
        double min,
        double max,
        double standardDeviation,
        long totalPeers,
        long numTrusted,
        long numNotTrusted
) {
}
