package com.androiddjibridge.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic, data-only JSON codec for versioned camera profiles. */
public final class CameraProfileJsonCodec {
    private static final Set<String> KNOWN_FIELDS = new HashSet<>(Arrays.asList(
            "schemaVersion",
            "stableProfileId",
            "displayName",
            "packageName",
            "recordButtonResourceId",
            "recordButtonClassName",
            "recordButtonText",
            "allowedContentDescriptions",
            "allowedContentDescriptionPatterns",
            "parentResourceId",
            "parentClassName",
            "requiredChildResourceIds",
            "requiredAccessibilityActions",
            "stateResourceId",
            "recordingStateValues",
            "standbyStateValues",
            "durationResourceId",
            "stateVerificationSupported",
            "capabilities",
            "origin",
            "validationStatus",
            "notes"));

    private CameraProfileJsonCodec() {
    }

    public static String toJson(CameraProfile profile) {
        if (profile == null) {
            throw new IllegalArgumentException("profile is required");
        }
        StringBuilder out = new StringBuilder(1024);
        out.append('{');
        field(out, "schemaVersion", Integer.toString(profile.getSchemaVersion()), false);
        field(out, "stableProfileId", quote(profile.getStableId()), true);
        field(out, "displayName", quote(profile.getDisplayName()), true);
        field(out, "packageName", quote(profile.getPackageName()), true);
        field(out, "recordButtonResourceId", nullableString(profile.getRecordButtonResourceId()), true);
        field(out, "recordButtonClassName", nullableString(profile.getRecordButtonClassName()), true);
        field(out, "recordButtonText", nullableString(profile.getRecordButtonText()), true);
        field(out, "allowedContentDescriptions",
                stringArray(profile.getAllowedContentDescriptions()), true);
        field(out, "allowedContentDescriptionPatterns",
                stringArray(profile.getAllowedContentDescriptionPatterns()), true);
        field(out, "parentResourceId", nullableString(profile.getParentResourceId()), true);
        field(out, "parentClassName", nullableString(profile.getParentClassName()), true);
        field(out, "requiredChildResourceIds",
                stringArray(profile.getRequiredChildResourceIds()), true);
        field(out, "requiredAccessibilityActions",
                enumArray(profile.getRequiredAccessibilityActions()), true);
        field(out, "stateResourceId", nullableString(profile.getStateResourceId()), true);
        field(out, "recordingStateValues",
                stringArray(profile.getRecordingStateValues()), true);
        field(out, "standbyStateValues",
                stringArray(profile.getStandbyStateValues()), true);
        field(out, "durationResourceId", nullableString(profile.getDurationResourceId()), true);
        field(out, "stateVerificationSupported",
                Boolean.toString(profile.isStateVerificationSupported()), true);
        field(out, "capabilities", enumArray(profile.getCapabilities()), true);
        field(out, "origin", quote(profile.getOrigin().name()), true);
        field(out, "validationStatus", quote(profile.getValidationStatus().name()), true);
        field(out, "notes", quote(profile.getNotes()), true);
        out.append('}');
        return out.toString();
    }

