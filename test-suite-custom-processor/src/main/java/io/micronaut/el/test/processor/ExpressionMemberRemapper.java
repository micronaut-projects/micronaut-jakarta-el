package io.micronaut.el.test.processor;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.expressions.EvaluatedExpressionReference;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.inject.annotation.AnnotationRemapper;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.VisitorContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The user's processor. Micronaut treats any annotation string containing {@code #{...}} as one of its own
 * evaluated expressions; this remapper takes the members it owns back and hands their expressions to
 * micronaut-expression-language through the public {@link ELExpression} annotation, declared beside the
 * annotation it came from, so that they are compiled at compilation time and found at runtime by their text
 * through the standard {@code jakarta.el.ExpressionFactory}.
 *
 * <p>A remapper runs inside the annotation metadata builder, for every element, before any visitor: there is
 * no ordering to arrange, no visitor kind to match, no metadata to mutate, and a repeatable annotation is
 * presented one value at a time. It returns the annotation to keep in place of the one it was given.</p>
 *
 * <p>Two sets of annotations are handled: the ones carrying {@link JakartaExpLang}, and the constraints of
 * Jakarta Validation, whose {@code message} member is an expression language template by specification.</p>
 */
public final class ExpressionMemberRemapper implements AnnotationRemapper {

    private static final String CONSTRAINT = "jakarta.validation.Constraint";

    @Override
    public String getPackageName() {
        return ALL_PACKAGES;
    }

    @Override
    public List<AnnotationValue<?>> remap(AnnotationValue<?> annotation, VisitorContext visitorContext) {
        ClassElement annotationType = visitorContext.getClassElement(annotation.getAnnotationName()).orElse(null);
        if (annotationType == null) {
            return List.of(annotation);
        }
        List<String> members;
        ClassElement expectedType;
        AnnotationValue<JakartaExpLang> marker = annotationType.getAnnotation(JakartaExpLang.class);
        if (marker != null) {
            members = List.of(marker.stringValues("members"));
            expectedType = marker.annotationClassValue("expectedType")
                .flatMap(value -> visitorContext.getClassElement(value.getName()))
                .orElseGet(() -> ClassElement.of(Object.class));
        } else if (annotationType.hasStereotype(CONSTRAINT)) {
            members = List.of("message");
            expectedType = ClassElement.of(Object.class);
        } else {
            return List.of(annotation);
        }
        if (members.isEmpty()) {
            members = List.of(AnnotationMetadata.VALUE_MEMBER);
        }
        Map<String, String> texts = textsOf(annotation, members);
        if (texts.isEmpty()) {
            return List.of(annotation);
        }
        List<AnnotationValue<?>> remapped = new ArrayList<>();
        // the annotation, with the text exactly as written in place of what Micronaut made of it
        Map<CharSequence, Object> values = new LinkedHashMap<>(annotation.getValues());
        values.putAll(texts);
        remapped.add(new AnnotationValue<>(annotation.getAnnotationName(), values));
        // and every #{...} segment of it, declared for compilation under its exact text
        for (String segment : segmentsOf(texts.values())) {
            remapped.add(AnnotationValue.builder(ELExpression.class)
                .value(segment)
                .member("expectedType", new AnnotationClassValue<>(expectedType.getName()))
                .build());
        }
        return remapped;
    }

    private static int nextSegment(String text, int from) {
        int hash = text.indexOf("#{", from);
        int dollar = text.indexOf("${", from);
        if (hash < 0) {
            return dollar;
        }
        return dollar < 0 ? hash : Math.min(hash, dollar);
    }

    /**
     * The text of the members as written. Micronaut has already replaced a string containing {@code #{...}} by an
     * evaluated expression reference when the remapper runs; the original text is taken back.
     */
    private static Map<String, String> textsOf(AnnotationValue<?> annotation, List<String> members) {
        Map<String, String> texts = new LinkedHashMap<>();
        for (String member : members) {
            Object value = annotation.getValues().get(member);
            if (value instanceof EvaluatedExpressionReference reference) {
                texts.put(member, reference.annotationValue().toString());
            } else if (value instanceof String text && (text.contains("#{") || text.contains("${"))) {
                // a ${...} is not claimed by Micronaut at compilation time, it reaches the remapper as written
                texts.put(member, text);
            }
        }
        return texts;
    }

    /**
     * The {@code #{...}} and {@code ${...}} segments of the texts, as an interpolator scans them: a segment ends at the first
     * unescaped closing brace outside a string literal. The registry is keyed by the text the runtime asks the
     * factory for, so the segments are taken from the text as written.
     */
    private static List<String> segmentsOf(Iterable<String> texts) {
        List<String> segments = new ArrayList<>();
        for (String text : texts) {
            int i = 0;
            while ((i = nextSegment(text, i)) >= 0) {
                int end = -1;
                char quote = 0;
                for (int j = i + 2; j < text.length(); j++) {
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
                        end = j;
                        break;
                    }
                }
                if (end < 0) {
                    break;
                }
                String segment = text.substring(i, end + 1);
                if (!segments.contains(segment)) {
                    segments.add(segment);
                }
                i = end + 1;
            }
        }
        return segments;
    }
}
