package io.github.gyai.projects.devtools;

import io.github.gyai.projects.client.beta.BetaDisplayDocument;
import io.github.gyai.projects.client.beta.BetaUiViewModels;
import java.util.ArrayList;
import java.util.List;

/** DevTools-only Beta Mob Editor presentation mapping. */
public final class BetaMobEditorV2ViewModel {
    private BetaMobEditorV2ViewModel() { }
    public static BetaUiViewModels.Panel panel(BetaDisplayDocument state) {
        List<String> preferred=List.of("schema-version","base-revision","page","validation","conflict","save-result","rollback-result","test-spawn"); ArrayList<String> lines=new ArrayList<>(); for(String key:preferred)if(state.fields().containsKey(key))lines.add(key+": "+state.fields().get(key)); lines.addAll(state.entries()); boolean retry=state.status()!=BetaDisplayDocument.Status.RETRY_FORBIDDEN&&state.status()!=BetaDisplayDocument.Status.TERMINAL; return new BetaUiViewModels.Panel("Mob Editor v2",state.status(),state.message(),lines,retry,state.status()==BetaDisplayDocument.Status.CONFLICT);
    }
}
