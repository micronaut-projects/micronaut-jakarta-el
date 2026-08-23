package io.micronaut.el.example.eligible;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.AnnotationValueBuilder;
import io.micronaut.core.expressions.EvaluatedExpressionReference;
import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELFunctions;
import io.micronaut.inject.annotation.AnnotationRemapper;
import io.micronaut.inject.visitor.VisitorContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The annotation processor of {@link Eligible}: it has the condition, and the message, compiled at compilation
 * time.
 *
 * <p>Micronaut treats any annotation string containing {@code #{...}} as one of its own evaluated expressions,
 * and would fail to compile an expression of the Jakarta language. This remapper, which runs inside the
 * annotation metadata builder before anything else sees the annotation, takes the text back and returns, in
 * place of the one annotation it was given:</p>
 *
 * <ul>
 *     <li>the same {@code @Eligible}, holding the text as written;</li>
 *     <li>an {@link ELExpression} for the condition, expected to be a {@link Boolean}, and one for the message,
 *     expected to be a {@link String}, which micronaut-jakarta-el compiles into the registry of the class;</li>
 *     <li>an {@link ELEnvironment} listing {@link EligibilityFunctions}, whose functions declare their own names
 *     and prefix, and importing {@link Locale}; declared on the method, it applies to the expressions declared
 *     there. The parameters of the method are typed variables without any declaration.</li>
 * </ul>
 *
 * <p>At runtime the text is read back from the annotation metadata and handed to
 * {@code jakarta.el.ExpressionFactory}, which returns the compiled expression: nothing is parsed.</p>
 */
// tag::remapper[]
public final class EligibleRemapper implements AnnotationRemapper {

    @Override
    public String getPackageName() {
        return Eligible.class.getPackageName(); // <1>
    }

    @Override
    public List<AnnotationValue<?>> remap(AnnotationValue<?> annotation, VisitorContext visitorContext) {
        if (!annotation.getAnnotationName().equals(Eligible.class.getName())) {
            return List.of(annotation);
        }
        String condition = textOf(annotation.getValues().get(AnnotationMetadata.VALUE_MEMBER)); // <2>
        if (condition == null) {
            return List.of(annotation);
        }
        String otherwise = textOf(annotation.getValues().get("otherwise"));
        String name = annotation.stringValue("name").orElse("");

        List<AnnotationValue<?>> remapped = new ArrayList<>();
        AnnotationValueBuilder<Eligible> eligible = AnnotationValue.builder(Eligible.class)
            .value(condition)
            .member("name", name);
        if (otherwise != null) {
            eligible.member("otherwise", otherwise);
        }
        remapped.add(eligible.build()); // <3>
        remapped.add(AnnotationValue.builder(ELExpression.class) // <4>
            .value(condition)
            .member("expectedType", new AnnotationClassValue<>(Boolean.class))
            .member("name", name)
            .build());
        if (otherwise != null) {
            remapped.add(AnnotationValue.builder(ELExpression.class)
                .value(otherwise)
                .member("expectedType", new AnnotationClassValue<>(String.class))
                .member("name", name.isEmpty() ? "" : name + "_OTHERWISE")
                .build());
        }
        remapped.add(AnnotationValue.builder(ELEnvironment.class) // <5>
            .member("functions", AnnotationValue.builder(ELFunctions.class)
                .value(EligibilityFunctions.class)
                .build())
            .member("imports", new AnnotationClassValue<>(Locale.class))
            .build());
        return remapped;
    }

    /**
     * The expression as written, with {@code ${...}} stored as {@code #{...}}: the specification treats the two
     * alike, while Micronaut resolves a {@code ${...}} in the metadata of a bean as a property placeholder.
     */
    private static String textOf(Object value) {
        String text;
        if (value instanceof EvaluatedExpressionReference reference) {
            text = reference.annotationValue().toString(); // <6>
        } else if (value instanceof String string) {
            text = string;
        } else {
            return null;
        }
        text = text.trim();
        if (text.isEmpty()) {
            return null;
        }
        return text.startsWith("${") ? "#" + text.substring(1) : text; // <7>
    }
}
// end::remapper[]
