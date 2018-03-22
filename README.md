# Validation

[![Build Status](https://travis-ci.org/h-j-k/validation.svg?branch=master)](https://travis-ci.org/h-j-k/validation)
[![codecov.io](http://codecov.io/github/h-j-k/validation/coverage.svg?branch=master)](http://codecov.io/github/h-j-k/validation?branch=master)
[![Quality Gate](https://sonarcloud.io/api/badges/gate?key=com.ikueb:validation)](https://sonarcloud.io/dashboard/?id=com.ikueb:validation)
[![Technical Debt Ratio](https://sonarcloud.io/api/badges/measure?key=com.ikueb:validation&metric=sqale_debt_ratio)](https://sonarcloud.io/dashboard/?id=com.ikueb:validation)
[![Comments](https://sonarcloud.io/api/badges/measure?key=com.ikueb:validation&metric=comment_lines_density)](https://sonarcloud.io/dashboard/?id=com.ikueb:validation)

A simple validation library using Java 8's `Stream` and `Predicate`.

[Homepage](https://h-j-k.github.io/validation)

[GitHub project page](https://github.com/h-j-k/validation)

[Javadocs](https://h-j-k.github.io/validation/javadoc)

# Motivation

Traditionally, validating a `POJO` before further processing may look something like:

    if (input == null) {
        throw new IllegalStateException();
    }
    if (!correctValue.equals(input.field)) {
        throw new IllegalStateException();
    }
    if (!input.markedForProcessing()) {
        throw new IllegalStateException();
    }
    // ...

Using Java 8's `Predicate` reduces the boilerplate-like code, and arguably more fluent as well:

    T payload = Optional.ofNullable(input)
                        .filter(correctValue.equals(v.field)
                                    && v.markedForProcessing())
                        .orElseThrow(IllegalStateException::new);

The Validator library abstracts out the checks as a series of `Predicate`s instead:

    T payload = Validator.check(input, Validator.notNull(), 
                                        v -> correctValue.equals(v.field),
                                        InputType::markedForProcessing);
                        .orElseThrow(IllegalStateException::new);

Alternatively, custom reasons may be supplied for the thrown `IllegalStateException` for invalidated cases: 

    T payload = Validator.check(input, Arrays.asList(
                                Rule.of(Validator.notNull(), 
                                        "Invalid input."),
                                Rule.of(v -> correctValue.equals(v.field), 
                                        "Incorrect field value."),
                                Rule.of(InputType::markedForProcessing, 
                                        "Not marked for processing."))).get();

# Bugs/feedback

Please make use of the GitHub features to report any bugs, issues, or even pull requests. :)

Enjoy!