package com.antioxidant;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    private Json() {}

    public static Object parse(String text) {
        Parser parser = new Parser(text);
        Object value = parser.value();
        parser.space();
        if (!parser.end()) throw new IllegalArgumentException("Trailing JSON data at character " + parser.index);
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> object(Object value) {
        return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> array(Object value) {
        return value instanceof List<?> ? (List<Object>) value : List.of();
    }

    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out, 0);
        return out.append('\n').toString();
    }

    private static void write(Object value, StringBuilder out, int depth) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String string) {
            quote(string, out);
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            if (!map.isEmpty()) {
                boolean first = true;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!first) out.append(',');
                    out.append('\n');
                    indent(out, depth + 1);
                    quote(String.valueOf(entry.getKey()), out);
                    out.append(": ");
                    write(entry.getValue(), out, depth + 1);
                    first = false;
                }
                out.append('\n');
                indent(out, depth);
            }
            out.append('}');
        } else if (value instanceof Iterable<?> values) {
            out.append('[');
            boolean first = true;
            for (Object item : values) {
                if (!first) out.append(',');
                out.append('\n');
                indent(out, depth + 1);
                write(item, out, depth + 1);
                first = false;
            }
            if (!first) {
                out.append('\n');
                indent(out, depth);
            }
            out.append(']');
        } else {
            quote(String.valueOf(value), out);
        }
    }

    private static void quote(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        out.append('"');
    }

    private static void indent(StringBuilder out, int depth) {
        out.append("  ".repeat(depth));
    }

    private static final class Parser {
        private final String text;
        private int index;

        private Parser(String text) {
            this.text = text;
        }

        private Object value() {
            space();
            if (end()) throw error("Expected a value");
            return switch (text.charAt(index)) {
                case '{' -> objectValue();
                case '[' -> arrayValue();
                case '"' -> stringValue();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> numberValue();
            };
        }

        private Map<String, Object> objectValue() {
            index++;
            Map<String, Object> result = new LinkedHashMap<>();
            space();
            if (take('}')) return result;
            while (true) {
                space();
                if (end() || text.charAt(index) != '"') throw error("Expected an object key");
                String key = stringValue();
                space();
                require(':');
                result.put(key, value());
                space();
                if (take('}')) return result;
                require(',');
            }
        }

        private List<Object> arrayValue() {
            index++;
            List<Object> result = new ArrayList<>();
            space();
            if (take(']')) return result;
            while (true) {
                result.add(value());
                space();
                if (take(']')) return result;
                require(',');
            }
        }

        private String stringValue() {
            require('"');
            StringBuilder result = new StringBuilder();
            while (!end()) {
                char c = text.charAt(index++);
                if (c == '"') return result.toString();
                if (c != '\\') {
                    if (c < 0x20) throw error("Control character in string");
                    result.append(c);
                    continue;
                }
                if (end()) throw error("Incomplete escape sequence");
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> result.append(escaped);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> {
                        if (index + 4 > text.length()) throw error("Incomplete unicode escape");
                        try {
                            result.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
                        } catch (NumberFormatException exception) {
                            throw error("Invalid unicode escape");
                        }
                        index += 4;
                    }
                    default -> throw error("Invalid escape sequence");
                }
            }
            throw error("Unterminated string");
        }

        private Object numberValue() {
            int start = index;
            if (take('-') && end()) throw error("Invalid number");
            if (take('0')) {
                if (!end() && Character.isDigit(text.charAt(index))) throw error("Leading zero in number");
            } else {
                digits();
            }
            boolean decimal = false;
            if (take('.')) {
                decimal = true;
                digits();
            }
            if (take('e') || take('E')) {
                decimal = true;
                if (!take('+')) take('-');
                digits();
            }
            String number = text.substring(start, index);
            try {
                return decimal ? Double.parseDouble(number) : Long.parseLong(number);
            } catch (NumberFormatException exception) {
                throw error("Invalid number");
            }
        }

        private void digits() {
            int start = index;
            while (!end() && Character.isDigit(text.charAt(index))) index++;
            if (start == index) throw error("Expected a digit");
        }

        private Object literal(String expected, Object value) {
            if (!text.startsWith(expected, index)) throw error("Invalid token");
            index += expected.length();
            return value;
        }

        private void space() {
            while (!end() && Character.isWhitespace(text.charAt(index))) index++;
        }

        private boolean take(char expected) {
            if (!end() && text.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void require(char expected) {
            if (!take(expected)) throw error("Expected '" + expected + "'");
        }

        private boolean end() {
            return index >= text.length();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at character " + index);
        }
    }
}