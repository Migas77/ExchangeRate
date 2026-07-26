package com.miguelbf.exchangerateapi.utilities;

import org.junit.jupiter.params.provider.Arguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ArgumentCombinations {

    public static Stream<Arguments> allCombinations(List<?>... lists) {
        return Arrays.stream(lists)
            .reduce(
                Stream.of(Collections.emptyList()),
                (combinations, list) -> combinations
                    .flatMap(combo -> list.stream()
                        .map(value -> {
                            List<Object> next = new ArrayList<>(combo);
                            next.add(value);
                            return next;
                        })
                    ),
                Stream::concat
            )
            .map(combo -> Arguments.of(combo.toArray()));
    }

}
