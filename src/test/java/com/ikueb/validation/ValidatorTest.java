package com.ikueb.validation;

import static com.ikueb.validation.Validator.check;
import static com.ikueb.validation.Validator.filter;
import static com.ikueb.validation.Validator.notNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ValidatorTest {

    private static final Logger logger = LoggerFactory.getLogger(ValidatorTest.class);

    public static interface Widget {
        boolean isActive();
        boolean isReady();
    }

    enum TestWidget implements Widget {
        NULL, INACTIVE, NOT_READY, READY;

        @Override
        public boolean isActive() {
            return this != INACTIVE;
        }

        @Override
        public boolean isReady() {
            return this == READY;
        }

        private Widget get() {
            return this == NULL ? null : this;
        }

        public String getFailureReason() {
            return "Widget is " + name().toLowerCase().replace('_', ' ') + ".";
        }
    }

    private static final Predicate<Widget>[] RULES = new Predicate[] { notNull(),
            w -> ((Widget) w).isActive(), w -> ((Widget) w).isReady() };
    private static final String[] REASONS = EnumSet
            .complementOf(EnumSet.of(TestWidget.READY)).stream()
            .map(TestWidget::getFailureReason).toArray(String[]::new);
    private static final Map<Predicate<Widget>, String> RULE_MAP = IntStream.range(0, 3)
            .collect(LinkedHashMap::new, (m, i) -> m.put(RULES[i], REASONS[i]), Map::putAll);

    private static void assertReady(Widget widget) {
        assertThat(widget, equalTo(TestWidget.READY));
    }

    private static void assertException(IllegalStateException e, String expected) {
        assertThat(e.getMessage(), equalTo(expected));
        logger.debug("Verified for: " + e.getMessage());
    }

    @DataProvider(name = "widget-test-cases")
    public static Iterator<Object[]> getWidgetTestCases() {
        return EnumSet.allOf(TestWidget.class).stream().map(v -> new Object[] { v })
                .iterator();
    }

    @Test(dataProvider = "widget-test-cases")
    public void testNoException(TestWidget widget) {
        check(widget.get(), RULES).ifPresent(ValidatorTest::assertReady);
    }

    @Test(dataProvider = "widget-test-cases")
    public void testDefaultException(TestWidget widget) {
        try {
            check(widget.get(), true, RULES).ifPresent(ValidatorTest::assertReady);
        } catch (IllegalStateException e) {
            String expected = "Validation rule #" + (widget.ordinal() + 1) + " failed.";
            assertException(e, expected);
        }
    }

    @Test(dataProvider = "widget-test-cases")
    public void testException(TestWidget widget) {
        try {
            check(widget.get(), Arrays.asList(RULES), Arrays.asList(REASONS))
                    .ifPresent(ValidatorTest::assertReady);
        } catch (IllegalStateException e) {
            assertException(e, widget.getFailureReason());
        }
    }

    @Test(dataProvider = "widget-test-cases")
    public void testMappedException(TestWidget widget) {
        try {
            check(widget.get(), RULE_MAP).ifPresent(ValidatorTest::assertReady);
        } catch (IllegalStateException e) {
            assertException(e, widget.getFailureReason());
        }
    }

    @Test
    public void testRuleCreation() {
        check(TestWidget.READY, false, Stream.of(RULES).map(r -> Rule.of(r, null))
                .collect(Collectors.toList())).ifPresent(ValidatorTest::assertReady);
    }

    @Test
    public void testFilter() {
        assertThat(filter(EnumSet.allOf(TestWidget.class), HashSet::new,
                notNull(), Widget::isActive, Widget::isReady),
                equalTo(EnumSet.of(TestWidget.READY)));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testListsOfDifferentSizesThrows() {
        check(TestWidget.READY, Arrays.asList(RULES), Collections.emptyList());
    }

    private static final String TEST = " TEST ";
    private static final String OTHER = "Other";

    @DataProvider(name = "string-test-cases")
    public static Iterator<Object[]> getStringTestCases() {
        return Arrays.asList(
                new Object[] { null, OTHER },
                new Object[] { "", OTHER },
                new Object[] { " ", OTHER },
                new Object[] { "\t", OTHER },
                new Object[] { "\n", OTHER },
                new Object[] { "\t\n ", OTHER },
                new Object[] { TEST, TEST.trim() }).iterator();
    }

    @Test(dataProvider="string-test-cases")
    public void testStringCases(String value, String expected) {
        assertThat(Validator.trimStringOr(value, OTHER), equalTo(expected));
    }
}
