package io.github.gyai.projects.client.ui.widget;

public final class ProjectSInteractionGate {
    private boolean modalOpen;

    public boolean canUseBackground() {
        return !modalOpen;
    }

    public boolean modalOpen() {
        return modalOpen;
    }

    public void openModal() {
        modalOpen = true;
    }

    public void closeModal() {
        modalOpen = false;
    }
}
