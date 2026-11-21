package com.freightfox.dispatch.service;

import com.freightfox.dispatch.exception.InvalidRequestException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

/** Rejects a batch that contains the same ID more than once (ambiguous: which one should win?). */
final class DuplicateIdValidator {

    private DuplicateIdValidator() {
    }

    static <T> void ensureUnique(List<T> items, Function<T, String> idExtractor, String idName) {
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new TreeSet<>();
        for (T item : items) {
            String id = idExtractor.apply(item).trim();
            if (!seen.add(id)) {
                duplicates.add(id);
            }
        }
        if (!duplicates.isEmpty()) {
            throw new InvalidRequestException("Duplicate " + idName + " values in request",
                    duplicates.stream().map(id -> idName + " '" + id + "' appears more than once").toList());
        }
    }
}
