package clarinetsim.math;

import java.util.Collection;

public class MathUtils {

    private MathUtils() {}

    public static double mean(Collection<Double> values) {
        var mean = 0.0;
        for(var i : values) {
            mean += i;
        }
        return mean / values.size();
    }

    public static double standardDeviation(Collection<Double> values) {
        var mean = mean(values);

        // The variance
        var variance = 0.0;
        for(var i : values) {
            variance += Math.pow(i-mean, 2);
        }
        variance /= (values.size()-1);

        // Standard Deviation
        return Math.sqrt(variance);
    }

}