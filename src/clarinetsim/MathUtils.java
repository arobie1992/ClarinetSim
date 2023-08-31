package clarinetsim;

import java.util.Collection;

public class MathUtils {
    private MathUtils() {}

    public static int stdev(Collection<Integer> values) {
        // The mean average
        double mean = 0.0;
        for(Integer i : values) {
            mean += i.doubleValue();
        }
        mean /= values.size();

        // The variance
        double variance = 0;
        for(var i : values) {
            variance += Math.pow(i.doubleValue() - mean, 2);
        }
        variance /= values.size();

        // Standard Deviation
        double std = Math.sqrt(variance);
        return Double.valueOf(std).intValue();
    }
}
