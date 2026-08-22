package io.micronaut.el.runtime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ELArithmeticTest {

    @Test
    void addition() {
        assertEquals(0L, ELArithmetic.add(null, null));
        assertEquals(3L, ELArithmetic.add(1, 2));
        assertEquals(3.5d, ELArithmetic.add(1.5d, 2));
        assertEquals(3.5d, ELArithmetic.add("1.5", 2));
        assertEquals(new BigDecimal("3.5"), ELArithmetic.add(new BigDecimal("1.5"), 2));
        assertEquals(BigInteger.valueOf(3), ELArithmetic.add(BigInteger.ONE, 2));
        assertEquals(1L, ELArithmetic.add(null, 1));
    }

    @Test
    void division() {
        assertEquals(0L, ELArithmetic.divide(null, null));
        assertEquals(3.5d, ELArithmetic.divide(7, 2));
        assertEquals(new BigDecimal("4"), ELArithmetic.divide(BigInteger.valueOf(7), 2));
    }

    @Test
    void modulo() {
        assertEquals(1L, ELArithmetic.mod(7, 2));
        assertEquals(1.0d, ELArithmetic.mod(7.0d, 2));
        assertEquals(BigInteger.ONE, ELArithmetic.mod(BigInteger.valueOf(7), 2));
    }

    @Test
    void negation() {
        assertEquals(0L, ELArithmetic.negate(null));
        assertEquals(-1L, ELArithmetic.negate(1L));
        assertEquals(-1.5d, ELArithmetic.negate("1.5"));
        assertEquals(new BigDecimal("-1.5"), ELArithmetic.negate(new BigDecimal("1.5")));
    }

    @Test
    void concatenation() {
        assertEquals("ab", ELArithmetic.concat("a", "b"));
        assertEquals("a", ELArithmetic.concat("a", null));
    }
}
