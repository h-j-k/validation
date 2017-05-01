/*
 * Copyright 2017 h-j-k. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ikueb.validation;

import java.util.*;
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
     * Validates if the value satisfies all {@link Predicate}s. If there are no {@link Predicate}s,
     * this returns an {@link Optional} of the input {@code value}, due to the property of vacuous truth
     * (all {@link Predicate}s satisfies the value).
     *
     * @param value      the value to validate
     * @param predicates the {@link Predicate}s to use
     * @return an {@link Optional} wrapper over the value
     */
    @SafeVarargs
    public static <T> Optional<T> check(T value, Predicate<T>... predicates) {
        return Optional.ofNullable(value).filter(Arrays.stream(predicates)
                .filter(Objects::nonNull)
                .reduce((a, b) -> a.and(b)).orElse(t -> true));
    }

    /**
     * Validates if the value satisfies all {@link Predicate}s.
     *
     * @param value          the value to validate
     * @param throwException {@code true} to throw an {@link IllegalArgumentException}
     *                       instead for validation failure
     * @param predicates     the {@link Predicate}s to use
     * @return an {@link Optional} wrapper over the value
     * @throws IllegalStateException with the message
     *                               {@code "Validation rule #N failed"}, where {@code N} is the index
     *                               (1-based) of the failed {@link Predicate}, when the validation fails
     */
    @SafeVarargs
    public static <T> Optional<T> check(T value, boolean throwException,
                                        Predicate<T>... predicates) {
        Objects.requireNonNull(predicates);
        if (!throwException) {
            return check(value, predicates);
        }
        return check(value, toRules(predicates.length, (rules, i) ->
                rules.add(Trigger.of(predicates[i], i + 1))));
    }

    /**
     * Validates if the value satisfies all {@link Predicate}s.
     *
     * @param value      the value to validate
     * @param predicates the {@link Predicate}s to use
     * @param reasons    the the coresponding reasons to use
     * @return an {@link Optional} wrapper over the value
     * @throws IllegalStateException with the message given by the corresponding reason
     *                               supplied, when the validation fails
     */
    public static <T> Optional<T> check(T value, List<Predicate<T>> predicates,
                                        List<String> reasons) {
        Objects.requireNonNull(predicates);
        Objects.requireNonNull(reasons);
        if (predicates.size() != reasons.size()) {
            throw new IllegalArgumentException("Predicate and reason counts do not match.");
        }
        return check(value, toRules(predicates.size(), (rules, i) ->
                rules.add(Trigger.of(predicates.get(i), reasons.get(i)))));
    }

    /**
     * Validates if the value satisfies all {@link Predicate} keys in the given
     * {@link Map}. The ordering of the checks is determined by that of
     * {@link Map#entrySet()}, hence it is recommended that the implementation has a
     * predictable iteration order.
     *
     * @param value         the value to validate
     * @param validationMap a {@link Map} of {@link Predicate}s to failure reasons
     * @return an {@link Optional} wrapper over the value
     * @throws IllegalStateException with the message given by the corresponding reason
     *                               supplied, when the validation fails
     */
    public static <T> Optional<T> check(T value,
                                        Map<Predicate<T>, String> validationMap) {
        Objects.requireNonNull(validationMap);
        return check(value, validationMap.entrySet().stream()
                .map(entry -> Trigger.of(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
    }

    /**
     * Validates if the value satisfies all {@link Trigger} rules.
     *
     * @param value the value to validate
     * @param rules the {@link Trigger}s to validate with
     * @return an {@link Optional} wrapper over the value
     * @throws IllegalStateException with the message given by the corresponding reason
     *                               supplied, when the validation fails
     */
    public static <T> Optional<T> check(T value, List<Trigger<T>> rules) {
        Objects.requireNonNull(rules);
        Optional<Trigger<T>> failed = rules.stream()
                .filter(rule -> rule.test(value)).findFirst();
        if (failed.isPresent()) {
            throw new IllegalStateException(Objects.toString(failed.get(),
                    "Validation failed."));
        }
        return Optional.of(value);
    }

    /**
     * Validates on a {@link Collection} of values to return the subset of it that are
     * successfully validated. If there are no {@link Predicate}s, this returns an {@link Optional}
     * of the input {@code value}, due to the property of vacuous truth
     * (all {@link Predicate}s satisfies the value).
     *
     * @param values     the values to validate
     * @param supplier   the supplier for the resulting {@link Collection}
     * @param predicates the {@link Predicate}s to use
     * @return a {@link Collection} of validated values
     */
    @SafeVarargs
    public static <T> Collection<T> filter(Collection<T> values,
                                           Supplier<Collection<T>> supplier, Predicate<T>... predicates) {
        Objects.requireNonNull(values);
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(predicates);
        return values.stream()
                .filter(Arrays.stream(predicates)
                        .filter(Objects::nonNull)
                        .reduce((a, b) -> a.and(b)).orElse(t -> true))
                .collect(Collectors.toCollection(supplier));
    }

    /**
     * @param endExclusive the exclusive upper bound to
     *                     {@link IntStream#range(int, int)}
     * @param accumulator  the accumulator to use
     * @return a {@link List} of {@link Trigger}s
     */
    private static <T> List<Trigger<T>> toRules(int endExclusive,
                                                ObjIntConsumer<List<Trigger<T>>> accumulator) {
        return IntStream.range(0, endExclusive).collect(ArrayList::new,
                accumulator, List::addAll);
    }
}