    public static CameraProfile fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("profile JSON is required");
        }
        Object parsed = new Parser(json).parseDocument();
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("profile JSON must be an object");
        }
        Map<?, ?> map = (Map<?, ?>) parsed;
        for (Object key : map.keySet()) {
            if (!(key instanceof String) || !KNOWN_FIELDS.contains(key)) {
                throw new IllegalArgumentException("unknown profile field " + key);
            }
        }

        int schemaVersion = integer(map, "schemaVersion", true);
        if (schemaVersion != CameraProfile.CURRENT_SCHEMA_VERSION) {
            throw new UnsupportedSchemaException(schemaVersion);
        }

        CameraProfile.Builder builder = CameraProfile.builder()
                .schemaVersion(schemaVersion)
                .stableId(string(map, "stableProfileId", true))
                .displayName(string(map, "displayName", true))
                .packageName(string(map, "packageName", true))
                .recordButtonResourceId(string(map, "recordButtonResourceId", false))
                .recordButtonClassName(string(map, "recordButtonClassName", false))
                .recordButtonText(string(map, "recordButtonText", false))
                .allowedContentDescriptions(strings(map, "allowedContentDescriptions"))
                .allowedContentDescriptionPatterns(
                        strings(map, "allowedContentDescriptionPatterns"))
                .parentResourceId(string(map, "parentResourceId", false))
                .parentClassName(string(map, "parentClassName", false))
                .requiredChildResourceIds(strings(map, "requiredChildResourceIds"))
                .requiredAccessibilityActions(actions(map, "requiredAccessibilityActions"))
                .stateResourceId(string(map, "stateResourceId", false))
                .recordingStateValues(strings(map, "recordingStateValues"))
                .standbyStateValues(strings(map, "standbyStateValues"))
                .durationResourceId(string(map, "durationResourceId", false))
                .stateVerificationSupported(
                        bool(map, "stateVerificationSupported", true))
                .capabilities(capabilities(map, "capabilities"))
                .origin(enumValue(map, "origin", CameraProfileOrigin.class))
                .validationStatus(enumValue(
                        map, "validationStatus", CameraProfileValidationStatus.class))
                .notes(string(map, "notes", false));
        return builder.build();
    }

    public static final class UnsupportedSchemaException extends IllegalArgumentException {
        public UnsupportedSchemaException(int schemaVersion) {
            super("unsupported camera profile schema version " + schemaVersion);
        }
    }

    private static void field(StringBuilder out, String name, String value, boolean comma) {
        if (comma) {
            out.append(',');
        }
        out.append(quote(name)).append(':').append(value);
    }

    private static String nullableString(String value) {
        return value == null ? "null" : quote(value);
    }

    private static String stringArray(List<String> values) {
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append(quote(values.get(i)));
        }
        return out.append(']').toString();
    }

    private static String enumArray(Iterable<? extends Enum<?>> values) {
        StringBuilder out = new StringBuilder("[");
        int index = 0;
        for (Enum<?> value : values) {
            if (index++ > 0) {
                out.append(',');
            }
            out.append(quote(value.name()));
        }
        return out.append(']').toString();
    }

    private static String quote(String value) {
        if (value == null) {
            throw new IllegalArgumentException("string value is required");
        }
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.append('"').toString();
    }

    private static String string(Map<?, ?> map, String key, boolean required) {
        if (!map.containsKey(key)) {
            if (required) {
                throw new IllegalArgumentException("missing profile field " + key);
            }
            return null;
        }
        Object value = map.get(key);
        if (value == null && !required) {
            return null;
        }
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("profile field " + key + " must be a string");
        }
        return (String) value;
    }

    private static int integer(Map<?, ?> map, String key, boolean required) {
        if (!map.containsKey(key)) {
            if (required) {
                throw new IllegalArgumentException("missing profile field " + key);
            }
            return 0;
        }
        Object value = map.get(key);
        if (!(value instanceof Long)) {
            throw new IllegalArgumentException("profile field " + key + " must be an integer");
        }
        long number = (Long) value;
        if (number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("profile field " + key + " is out of range");
        }
        return (int) number;
    }

    private static boolean bool(Map<?, ?> map, String key, boolean required) {
        if (!map.containsKey(key)) {
            if (required) {
                throw new IllegalArgumentException("missing profile field " + key);
            }
            return false;
        }
        Object value = map.get(key);
        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException("profile field " + key + " must be boolean");
        }
        return (Boolean) value;
    }

    private static List<String> strings(Map<?, ?> map, String key) {
        if (!map.containsKey(key) || map.get(key) == null) {
            return new ArrayList<>();
        }
        Object value = map.get(key);
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("profile field " + key + " must be an array");
        }
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (!(item instanceof String)) {
                throw new IllegalArgumentException("profile array " + key
                        + " must contain strings");
            }
            result.add((String) item);
        }
        return result;
    }

    private static List<CameraAccessibilityAction> actions(Map<?, ?> map, String key) {
        List<String> names = strings(map, key);
        List<CameraAccessibilityAction> result = new ArrayList<>();
        for (String name : names) {
            try {
                result.add(CameraAccessibilityAction.valueOf(name));
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("unsupported Accessibility action " + name);
            }
        }
        return result;
    }

    private static List<CameraCapability> capabilities(Map<?, ?> map, String key) {
        List<String> names = strings(map, key);
        List<CameraCapability> result = new ArrayList<>();
        for (String name : names) {
            try {
                result.add(CameraCapability.valueOf(name));
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("unsupported camera capability " + name);
            }
        }
        return result;
    }

    private static <T extends Enum<T>> T enumValue(
            Map<?, ?> map, String key, Class<T> type) {
        String name = string(map, key, true);
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("unsupported " + key + " " + name);
        }
    }

    private static final class Parser {
        private final String input;
        private int index;

        Parser(String input) {
            this.input = input;
        }

        Object parseDocument() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (index != input.length()) {
                throw error("trailing JSON data");
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= input.length()) {
                throw error("unexpected end of JSON");
            }
            char c = input.charAt(index);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' && consume("true")) return Boolean.TRUE;
            if (c == 'f' && consume("false")) return Boolean.FALSE;
            if (c == 'n' && consume("null")) return null;
            if (c == '-' || Character.isDigit(c)) return parseNumber();
            throw error("unexpected JSON token");
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (consumeChar('}')) {
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
                result.put(key, parseValue());
                skipWhitespace();
                if (consumeChar('}')) {
                    return result;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (consumeChar(']')) {
                return result;
            }
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (consumeChar(']')) {
                    return result;
                }
                expect(',');
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
                if (c < 0x20) {
                    throw error("control character in string");
                }
                if (c != '\\') {
                    result.append(c);
                    continue;
                }
                if (index >= input.length()) {
                    throw error("unfinished escape");
                }
                char escaped = input.charAt(index++);
                switch (escaped) {
                    case '"': result.append('"'); break;
                    case '\\': result.append('\\'); break;
                    case '/': result.append('/'); break;
                    case 'b': result.append('\b'); break;
                    case 'f': result.append('\f'); break;
                    case 'n': result.append('\n'); break;
                    case 'r': result.append('\r'); break;
                    case 't': result.append('\t'); break;
                    case 'u':
                        result.append(parseUnicode());
                        break;
                    default:
                        throw error("invalid escape");
                }
            }
            throw error("unterminated string");
        }

        private char parseUnicode() {
            if (index + 4 > input.length()) {
                throw error("unfinished unicode escape");
            }
            int value = 0;
            for (int i = 0; i < 4; i++) {
                int digit = Character.digit(input.charAt(index++), 16);
                if (digit < 0) {
                    throw error("invalid unicode escape");
                }
                value = (value << 4) | digit;
            }
            return (char) value;
        }

        private Long parseNumber() {
            int start = index;
            if (consumeChar('-')) {
                if (index >= input.length() || !Character.isDigit(input.charAt(index))) {
                    throw error("invalid number");
                }
            }
            if (consumeChar('0')) {
                if (index < input.length() && Character.isDigit(input.charAt(index))) {
                    throw error("leading zero in number");
                }
            } else {
                while (index < input.length() && Character.isDigit(input.charAt(index))) {
                    index++;
                }
            }
            if (index < input.length()
                    && (input.charAt(index) == '.' || input.charAt(index) == 'e'
                    || input.charAt(index) == 'E')) {
                throw error("profile numbers must be integers");
            }
            try {
                return Long.valueOf(input.substring(start, index));
            } catch (NumberFormatException error) {
                throw error("invalid number");
            }
        }

        private boolean consume(String value) {
            if (input.regionMatches(index, value, 0, value.length())) {
                index += value.length();
                return true;
            }
            return false;
        }

        private boolean consumeChar(char value) {
            if (index < input.length() && input.charAt(index) == value) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char value) {
            if (!consumeChar(value)) {
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
