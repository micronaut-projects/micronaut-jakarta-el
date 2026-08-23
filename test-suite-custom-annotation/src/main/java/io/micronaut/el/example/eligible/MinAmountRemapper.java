package io.micronaut.el.example.eligible;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELVariable;
import io.micronaut.inject.annotation.AnnotationRemapper;
import io.micronaut.inject.visitor.VisitorContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The annotation processor of {@link MinAmount}: every {@code ${...}} segment of the message is compiled, with
 * the attributes of the constraint and the validated value as typed variables.
 */
// tag::remapper[]
public final class MinAmountRemapper implements AnnotationRemapper {

    @Override
    public String getPackageName() {
        return MinAmount.class.getPackageName();
    }

    @Override
    public List<AnnotationValue<?>> remap(AnnotationValue<?> annotation, VisitorContext visitorContext) {
        if (!annotation.getAnnotationName().equals(MinAmount.class.getName())) {
            return List.of(annotation);
        }
        String message = textOf(annotation.getValues().get("message"));
        if (message == null) {
            return List.of(annotation);
        }
        Map<CharSequence, Object> values = new LinkedHashMap<>(annotation.getValues());
        values.put("message", message);
        List<AnnotationValue<?>> remapped = new ArrayList<>();
        remapped.add(new AnnotationValue<>(MinAmount.class.getName(), values));
        for (String segment : ConstraintMessages.segmentsOf(message)) { // <1>
            remapped.add(AnnotationValue.builder(ELExpression.class)
                .value(segment)
                .member("expectedType", new AnnotationClassValue<>(String.class))
                .build());
        }
        remapped.add(AnnotationValue.builder(ELEnvironment.class) // <2>
            .member("variables",
                AnnotationValue.builder(ELVariable.class).member("name", "value").member("type", new AnnotationClassValue<>(Long.class)).build(),
                AnnotationValue.builder(ELVariable.class).member("name", "inclusive").member("type", new AnnotationClassValue<>(Boolean.class)).build(),
                AnnotationValue.builder(ELVariable.class).member("name", "validatedValue").member("type", new AnnotationClassValue<>(Long.class)).build())
            .build());
        return remapped;
    }

    private static String textOf(Object value) {
        if (value instanceof String text) {
            // stored with ${...} as #{...}, which nothing in Micronaut resolves as a property placeholder;
            // a message template uses ${...}, so, unlike a whole-expression member, it never reaches the
            // metadata builder as an evaluated expression reference
            return text.replace("${", "#{");
        }
        return null;
    }


}
// end::remapper[]
