package clarinetsim;

public enum Penalty {
    WEAK(1),
    STRONG(3);

    private final int value;

    Penalty(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
