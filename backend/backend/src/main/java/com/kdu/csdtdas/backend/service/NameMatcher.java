package com.kdu.csdtdas.backend.service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

final class NameMatcher {

    private static final Set<String> TITLES = Set.of(
            "dr", "prof", "professor", "mr", "mrs", "miss", "ms", "engr", "engineer",
            "pastor", "rev", "chief", "sir", "madam", "alhaji", "alhaja"
    );

    private NameMatcher() {}

    static Set<String> tokens(String name) {
        Set<String> out = new LinkedHashSet<>();
        if (name == null) return out;

        for (String t : name.toLowerCase().replaceAll("[^a-z]+", " ").trim().split("\\s+")) {
            if (!t.isBlank() && !TITLES.contains(t)) out.add(t);
        }
        return out;
    }

    private static Set<String> significant(String name) {
        return tokens(name).stream().filter(t -> t.length() >= 3).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Same words in any order, ignoring titles: "Mr. Olagunju Solomon" equals "Solomon Olagunju". */
    static String normalize(String name) {
        return String.join(" ", new TreeSet<>(tokens(name)));
    }

    static boolean matches(String listed, String fullName) {
        Set<String> a = significant(listed);
        Set<String> b = tokens(fullName);

        if (a.isEmpty() || b.isEmpty()) return false;
        if (b.containsAll(a)) return true;

        Set<String> own = significant(fullName);
        return own.size() >= 2 && tokens(listed).containsAll(own);
    }
}