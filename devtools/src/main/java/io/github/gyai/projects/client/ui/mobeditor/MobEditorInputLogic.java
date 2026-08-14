package io.github.gyai.projects.client.ui.mobeditor;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

/** Pure byte/count bounds shared by Mob and custom-head input forms. */
public final class MobEditorInputLogic {
    private MobEditorInputLogic() { }

    public static boolean utf8Within(String value, int maximumBytes) {
        return value != null && maximumBytes >= 0
                && value.getBytes(StandardCharsets.UTF_8).length <= maximumBytes;
    }

    public static boolean validHeadImport(
            String id, String displayName, String textureValue, String sourceNote,
            List<String> tags
    ) {
        List<String> safeTags = tags == null ? List.of() : tags;
        return utf8Within(id, 64) && utf8Within(displayName, 128)
                && utf8Within(textureValue, 16_384) && utf8Within(sourceNote, 256)
                && safeTags.size() <= 32
                && new HashSet<>(safeTags).size() == safeTags.size()
                && safeTags.stream().allMatch(value -> utf8Within(value, 32));
    }
}
