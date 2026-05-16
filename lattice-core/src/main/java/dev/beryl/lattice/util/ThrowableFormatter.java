package dev.beryl.lattice.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class ThrowableFormatter {
    private ThrowableFormatter() {
    }

    public static String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}

