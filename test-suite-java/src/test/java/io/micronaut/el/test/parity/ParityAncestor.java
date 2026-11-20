package io.micronaut.el.test.parity;

/**
 * The interface declaring {@code pick} first, for {@link ParityDiamond}.
 */
public interface ParityAncestor {

    default String pick(Number value) {
        return "ancestor";
    }

    /**
     * An interface that only inherits {@code pick}.
     */
    interface Other extends ParityAncestor {
    }

    /**
     * An interface that redeclares {@code pick}.
     */
    interface Nearer extends ParityAncestor {

        @Override
        default String pick(Number value) {
            return "nearer";
        }
    }
}
