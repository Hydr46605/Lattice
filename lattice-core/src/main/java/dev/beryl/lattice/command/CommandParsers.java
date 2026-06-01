package dev.beryl.lattice.command;

import java.util.Locale;

final class CommandParsers {
    private CommandParsers() {
    }

    static <T> CommandArgumentParser<T> defaultParser(Class<T> type) {
        return input -> cast(parse(type, input));
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object value) {
        return (T) value;
    }

    private static Object parse(Class<?> type, String input) {
        if (type == String.class) {
            return input;
        }
        if (type == Integer.class || type == int.class) {
            return Integer.parseInt(input);
        }
        if (type == Long.class || type == long.class) {
            return Long.parseLong(input);
        }
        if (type == Double.class || type == double.class) {
            return Double.parseDouble(input);
        }
        if (type == Float.class || type == float.class) {
            return Float.parseFloat(input);
        }
        if (type == Boolean.class || type == boolean.class) {
            return parseBoolean(input);
        }
        throw new UnsupportedArgumentTypeException(type);
    }

    private static boolean parseBoolean(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "true", "yes", "on", "1" -> true;
            case "false", "no", "off", "0" -> false;
            default -> throw new InvalidBooleanArgumentException();
        };
    }

    static final class UnsupportedArgumentTypeException extends RuntimeException {
        private final Class<?> type;

        UnsupportedArgumentTypeException(Class<?> type) {
            this.type = type;
        }

        Class<?> type() {
            return type;
        }
    }

    static final class InvalidBooleanArgumentException extends RuntimeException {
    }
}
