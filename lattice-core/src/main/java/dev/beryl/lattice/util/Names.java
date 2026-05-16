package dev.beryl.lattice.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class Names {
    private static final Pattern ID = Pattern.compile("[a-z][a-z0-9_-]*(\\.[a-z][a-z0-9_-]*)*");

    private Names() {
    }

    public static boolean isId(String value) {
        return value != null && ID.matcher(value).matches();
    }

    public static String normalizeId(String value) {
        return Preconditions.requireText(value, "value").toLowerCase(Locale.ROOT);
    }
}

