package io.github.gyai.projects.client;

import io.github.gyai.projects.client.menu.ProjectSMenuExtension;
import io.github.gyai.projects.client.menu.ProjectSMenuExtensions;

/** Core-only checks: developer implementation classes are not present and catalog remains usable. */
public final class ClientCoreBoundaryTest {
    public static void main(String[] args) throws Exception {
        try { Class.forName("io.github.gyai.projects.client.MobEditorScreen", false, ClientCoreBoundaryTest.class.getClassLoader()); throw new AssertionError("Mob Editor leaked into client-core"); } catch (ClassNotFoundException expected) { }
        SkillCatalog.Skill known = SkillCatalog.findById("spin_slash");
        assert SkillDescriptionStore.description(known).equals(known.description());
        assert !io.github.gyai.projects.client.beta.BetaClientRuntime.isCapabilityEnabled(
                io.github.gyai.projects.client.beta.BetaProtocol.Capability.MOB_EDITOR_V2);
        ProjectSMenuExtensions.register(new ProjectSMenuExtension("test.z", "Z", "", () -> true, screen -> { }));
        ProjectSMenuExtensions.register(new ProjectSMenuExtension("test.a", "A", "", () -> true, screen -> { }));
        assert ProjectSMenuExtensions.entries().get(0).id().equals("test.a");
        try { ProjectSMenuExtensions.register(new ProjectSMenuExtension("test.a", "again", "", () -> true, screen -> { })); throw new AssertionError("duplicate accepted"); } catch (IllegalStateException expected) { }
    }
}
