package clarinetsim.metrics;

public record ReputationInformation(
        double average,
        double min,
        double max,
        double standardDeviation,
        long totalPeers,
        long numTrusted,
        long numUntrusted,
        long numMessages,
        long numMessagesWithTrusted,
        long numMessagesWithUntrusted,
        long numAssessments,
        long numAssessmentsOfTrusted,
        long numAssessmentsOfUntrusted
) {
}
