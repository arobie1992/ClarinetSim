package clarinetsim.reputation;

interface Reputation {
    void penalize(Penalty penalty);
    void reward();
    int value();
    int interactions();
}
