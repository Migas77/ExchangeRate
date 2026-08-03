package com.miguelbf.exchangerateapi.utilities;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import java.util.Arrays;
import java.util.List;

public class CustomMatchers {

    private CustomMatchers() {
    }

    public static Matcher<String> containsAllStrings(String... substrings) {
        List<Matcher<? super String>> matchers = Arrays.stream(substrings)
            .<Matcher<? super String>>map(Matchers::containsString)
            .toList();

        return Matchers.allOf(matchers);
    }

}