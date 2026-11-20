package io.micronaut.el.test.parity;

/**
 * The generated twin of the {@code Diamond} of {@code ELMethodContributorTest}: it implements the interface that
 * only inherits {@code pick} before the one that redeclares it, and the Java rules for default methods select the
 * nearer declaration, which is the one the contributed registry must select too.
 */
public final class ParityDiamond implements ParityAncestor.Other, ParityAncestor.Nearer {
}
