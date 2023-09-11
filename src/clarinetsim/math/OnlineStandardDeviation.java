package clarinetsim.math;

import java.util.Optional;

public class OnlineStandardDeviation {

    private double n = 0;
    private double mean = 0;
    private double m2 = 0;

    public void update(double value) {
        n++;
        var delta = value - mean;
        mean += delta/n;
        var delta2 = value - mean;
        m2 += delta * delta2;
    }

    public Optional<Double> get() {
        if(n < 2) {
            return Optional.empty();
        }
        var variance = m2/(n-1);
        var standardDeviation = Math.sqrt(variance);
        return Optional.of(standardDeviation);
    }

}
