package io.micronaut.el.processor.visitor;

import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.VisitorContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The resolution of the type names the annotations of the module carry. A class literal reaches the processor
 * as the name of the type it names, which for an array is the descriptor the JVM writes it as.
 */
class ELTypesTest {

    /**
     * A context that fails on every call: the names below name types the resolution knows, so none of them is
     * looked up.
     */
    private static final VisitorContext NO_LOOKUP = (VisitorContext) Proxy.newProxyInstance(
        ELTypesTest.class.getClassLoader(),
        new Class<?>[]{VisitorContext.class},
        (proxy, method, args) -> {
            throw new AssertionError("The resolution should not consult the context: " + method.getName());
        });

    private static ClassElement resolve(String name) {
        return ELTypes.resolve(name, NO_LOOKUP);
    }

    @Test
    void thePrimitiveNames() {
        assertEquals("void", resolve("void").getName());
        assertEquals("boolean", resolve("boolean").getName());
        assertEquals("byte", resolve("byte").getName());
        assertEquals("short", resolve("short").getName());
        assertEquals("int", resolve("int").getName());
        assertEquals("long", resolve("long").getName());
        assertEquals("char", resolve("char").getName());
        assertEquals("float", resolve("float").getName());
        assertEquals("double", resolve("double").getName());
    }

    @Test
    void theDescriptorOfAnArrayOfPrimitives() {
        assertArray("[Z", "boolean", 1);
        assertArray("[B", "byte", 1);
        assertArray("[S", "short", 1);
        assertArray("[I", "int", 1);
        assertArray("[J", "long", 1);
        assertArray("[C", "char", 1);
        assertArray("[F", "float", 1);
        assertArray("[D", "double", 1);
    }

    @Test
    void theDescriptorOfAnArrayOfArrays() {
        assertArray("[[I", "int", 2);
        assertArray("[[[D", "double", 3);
    }

    @Test
    void theNameOfAnArrayWrittenWithBrackets() {
        assertArray("int[]", "int", 1);
        assertArray("double[][]", "double", 2);
    }

    @Test
    void aDescriptorOfATypeThatIsNotOne() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> resolve("[Q"));
        assertTrue(e.getMessage().contains("[Q"), e.getMessage());
    }

    private static void assertArray(String name, String component, int dimensions) {
        ClassElement resolved = resolve(name);
        assertTrue(resolved.isArray(), name + " is an array");
        assertEquals(dimensions, resolved.getArrayDimensions(), name + " has " + dimensions + " dimension(s)");
        assertEquals(component, resolved.getName(), name + " is an array of " + component);
    }
}
