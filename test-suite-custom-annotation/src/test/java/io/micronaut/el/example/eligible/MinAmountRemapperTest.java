package io.micronaut.el.example.eligible;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class MinAmountRemapperTest {

    private final MinAmountRemapper remapper = new MinAmountRemapper();

    @Test
    void theDefaultsOfTheAnnotationApplyWhenTheMembersAreLeftOut() {
        AnnotationValue<?> annotation = new AnnotationValue<>(MinAmount.class.getName(), Map.of("value", 100L));

        List<AnnotationValue<?>> remapped = remapper.remap(annotation, null);

        AnnotationValue<?> minAmount = remapped.get(0);
        assertEquals(MinAmount.class.getName(), minAmount.getAnnotationName());
        assertEquals(MinAmount.DEFAULT_MESSAGE.replace("${", "#{"), minAmount.stringValue("message").orElseThrow());
        assertEquals(false, minAmount.booleanValue("inclusive").orElseThrow());
        assertEquals(100L, minAmount.longValue().orElseThrow());
        // one compiled expression per ${...} segment of the message, then the variables of the expressions
        assertEquals(ELExpression.class.getName(), remapped.get(1).getAnnotationName());
        assertEquals(ELEnvironment.class.getName(), remapped.get(remapped.size() - 1).getAnnotationName());
    }

    @Test
    void theMembersGivenAreKept() {
        AnnotationValue<?> annotation = new AnnotationValue<>(MinAmount.class.getName(),
            Map.of("value", 10L, "inclusive", true, "message", "At least ${value}"));

        List<AnnotationValue<?>> remapped = remapper.remap(annotation, null);

        AnnotationValue<?> minAmount = remapped.get(0);
        assertEquals("At least #{value}", minAmount.stringValue("message").orElseThrow());
        assertEquals(true, minAmount.booleanValue("inclusive").orElseThrow());
        assertEquals(3, remapped.size());
    }

    @Test
    void otherAnnotationsAreLeftAlone() {
        AnnotationValue<?> annotation = new AnnotationValue<>(Eligible.class.getName(), Map.of("value", "true"));

        List<AnnotationValue<?>> remapped = remapper.remap(annotation, null);

        assertEquals(1, remapped.size());
        assertSame(annotation, remapped.get(0));
    }
}
