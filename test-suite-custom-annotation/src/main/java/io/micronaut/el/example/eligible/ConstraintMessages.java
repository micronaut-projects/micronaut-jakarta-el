package io.micronaut.el.example.eligible;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.el.CompiledELContext;
import jakarta.el.ELManager;

import java.util.ArrayList;
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
        String message = withAttributes.toString();
        for (String segment : segmentsOf(message)) { // <3>
            String evaluated = ELManager.getExpressionFactory().createValueExpression(context, segment, String.class).getValue(context);
            message = message.replace(segment, evaluated);
        }
        return message;
    }

    /**
     * The {@code ${...}} and {@code #{...}} segments of the template, stored as {@code #{...}}, as an interpolator
     * scans them: up to the first closing brace outside a string literal.
     */
    public static List<String> segmentsOf(String text) {
        List<String> segments = new ArrayList<>();
        int i = 0;
        while ((i = nextSegment(text, i)) >= 0) {
            int end = endOfSegment(text, i + 2);
            if (end < 0) {
                break;
            }
            String segment = "#" + text.substring(i + 1, end + 1);
            if (!segments.contains(segment)) {
                segments.add(segment);
            }
            i = end + 1;
        }
        return segments;
    }

    private static int nextSegment(String text, int from) {
        int hash = text.indexOf("#{", from);
        int dollar = text.indexOf("${", from);
        if (hash < 0) {
            return dollar;
        }
        return dollar < 0 ? hash : Math.min(hash, dollar);
    }

    private static int endOfSegment(String text, int from) {
        char quote = 0;
        for (int j = from; j < text.length(); j++) {
            char c = text.charAt(j);
            if (c == '\\' && j + 1 < text.length()) {
                j++;
            } else if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
            } else if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '}') {
                return j;
            }
        }
        return -1;
    }
}
// end::messages[]
