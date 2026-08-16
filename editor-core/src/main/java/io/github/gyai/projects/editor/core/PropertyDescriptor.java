package io.github.gyai.projects.editor.core;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** Frontend-neutral property contract. Validation messages are deterministic metadata. */
public final class PropertyDescriptor<D, T> {
    private final String id, displayName, group, category;
    private final Class<T> type;
    private final Function<D, T> reader;
    private final BiConsumer<D, T> writer;
    private final Predicate<T> validator;
    private final String validationMessage;
    public PropertyDescriptor(String id, String displayName, Class<T> type, Function<D, T> reader,
            BiConsumer<D, T> writer, Predicate<T> validator, String validationMessage, String group, String category) {
        if (id == null || id.isBlank() || displayName == null || displayName.isBlank() || type == null || reader == null || writer == null) throw new IllegalArgumentException("Invalid property descriptor");
        this.id=id; this.displayName=displayName; this.type=type; this.reader=reader; this.writer=writer;
        this.validator=validator == null ? value -> true : validator;
        this.validationMessage=validationMessage == null ? "Invalid value" : validationMessage;
        this.group=group == null ? "General" : group; this.category=category == null ? "General" : category;
    }
    public String id(){ return id; } public String displayName(){ return displayName; } public Class<T> type(){ return type; }
    public String group(){ return group; } public String category(){ return category; }
    public T read(D document){ return reader.apply(Objects.requireNonNull(document)); }
    public void write(D document, T value){ writer.accept(Objects.requireNonNull(document), value); }
    public ValidationResult validate(T value){ return validator.test(value) ? ValidationResult.success() : new ValidationResult(java.util.List.of(new ValidationIssue(id, ValidationIssue.Severity.ERROR, validationMessage))); }
}
