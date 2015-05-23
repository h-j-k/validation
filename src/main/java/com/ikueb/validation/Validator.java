package com.ikueb.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * An utility class that validates a given value against {@link Predicate} 'rules' and
 * wrapping it in an {@link Optional}. Invalidated values are either returned as
 * {@link Optional#empty()}, or an {@link IllegalStateException} may be thrown by
 * preference.
 */
public final class Validator {

    private Validator() {
        // empty
    }

    /**
     * @return a {@link Predicate} that test for a non-{@code null} value.
     */
    public static final <T> Predicate<T> notNull() {
        return o -> o != null;
    }

    /**
     * Validates if a {@link String} is not null and not empty when trimmed.
     *
     * @param value the value to validate
     * @param other the value to use in case of invalidation
     * @return the original {@link String} trimmed, or the other value
     */
    public static String trimStringOr(String value, String other) {
        return check(value, notNull(), s -> !s.trim().isEmpty())
                .map(String::trim).orElse(other);
    }

    /**
     * Validates if the value satisfies all {@link Predicate}s.
     *
     * @param value the value to validate
     * @param predicates the {@link Predicate}s to use
     * @return an {@link Optional} wrapper over the value
     */
    @SafeVarargs
    public static <T> Optional<T> check(T value, Predicate<T>... predicates) {
        return check(value, false, predicates);
    }

    /**
     * Validates if the value satisfies all {@link Predicate}s.
     *
     * @param value the value to validate
     * @param throwException {@code true} to throw an {@link IllegalArgumentException}
     *            instead for validation failure
     * @param predicates the {@link Predicate}s to use
     * @return an {@link Optional} wrapper over the value
     * @throws IllegalStateException with the message
     *             {@code "Validation rule #N failed"}, where {@code N} is the index
     *             (1-based) of the failed {@link Predicate}, when the validation fails
     */
    @SafeVarargs
    public static <T> Optional<T> check(T value, boolean throwException,
            Predicate<T>... predicates) {
        Objects.requireNonNull(predicates);
        return check(value, throwException, toRules(predicates.length, (rules, i) ->
            rules.add(Rule.of(predicates[i], i + 1))));
    }

    /**
     * Validates if the value satisfies all {@link Predicate}s.
     *
     * @param value the value to validate
     * @param predicates the {@link Predicate}s to use
     * @param reasons the the coresponding reasons to use
     * @return an {@link Optional} wrapper over the value
     * @throws IllegalStateException with the message given by the corresponding reason
     *             supplied, when the validation fails
     */
    public static <T> Optional<T> check(T value, List<Predicate<T>> predicates,
            List<String> reasons) {
        Objects.requireNonNull(predicates);
        Objects.requireNonNull(reasons);
        if (predicates.size() != reasons.size()) {
            throw new IllegalArgumentException("Predicate and reason counts do not match.");
        }
        return check(value, true, toRules(predicates.size(), (rules, i) ->
                rules.add(Rule.of(predicates.get(i), reasons.get(i)))));
    }

    /**
     * Validates if the value satisfies all {@link Predicate} keys in the given
     * {@link Map}. The ordering of the checks is determined by that of
     * {@link Map#entrySet()}, hence it is recommended that the implementation has a
     * predictable iteration order.
     *
     * @param value the value to validate
     * @param validationMap a {@link Map} of {@link Predicate}s to failure reasons
     * @return an {@link Optional} wrapper over the value
     * @throws IllegalStateException with the message given by the corresponding reason
     *             supplied, when the validation fails
     */
    public static <T> Optional<T> check(T value,
            Map<Predicate<T>, String> validationMap) {
        Objects.requireNonNull(validationMap);
        return check(value, true, validationMap.entrySet().stream()
                        .map(entry -> Rule.of(entry.getKey(), entry.getValue()))
                        .collect(Collectors.toList()));
    }

    /**
     * Validates on a {@link Collection} of values to return the subset of it that are
     * successfully validated.
     *
     * @param values the values to validate
     * @param supplier the supplier for the resulting {@link Collection}
     * @param predicates the {@link Predicate}s to use
     * @return a {@link Collection} of validated values
     */
    public static <T> Collection<T> filter(Collection<T> values,
            Supplier<Collection<T>> supplier, Predicate<T>... predicates) {
        Objects.requireNonNull(values);
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(predicates);
        List<Rule<T>> list = toRules(predicates.length,
                (rules, i) -> rules.add(Rule.of(predicates[i], null)));
        return values.stream().map(v -> check(v, false, list))
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toCollection(supplier));
    }

    /**
     * Validates if the value satisfies all {@link Rule} rules.
     *
     * @param value the value to validate
     * @param throwException {@code true} to throw an {@link IllegalArgumentException}
     *            instead for validation failure
     * @param rules the {@link Rule}s to validate with
     * @return an {@link Optional} wrapper over the value
     * @throws IllegalStateException with the message given by the corresponding reason
     *             supplied, when the validation fails
     */
    private static <T> Optional<T> check(T value, boolean throwException,
            List<Rule<T>> rules) {
        Objects.requireNonNull(rules);
        Optional<Rule<T>> failed = rules.stream()
                .filter(rule -> rule.test(value)).findFirst();
        if (failed.isPresent() && throwException) {
            throw new IllegalStateException(Objects.toString(failed.get(),
                    "Validation failed."));
        }
        return failed.isPresent() ? Optional.empty() : Optional.of(value);
    }

    /**
     * @param endExclusive the exclusive upper bound to
     *            {@link IntStream#range(int, int)}
     * @param accumulator the accumulator to use
     * @return a {@link List} of {@link Rule}s
     */
    private static <T> List<Rule<T>> toRules(int endExclusive,
            ObjIntConsumer<List<Rule<T>>> accumulator) {
        return IntStream.range(0, endExclusive).collect(ArrayList::new,
                accumulator, List::addAll);
    }
}
