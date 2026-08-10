package io.github.gyai.projects.devtools.skillvfx;

import java.util.Optional;

/** Connection-local v2 preference with an immutable fallback choice after the first catalog attempt. */
public final class SkillVfxEditorProtocolSelection {
    public enum Version { V1, V2 }
    private Version selected;
    public Optional<Version> select(boolean canV2, boolean canV1) {
        if(selected!=null)return Optional.of(selected);
        selected=canV2?Version.V2:canV1?Version.V1:null;return Optional.ofNullable(selected);
    }
    public Optional<Version> selected(){return Optional.ofNullable(selected);}
    public void reset(){selected=null;}
}
