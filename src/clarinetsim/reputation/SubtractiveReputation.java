package clarinetsim.reputation;

import peersim.config.Configuration;

class SubtractiveReputation implements Reputation {

    private int value;
    private int interactions = 0;

    public SubtractiveReputation(String prefix) {
        value = Configuration.getInt(prefix + ".reputation.subtractive.initial");
    }

    @Override public void penalize(Penalty penalty) {
        value -= penalty.value();
        interactions++;
    }

    @Override public void reward() {
        // the rewarding is a no-op, but should count toward interactions
        interactions++;
    }

    @Override public int value() {
        return value;
    }

    @Override public int interactions() {
        return interactions;
    }
}
