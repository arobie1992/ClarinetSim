package clarinetsim.reputation;

interface Reputation {
    void penalize(Penalty penalty);
    void reward();
    double value();
    int interactions();
}
