package clarinetsim.reputation;

sealed interface Penalty permits StrongPenalty, WeakPenalty {
    double value();
}
