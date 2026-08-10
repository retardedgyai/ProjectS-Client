package io.github.gyai.projects.devtools.skillvfx;

import java.util.Optional;

/** Connection-local v3-first preference with an immutable fallback choice after the first catalog attempt. */
public final class SkillVfxEditorProtocolSelection {
    public enum Version { V1, V2, V3 }
    private Version selected;
    public Optional<Version> select(boolean canV3, boolean canV2, boolean canV1) {
        if(selected!=null)return Optional.of(selected);
        selected=canV3?Version.V3:canV2?Version.V2:canV1?Version.V1:null;return Optional.ofNullable(selected);
    }
    public Optional<Version> select(boolean canV2, boolean canV1) {
        return select(false,canV2,canV1);
    }
    public Optional<Version> selected(){return Optional.ofNullable(selected);}
    public void reset(){selected=null;}
}
