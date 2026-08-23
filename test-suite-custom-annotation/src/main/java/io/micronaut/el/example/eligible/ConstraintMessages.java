package io.micronaut.el.example.eligible;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.el.CompiledELContext;
import jakarta.el.ELManager;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpolates the message of a constraint as the Bean Validation specification orders it: the attributes of the
 * constraint, {@code {value}}, first; the expressions of the specification next, each found precompiled in the
 * registry under its exact text.
 */
// tag::messages[]
public final class ConstraintMessages {

    private static final Pattern ATTRIBUTE = Pattern.compile("(?<![#$])\\{(\\w+)}");
    // the expressions of a template, up to the first closing brace: good enough for messages
    private static final Pattern EXPRESSION = Pattern.compile("[#$]\\{[^}]*}");

    private ConstraintMessages() {
    }

    public static String interpolate(AnnotationValue<?> constraint, Object validatedValue) {
        String template = constraint.stringValue("message").orElseThrow();
        Matcher attributes = ATTRIBUTE.matcher(template); // <1>
        StringBuilder withAttributes = new StringBuilder();
        while (attributes.find()) {
            Object attribute = constraint.getValues().get(attributes.group(1));
            attributes.appendReplacement(withAttributes, Matcher.quoteReplacement(String.valueOf(attribute)));
        }
        attributes.appendTail(withAttributes);

        CompiledELContext context = new CompiledELContext(); // <2>
        constraint.getValues().forEach((name, value) -> context.setBean(name.toString(), value));
        context.setBean("validatedValue", validatedValue);
        Matcher expressions = EXPRESSION.matcher(withAttributes);
        StringBuilder message = new StringBuilder();
        while (expressions.find()) { // <3>
            String evaluated = ELManager.getExpressionFactory()
                .<String>createValueExpression(context, stored(expressions.group()), String.class)
                .getValue(context);
            expressions.appendReplacement(message, Matcher.quoteReplacement(evaluated));
        }
        return expressions.appendTail(message).toString();
    }

    /**
     * The distinct expressions of a template, as stored: {@code #{...}}, the key of the compiled registry.
     */
    public static List<String> segmentsOf(String template) {
        return EXPRESSION.matcher(template).results()
            .map(segment -> stored(segment.group()))
            .distinct()
            .toList();
    }

    private static String stored(String segment) {
        return "#" + segment.substring(1);
    }
}
// end::messages[]
