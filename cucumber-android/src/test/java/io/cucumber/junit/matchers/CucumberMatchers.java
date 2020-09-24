package io.cucumber.junit.matchers;

import org.hamcrest.Matcher;
import org.hamcrest.collection.IsIterableContainingInOrder;

public class CucumberMatchers {
    public static Matcher<Iterable<? extends Class<?>>> containsOnly(final Class<?> type) {
        return IsIterableContainingInOrder.contains(type);
    }
}
