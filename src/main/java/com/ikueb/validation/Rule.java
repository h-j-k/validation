package com.ikueb.validation;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Encapsulation of a success case and the failure reason when {@link #test(Object)}
 * fails.
 *
 * @param <T> the requested type
 */
public final class Rule<T> implements Predicate<T> {
    private static final String FAIL_REASON = "Validation rule #%d failed.";
    private final Predicate<T> successCase;
    private final String failureReason;

    private Rule(Predicate<T> successCase, String failureReason) {
        this.successCase = Objects.requireNonNull(successCase);
        this.failureReason = failureReason;
    }

    static <T> Rule<T> of(Predicate<T> successCase, int index) {
        return of(successCase, String.format(FAIL_REASON, Integer.valueOf(index)));
    }

    static <T> Rule<T> of(Predicate<T> successCase,
            String failureReason) {
        return new Rule<>(successCase, failureReason);
    }

    @Override
    public boolean test(T t) {
        return successCase.negate().test(t);
    }

    @Override
    public String toString() {
        return failureReason;
    }
}
