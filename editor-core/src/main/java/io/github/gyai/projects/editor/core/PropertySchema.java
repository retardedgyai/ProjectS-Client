package io.github.gyai.projects.editor.core;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ordered schema with duplicate-id rejection, so inspector order never depends on a hash map. */
public final class PropertySchema<D> {
    private final Map<String, PropertyDescriptor<D, ?>> byId;
    private final List<PropertyDescriptor<D, ?>> properties;
    public PropertySchema(Collection<? extends PropertyDescriptor<D, ?>> descriptors) {
        LinkedHashMap<String, PropertyDescriptor<D, ?>> values = new LinkedHashMap<>();
        for (PropertyDescriptor<D, ?> descriptor : descriptors == null ? List.<PropertyDescriptor<D, ?>>of() : descriptors) {
            if (values.putIfAbsent(descriptor.id(), descriptor) != null) throw new IllegalArgumentException("Duplicate property id: " + descriptor.id());
        }
        properties = List.copyOf(values.values());
        byId = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
    public List<PropertyDescriptor<D, ?>> properties() { return properties; }
    public PropertyDescriptor<D, ?> property(String id) { return byId.get(id); }
}
