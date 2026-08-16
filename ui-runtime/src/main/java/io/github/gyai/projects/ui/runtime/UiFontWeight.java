package io.github.gyai.projects.ui.runtime;

public enum UiFontWeight {
    NORMAL(400), MEDIUM(500), SEMIBOLD(600), BOLD(700);

    private final int numericWeight;

    UiFontWeight(int numericWeight) {
        this.numericWeight = numericWeight;
    }

    public int numericWeight() { return numericWeight; }
}
