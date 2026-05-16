package dev.beryl.lattice.hook;

public enum HookPriority {
    LOWEST(0),
    LOW(25),
    NORMAL(50),
    HIGH(75),
    HIGHEST(100);

    private final int weight;

    HookPriority(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
