package io.github.gyai.projects.editor.core;

public record ValidationIssue(String propertyId, Severity severity, String message) {
    public ValidationIssue {
        if (propertyId == null || propertyId.isBlank() || severity == null || message == null || message.isBlank()) throw new IllegalArgumentException("Invalid validation issue");
    }
    public enum Severity { ERROR, WARNING }
}
