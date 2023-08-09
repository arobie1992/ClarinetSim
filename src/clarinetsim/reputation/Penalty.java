package clarinetsim.reputation;

public enum Penalty {
    STRONG(3),
    WEAK(1);

    private final int value;

    Penalty(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
