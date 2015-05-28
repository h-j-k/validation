/*
 * Copyright 2015 h-j-k. All Rights Reserved.
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

import static com.ikueb.validation.Validator.check;
import static com.ikueb.validation.Validator.filter;
import static com.ikueb.validation.Validator.notNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.testng.Assert.assertFalse;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
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
        NULL(notNull()),
        INACTIVE(Widget::isActive),
        NOT_READY(Widget::isReady),
        READY(w -> true);

        private final Predicate<Widget> test;

        private TestWidget(Predicate<Widget> test) {
            this.test = test;
        }

        @Override
        public boolean isActive() {
            return this != INACTIVE;
        }

        @Override
        public boolean isReady() {
            return this == READY;
        }

        public Widget get() {
            return this == NULL ? null : this;
        }

        public Predicate<Widget> test() {
            return test;
        }

        public String failureReason() {
            return "Widget is " + name().toLowerCase().replace('_', ' ') + ".";
        }
    }

    private static final Predicate<Widget>[] RULES = get(TestWidget::test, Predicate[]::new);
    private static final String[] REASONS = get(TestWidget::failureReason, String[]::new);
    private static final Map<Predicate<Widget>, String> RULE_MAP = IntStream.range(0, 3)
            .collect(LinkedHashMap::new, (m, i) -> m.put(RULES[i], REASONS[i]), Map::putAll);
    private static final List<Trigger<Widget>> RULE_LIST = Stream.of(RULES)
            .map(r -> Trigger.of(r, "")).collect(Collectors.toList());

    private static <R> R[] get(Function<TestWidget, R> mapper, IntFunction<R[]> generator) {
        return EnumSet.complementOf(EnumSet.of(TestWidget.READY))
                .stream().map(mapper).toArray(generator);
    }

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
    public void testSimpleUsage(TestWidget widget) {
        check(widget.get(), RULES).ifPresent(ValidatorTest::assertReady);
    }

    @Test(dataProvider = "widget-test-cases")
    public void testNoException(TestWidget widget) {
        check(widget.get(), false, RULES).ifPresent(ValidatorTest::assertReady);
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
            assertException(e, widget.failureReason());
        }
    }

    @Test(dataProvider = "widget-test-cases")
    public void testMappedException(TestWidget widget) {
        try {
            check(widget.get(), RULE_MAP).ifPresent(ValidatorTest::assertReady);
        } catch (IllegalStateException e) {
            assertException(e, widget.failureReason());
        }
    }

    @Test
    public void testRuleCreation() {
        check(TestWidget.READY, RULE_LIST).ifPresent(ValidatorTest::assertReady);
    }

    @Test
    public void testFilter() {
        assertThat(filter(EnumSet.allOf(TestWidget.class), HashSet::new,
                notNull(), Widget::isActive, Widget::isReady),
                equalTo(EnumSet.of(TestWidget.READY)));
    }

    @Test
    public void testOppositeRules() {
        assertFalse(check(null, v -> v == null, notNull()).isPresent());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testListsOfDifferentSizesThrows() {
        check(TestWidget.READY, Collections.singletonList(notNull()),
                Collections.emptyList());
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
