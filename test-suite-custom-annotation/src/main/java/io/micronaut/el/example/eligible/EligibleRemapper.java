package io.micronaut.el.example.eligible;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.expressions.EvaluatedExpressionReference;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.inject.annotation.AnnotationRemapper;
import io.micronaut.inject.visitor.VisitorContext;

import java.util.List;
import java.util.Map;

/**
 * The annotation processor of {@link Eligible}: it has the condition compiled at compilation time.
 *
 * <p>Micronaut treats any annotation string containing {@code #{...}} as one of its own evaluated expressions,
 * and would fail to compile an expression of the Jakarta language. This remapper, which runs inside the
 * annotation metadata builder before anything else sees the annotation, takes the text back and returns two
 * annotations in place of the one it was given: the same {@code @Eligible} holding the text as written, and an
 * {@link ELExpression} declaring it, which micronaut-jakarta-el compiles into the registry of the class.</p>
 *
 * <p>At runtime the text is read back from the annotation metadata and handed to
 * {@code jakarta.el.ExpressionFactory}, which returns the compiled expression: nothing is parsed.</p>
 */
public final class EligibleRemapper implements AnnotationRemapper {

    @Override
    public String getPackageName() {
        return Eligible.class.getPackageName();
    }

    @Override
    public List<AnnotationValue<?>> remap(AnnotationValue<?> annotation, VisitorContext visitorContext) {
        if (!annotation.getAnnotationName().equals(Eligible.class.getName())) {
            return List.of(annotation);
        }
        String condition = textOf(annotation.getValues().get(AnnotationMetadata.VALUE_MEMBER));
        if (condition == null) {
            return List.of(annotation);
        }
        return List.of(
            AnnotationValue.builder(Eligible.class).value(condition).build(),
            AnnotationValue.builder(ELExpression.class)
                .value(condition)
                .member("expectedType", new AnnotationClassValue<>(Boolean.class))
                .build()
        );
    }

    /**
     * The condition as written, with {@code ${...}} stored as {@code #{...}}: the specification treats the two
     * alike, while Micronaut resolves a {@code ${...}} in the metadata of a bean as a property placeholder.
     */
    private static String textOf(Object value) {
        String text;
        if (value instanceof EvaluatedExpressionReference reference) {
            text = reference.annotationValue().toString();
        } else if (value instanceof String string) {
            text = string;
        } else {
            return null;
        }
        text = text.trim();
        return text.startsWith("${") ? "#" + text.substring(1) : text;
    }
}
