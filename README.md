# Validation

A simple validation library using Java 8's `Stream` and `Predicate`.

Motivation
---

Traditionally, validating a `POJO` before further processing may look something like:

    if (input == null) {
        return;
    }
    if (!correctValue.equals(input.field)) {
        throw new IllegalStateException("Incorrect field value.");
    }
    if (!input.markedForProcessing()) {
        // ...
    }
    // ...

Using Java 8's `Predicate` reduces the boilerplate-like code, and arguably more fluent as well:

    T payload = Optional.of(input).filter(v -> v != null
                                            && correctValue.equals(v.field)
                                            && v.markedForProcessing())
                        .orElseThrow(IllegalStateException::new);

The Validator library abstracts out the checks as a series of `Predicate`s instead:

    T payload = Validator.check(input, Validator.notNull(), 
                                        v -> correctValue.equals(v.field),
                                        InputType::markedForProcessing);
                        .orElseThrow(IllegalStateException::new);

Alternatively, custom reasons may be supplied for the thrown `IllegalStateException` for invalidated cases: 

    T payload = Validator.check(input, 
                                Arrays.asList(Rule.of(Validator.notNull(), "Invalid input."),
                                    Rule.of(v -> correctValue.equals(v.field), "Incorrect field value."),
                                    Rule.of(InputType::markedForProcessing, "Not marked for processing."))).get();

Bugs/feedback
---

Please make use of the GitHub features to report any bugs, issues, or even pull requests. :)

Enjoy!