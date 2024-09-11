package clarinetsim.reputation;

import java.util.List;

public interface ReputationAlg {
    double calculate(List<MessageAssessment> assessments);
}
