package clarinetsim.reputation;

import peersim.config.Configuration;

class ProportionalReputation implements Reputation {

    double good = 0;
    double total = 0;
    private int interactions = 0;
    private final boolean strongMult;
    private static final String STRONG_TYPE_PROP = ".reputation.proportional.strong_type";

    public ProportionalReputation(String prefix) {
        var strongPenType = Configuration.getString(prefix + STRONG_TYPE_PROP);
        strongMult = switch(strongPenType) {
            case "MULT" -> true;
            case "ADD" -> false;
            default -> throw new IllegalArgumentException("Unrecognized " + prefix + STRONG_TYPE_PROP + strongPenType);
        };
    }

    @Override public void penalize(Penalty penalty) {
        if(strongMult && penalty instanceof StrongPenalty) {
            // this isn't actually what I meant to do--should have been (value * total)/100, but this is what got tested
            // and analyzed so leaving it as is; once other configurations are implemented might revisit testing this
            total += Math.max(penalty.value(), penalty.value() * total);
        } else {
            total += penalty.value();
        }
        interactions++;
    }

    @Override public void reward() {
        good++;
        total++;
        interactions++;
    }

    @Override public double value() {
        return total == 0 ? 100 : ((good/total) * 100);
    }

    @Override public int interactions() {
        return interactions;
    }
}
