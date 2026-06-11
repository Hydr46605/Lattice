package dev.beryl.lattice.update;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SimpleJsonParser {
    private final String input;
    private int index;

    private SimpleJsonParser(String input) {
        this.input = input == null ? "" : input;
    }

    static Object parse(String input) {
        SimpleJsonParser parser = new SimpleJsonParser(input);
        Object value = parser.value();
        parser.whitespace();
        if (!parser.done()) {
            throw parser.error("Unexpected trailing JSON");
        }
        return value;
    }

    private Object value() {
        whitespace();
        if (done()) {
            throw error("Expected JSON value");
        }
        char current = input.charAt(index);
        return switch (current) {
            case '{' -> object();
            case '[' -> array();
            case '"' -> string();
            case 't' -> literal("true", Boolean.TRUE);
            case 'f' -> literal("false", Boolean.FALSE);
            case 'n' -> literal("null", null);
            default -> number();
        };
    }

    private Map<String, Object> object() {
        expect('{');
        Map<String, Object> values = new LinkedHashMap<>();
        whitespace();
        if (peek('}')) {
            expect('}');
            return values;
        }
        while (true) {
            whitespace();
            String key = string();
            whitespace();
            expect(':');
            values.put(key, value());
            whitespace();
            if (peek('}')) {
                expect('}');
                return values;
            }
            expect(',');
        }
    }

    private List<Object> array() {
        expect('[');
        List<Object> values = new ArrayList<>();
        whitespace();
        if (peek(']')) {
            expect(']');
            return values;
        }
        while (true) {
            values.add(value());
            whitespace();
            if (peek(']')) {
                expect(']');
                return values;
            }
            expect(',');
        }
    }

    private String string() {
        expect('"');
        StringBuilder value = new StringBuilder();
        while (!done()) {
            char current = input.charAt(index++);
            if (current == '"') {
                return value.toString();
            }
            if (current != '\\') {
                value.append(current);
                continue;
            }
            if (done()) {
                throw error("Unterminated JSON escape");
            }
            char escaped = input.charAt(index++);
            switch (escaped) {
                case '"', '\\', '/' -> value.append(escaped);
                case 'b' -> value.append('\b');
                case 'f' -> value.append('\f');
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 't' -> value.append('\t');
                case 'u' -> value.append(unicode());
                default -> throw error("Invalid JSON escape");
            }
        }
        throw error("Unterminated JSON string");
    }

    private char unicode() {
        if (index + 4 > input.length()) {
            throw error("Invalid JSON unicode escape");
        }
        String value = input.substring(index, index + 4);
        index += 4;
        try {
            return (char) Integer.parseInt(value, 16);
        } catch (NumberFormatException exception) {
            throw error("Invalid JSON unicode escape");
        }
    }

    private Object number() {
        int start = index;
        if (peek('-')) {
            index++;
        }
        while (!done() && Character.isDigit(input.charAt(index))) {
            index++;
        }
        boolean floating = false;
        if (!done() && input.charAt(index) == '.') {
            floating = true;
            index++;
            while (!done() && Character.isDigit(input.charAt(index))) {
                index++;
            }
        }
        if (!done() && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
            floating = true;
            index++;
            if (!done() && (input.charAt(index) == '+' || input.charAt(index) == '-')) {
                index++;
            }
            while (!done() && Character.isDigit(input.charAt(index))) {
                index++;
            }
        }
        if (start == index) {
            throw error("Expected JSON number");
        }
        String value = input.substring(start, index);
        try {
            return floating ? Double.parseDouble(value) : Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw error("Invalid JSON number");
        }
    }

    private Object literal(String literal, Object value) {
        if (!input.startsWith(literal, index)) {
            throw error("Expected " + literal);
        }
        index += literal.length();
        return value;
    }

    private void whitespace() {
        while (!done()) {
            char current = input.charAt(index);
            if (current != ' ' && current != '\n' && current != '\r' && current != '\t') {
                return;
            }
            index++;
        }
    }

    private boolean peek(char expected) {
        return !done() && input.charAt(index) == expected;
    }

    private void expect(char expected) {
        if (done() || input.charAt(index) != expected) {
            throw error("Expected " + expected);
        }
        index++;
    }

    private boolean done() {
        return index >= input.length();
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at offset " + index);
    }
}
