package io.github.gyai.projects.client.beta;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public record BetaDisplayDocument(
        long revision,
        Status status,
        String message,
        Map<String, String> fields,
        List<String> entries
) {
    public enum Status {
        LOADING, READY, ERROR, UNSUPPORTED, CONFLICT, TERMINAL, RETRY_FORBIDDEN
    }

    public BetaDisplayDocument {
        if (revision < 0 || status == null) throw new IllegalArgumentException("Invalid display state");
        message = bounded(message == null ? "" : message, "message");
        fields = Map.copyOf(fields == null ? Map.of() : fields);
        entries = List.copyOf(entries == null ? List.of() : entries);
        if (fields.size() > BetaProtocol.MAP_MAX_ENTRIES
                || entries.size() > BetaProtocol.LIST_MAX_ENTRIES) {
            throw new IllegalArgumentException("Display state is oversized");
        }
        fields.forEach((key, value) -> {
            canonical(key);
            bounded(value, "field value");
            if (isNumericField(key)) {
                try {
                    double numeric = Double.parseDouble(value);
                    if (!Double.isFinite(numeric)) {
                        throw new IllegalArgumentException("Numeric display value must be finite");
                    }
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("Invalid numeric display value", exception);
                }
            }
        });
        entries.forEach(value -> bounded(value, "entry"));
    }

    public static BetaDisplayDocument loading() {
        return new BetaDisplayDocument(0, Status.LOADING, "読み込み中", Map.of(), List.of());
    }

    public static BetaDisplayDocument unsupported(String message) {
        return new BetaDisplayDocument(0, Status.UNSUPPORTED, message, Map.of(), List.of());
    }

    private static String bounded(String value, String name) {
        if (value.getBytes(StandardCharsets.UTF_8).length > BetaProtocol.STRING_MAX_BYTES) {
            throw new IllegalArgumentException(name + " is oversized");
        }
        return value;
    }

    private static void canonical(String value) {
        if (value == null || value.isBlank() || value.length() > 128
                || !value.matches("[a-z0-9][a-z0-9._:/-]*")) {
            throw new IllegalArgumentException("Invalid canonical field ID");
        }
    }

    private static boolean isNumericField(String key) {
        return key.endsWith("-gauge") || key.endsWith("-ratio")
                || key.equals("quality") || key.equals("xp") || key.equals("level")
                || key.equals("item-level") || key.equals("fire-stacks")
                || key.equals("refreeze-immunity")
                || key.equals("target-network-id") || key.equals("state-revision")
                || key.equals("fire-threshold")
                || key.equals("fire-decay-starts-in-millis")
                || key.equals("fire-detonation-pulse-revision")
                || key.equals("snapshot-expires-at-millis");
    }
}
