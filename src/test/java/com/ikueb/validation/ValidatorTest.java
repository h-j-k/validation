package com.ikueb.validation;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ValidatorTest {

    private static final Logger logger = LoggerFactory.getLogger(ValidatorTest.class);

    private static interface Widget {
        boolean isActive();
        boolean isReady();
    }

    private static final Predicate<Widget>[] RULES = Arrays.asList(Validator.notNull(),
            w -> ((Widget) w).isActive(), w -> ((Widget) w).isReady()).toArray(
            new Predicate[0]);

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

        public void verify() {
            Validator.check(this == NULL ? null : this, RULES)
                        .ifPresent(TestWidget::assertReady);
        }

        public void verifyWithDefaultExceptionMessage() {
            try {
                Validator.check(this == NULL ? null : this, true, RULES)
                            .ifPresent(TestWidget::assertReady);
            } catch (IllegalStateException e) {
                assertException(e, "Validation rule #" + (ordinal() + 1) + " failed.");
            }
        }

        public void verifyWithExceptionMessage() {
            try {
                Validator.check(this == NULL ? null : this, Arrays.asList(RULES),
                                EnumSet.complementOf(EnumSet.of(READY)).stream()
                                    .map(Object::toString).collect(Collectors.toList()))
                            .ifPresent(TestWidget::assertReady);
            } catch (IllegalStateException e) {
                assertException(e, toString());
            }
        }

        public void verifyWithMappedExceptionMessage() {
            Map<Predicate<Widget>, String> ruleMap = IntStream.range(0, 3)
                    .collect(LinkedHashMap::new, (map, i) -> map.put(RULES[i],
                            TestWidget.values()[i].toString()), Map::putAll);
            try {
                Validator.check(this == NULL ? null : this, ruleMap)
                            .ifPresent(TestWidget::assertReady);
            } catch (IllegalStateException e) {
                assertException(e, toString());
            }
        }

        private static void assertReady(Widget widget) {
            assertThat(widget, equalTo(READY));
        }

        private static void assertException(IllegalStateException e,
                String expectedMessage) {
            assertThat(e.getMessage(), equalTo(expectedMessage));
            logger.info("Verified for: " + e.getMessage());
        }
    }

    @DataProvider(name = "widget-test-cases")
    public static Iterator<Object[]> getWidgetTestCases() {
        return EnumSet.allOf(TestWidget.class).stream().map(v -> new Object[] { v })
                .iterator();
    }

    @Test(dataProvider = "widget-test-cases")
    public void testNoException(TestWidget widget) {
        widget.verify();
    }

    @Test(dataProvider = "widget-test-cases")
    public void testDefaultException(TestWidget widget) {
        widget.verifyWithDefaultExceptionMessage();
    }

    @Test(dataProvider = "widget-test-cases")
    public void testException(TestWidget widget) {
        widget.verifyWithExceptionMessage();
    }

    @Test(dataProvider = "widget-test-cases")
    public void testMappedException(TestWidget widget) {
        widget.verifyWithMappedExceptionMessage();
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testListsOfDifferentSizesThrows() {
        Validator.check(TestWidget.NULL, Arrays.asList(RULES), Collections.emptyList());
    }

    @Test
    public void testFilter() {
        assertThat(Validator.filter(EnumSet.allOf(TestWidget.class), HashSet::new,
                Validator.notNull(), Widget::isActive, Widget::isReady),
                equalTo(EnumSet.of(TestWidget.READY)));
    }

    private static final String TEST = " TEST ";
    private static final String OTHER = "Other";

    @DataProvider(name = "string-test-cases")
    public static Iterator<Object[]> getStringTestCases() {
        return Arrays.asList(
                new Object[] { null, OTHER },
                new Object[] { "", OTHER },
                new Object[] { " ", OTHER },
                new Object[] { TEST, TEST.trim() }).iterator();
    }

    @Test(dataProvider="string-test-cases")
    public void testStringCases(String value, String expected) {
        assertThat(Validator.trimStringOr(value, OTHER), equalTo(expected));
    }
}
