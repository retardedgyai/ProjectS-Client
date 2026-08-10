package io.github.gyai.projects.client;

import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only player-facing descriptions; DevTools may overlay received balance values. */
public final class SkillDescriptionSnapshot {
    private final Map<String, String> descriptions;
    private SkillDescriptionSnapshot(Map<String, String> descriptions) { this.descriptions = Map.copyOf(descriptions); }
    public static SkillDescriptionSnapshot fromCatalog() {
        Map<String, String> values = new LinkedHashMap<>();
        SkillCatalog.GROUPS.forEach(group -> group.skills().forEach(skill -> values.put(skill.id(), skill.description())));
        return new SkillDescriptionSnapshot(values);
    }
    public static SkillDescriptionSnapshot fromValues(Map<String, String> descriptions) { return new SkillDescriptionSnapshot(new LinkedHashMap<>(descriptions)); }
    public String description(String skillId) { return descriptions.getOrDefault(skillId, ""); }
    public Map<String, String> values() { return descriptions; }
}
