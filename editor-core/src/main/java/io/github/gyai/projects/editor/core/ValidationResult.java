package io.github.gyai.projects.editor.core;

import java.util.List;

public record ValidationResult(List<ValidationIssue> issues) {
    public ValidationResult { issues = List.copyOf(issues == null ? List.of() : issues); }
    public boolean valid() { return issues.stream().noneMatch(issue -> issue.severity() == ValidationIssue.Severity.ERROR); }
    public static ValidationResult success() { return new ValidationResult(List.of()); }
}
