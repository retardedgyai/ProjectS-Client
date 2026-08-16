package io.github.gyai.projects.devtools.ui;

import io.github.gyai.projects.client.ui.render.ProjectSUiLayout;

/** UI-Kit-only dimensions intentionally kept out of the player UI layout API. */
public final class DevToolsUiKitLayout {
    public enum Section { BUTTONS, ICON_GALLERY, INPUTS, NAVIGATION, CARDS, FEEDBACK, THEMES, TOKENS }
    private DevToolsUiKitLayout() { }
    public static int headerHeight() { return 58; }
    public static int footerHeight() { return 42; }
    public static int sectionHeight(Section section, boolean narrow) { return switch(section) { case BUTTONS -> narrow?170:120; case ICON_GALLERY -> narrow?1142:614; case INPUTS -> narrow?292:184; case NAVIGATION -> narrow?200:150; case CARDS -> 142; case FEEDBACK -> narrow?120:88; case THEMES -> narrow?224:136; case TOKENS -> 166; }; }
    public static int contentHeight(int screenWidth) { int panelWidth=ProjectSUiLayout.contentWidth(screenWidth), columns=ProjectSUiLayout.columns(screenWidth), columnWidth=columns==2?(panelWidth-12)/2:panelWidth; boolean narrow=columns==1&&columnWidth<380; if(columns==1){int height=0;for(Section section:Section.values())height+=sectionHeight(section,narrow)+12;return height;} int left=sectionHeight(Section.BUTTONS,false)+sectionHeight(Section.INPUTS,false)+sectionHeight(Section.FEEDBACK,false)+sectionHeight(Section.THEMES,false)+48; int right=sectionHeight(Section.ICON_GALLERY,false)+sectionHeight(Section.NAVIGATION,false)+sectionHeight(Section.CARDS,false)+sectionHeight(Section.TOKENS,false)+48; return Math.max(left,right); }
}
