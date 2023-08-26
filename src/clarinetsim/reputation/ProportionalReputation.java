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

    @Override public int value() {
        return total == 0 ? 100 : (int) ((good/total) * 100);
    }

    @Override public int interactions() {
        return interactions;
    }
}
