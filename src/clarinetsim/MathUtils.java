package clarinetsim;

import java.util.Collection;

public class MathUtils {
    private MathUtils() {}

    public static double average(Collection<Double> values) {
        double mean = 0.0;
        for(var i : values) {
            mean += i;
        }
        return mean / values.size();
    }

    public static double stdev(Collection<Double> values) {
        double mean = average(values);

        // The variance
        double variance = 0;
        for(var i : values) {
            variance += Math.pow(i - mean, 2);
        }
        variance /= values.size();

        // Standard Deviation
        return Math.sqrt(variance);
    }
}
