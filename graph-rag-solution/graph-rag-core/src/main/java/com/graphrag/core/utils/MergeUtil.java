package com.graphrag.core.utils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility helpers to merge and deduplicate lists while preserving order.
 */
public class MergeUtil {

    public static <T> List<T> merge(int limit, List<T>... lists) {
        return Stream.of(lists)
                .flatMap(List::stream)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }
}