package com.androiddjibridge.core;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Deterministic, strict JSON codec for the app-private mapping store. */
public final class MappingJsonCodec {
    private MappingJsonCodec() {
    }

    public static String toJson(MappingStore store) {
        if (store == null) {
            throw new IllegalArgumentException("mapping store is required");
        }
        StringBuilder out = new StringBuilder(512);
        out.append("{\"schemaVersion\":")
                .append(store.getSchemaVersion())
                .append(",\"mappings\":{");
        int index = 0;
        for (ControllerTrigger trigger : ControllerTrigger.values()) {
            if (index++ > 0) {
                out.append(',');
            }
            out.append(quote(trigger.name())).append(':')
                    .append(quote(store.actionFor(trigger).name()));
        }
        return out.append("}}").toString();
    }

    public static MappingStore fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("mapping JSON is required");
        }
        Object value = new Parser(json).parseDocument();
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("mapping JSON must be an object");
        }
        Map<?, ?> root = (Map<?, ?>) value;
        for (Object key : root.keySet()) {
            if (!"schemaVersion".equals(key) && !"mappings".equals(key)) {
                throw new IllegalArgumentException("unknown mapping field " + key);
            }
        }
        Object schema = root.get("schemaVersion");
        if (!(schema instanceof Long)) {
            throw new IllegalArgumentException("schemaVersion must be an integer");
        }
        long schemaNumber = (Long) schema;
        if (schemaNumber < Integer.MIN_VALUE || schemaNumber > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("schemaVersion is out of range");
        }
        int schemaVersion = (int) schemaNumber;
        if (schemaVersion != MappingStore.CURRENT_SCHEMA_VERSION) {
            throw new UnsupportedSchemaException(schemaVersion);
        }
        Object rawMappings = root.get("mappings");
        if (!(rawMappings instanceof Map)) {
            throw new IllegalArgumentException("mappings must be an object");
        }
        EnumMap<ControllerTrigger, CameraAction> mappings =
                new EnumMap<>(ControllerTrigger.class);
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) rawMappings).entrySet()) {
            if (!(entry.getKey() instanceof String)
                    || !(entry.getValue() instanceof String)) {
                throw new IllegalArgumentException("mapping entries must be strings");
            }
            try {
                mappings.put(
                        ControllerTrigger.valueOf((String) entry.getKey()),
                        CameraAction.valueOf((String) entry.getValue()));
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("unsupported trigger or action", error);
            }
        }
        return MappingStore.fromMap(schemaVersion, mappings);
    }

    public static final class UnsupportedSchemaException extends IllegalArgumentException {
        public UnsupportedSchemaException(int schemaVersion) {
            super("unsupported mapping schema " + schemaVersion);
        }
    }

    private static String quote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                out.append('\\');
            }
            out.append(c);
        }
        return out.append('"').toString();
    }

    private static final class Parser {
        private final String input;
        private int index;

        private Parser(String input) {
            this.input = input;
        }

        private Object parseDocument() {
            skipWhitespace();
            Object value = parseObject();
            skipWhitespace();
            if (index != input.length()) {
                throw error("trailing JSON data");
            }
            return value;
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (consume('}')) {
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                if (result.containsKey(key)) {
                    throw error("duplicate JSON field " + key);
                }
                skipWhitespace();
                expect(':');
                skipWhitespace();
                result.put(key, input.charAt(index) == '{'
                        ? parseObject() : parseScalar());
                skipWhitespace();
                if (consume('}')) {
                    return result;
                }
                expect(',');
            }
        }

        private Object parseScalar() {
            if (index < input.length() && input.charAt(index) == '"') {
                return parseString();
            }
            int start = index;
            if (index < input.length() && input.charAt(index) == '-') {
                index++;
            }
            while (index < input.length() && Character.isDigit(input.charAt(index))) {
                index++;
            }
            if (start == index || (input.charAt(start) == '-' && start + 1 == index)) {
                throw error("invalid scalar");
            }
            try {
                return Long.valueOf(input.substring(start, index));
            } catch (NumberFormatException error) {
                throw error("invalid integer");
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (index < input.length()) {
                char c = input.charAt(index++);
                if (c == '"') {
                    return result.toString();
                }
                if (c == '\\' || c < 0x20) {
                    throw error("unsupported string escape");
                }
                result.append(c);
            }
            throw error("unterminated string");
        }

        private boolean consume(char value) {
            if (index < input.length() && input.charAt(index) == value) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char value) {
            if (!consume(value)) {
                throw error("expected " + value);
            }
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " at JSON index " + index);
        }
    }
}
