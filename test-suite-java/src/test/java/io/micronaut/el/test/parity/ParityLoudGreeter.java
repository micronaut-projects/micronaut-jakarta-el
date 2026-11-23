package io.micronaut.el.test.parity;

/**
 * The generated twin of the {@code LoudGreeter} of {@code ELMethodContributorTest}: its own {@code pick} is the
 * one that runs on an instance of it, whatever the declared type of the variable.
 */
public class ParityLoudGreeter extends ParityGreeter {

    public ParityLoudGreeter(String name) {
        super(name);
    }

    @Override
    public String pick(Number value) {
        return "child";
    }
}
