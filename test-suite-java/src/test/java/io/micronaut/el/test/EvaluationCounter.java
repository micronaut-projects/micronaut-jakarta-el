package io.micronaut.el.test;

public final class EvaluationCounter {

    private int calls;

    public long bump() {
        calls++;
        return calls;
    }

    public int getCalls() {
        return calls;
    }
}
