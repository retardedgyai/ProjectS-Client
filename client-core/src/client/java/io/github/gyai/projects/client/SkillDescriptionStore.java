package io.github.gyai.projects.client;

/** Player-facing read-only description view. Core starts with catalog text if DevTools is absent. */
public final class SkillDescriptionStore {
    private static SkillDescriptionSnapshot snapshot = SkillDescriptionSnapshot.fromCatalog();
    private SkillDescriptionStore() { }
    public static synchronized String description(SkillCatalog.Skill skill) { return skill == null ? "" : snapshot.description(skill.id()).isBlank() ? skill.description() : snapshot.description(skill.id()); }
    public static synchronized void publish(SkillDescriptionSnapshot received) { snapshot = received == null ? SkillDescriptionSnapshot.fromCatalog() : received; }
    public static synchronized void reset() { snapshot = SkillDescriptionSnapshot.fromCatalog(); }
}
