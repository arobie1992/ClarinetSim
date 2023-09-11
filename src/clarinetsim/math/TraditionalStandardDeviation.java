package clarinetsim.math;

import java.util.List;
import java.util.Optional;

public class TraditionalStandardDeviation {

    public Optional<Double> calculate(List<Double> values) {
        if(values.size() < 2) {
            return Optional.empty();
        }

        var mean = 0.0;
        for(var i : values) {
            mean += i;
        }
        mean /= values.size();

        // The variance
        var variance = 0.0;
        for(var i : values) {
            variance += Math.pow(i-mean, 2);
        }
        variance /= (values.size()-1);

        // Standard Deviation
        return Optional.of(Math.sqrt(variance));
    }

}
