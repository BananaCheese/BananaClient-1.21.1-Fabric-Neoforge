package net.bananacheese.bananaclient.gui.theme;

public class Theme {

    public String  name;

    public int     backgroundColor;
    public int     headerColor;
    public int     borderColor;
    public int     accentColor;

    public int     enabledTextColor;
    public int     disabledTextColor;
    public int     headerTextColor;
    public int     categoryTextColor;

    public int     keyBadgeBackground;
    public int     keyBadgeText;

    public boolean showToggleSwitches;
    public boolean showCategoryLabels;
    public float   backgroundOpacity;

    public Theme copy() {
        Theme t = new Theme();
        t.name                = this.name + " (copy)";
        t.backgroundColor     = this.backgroundColor;
        t.headerColor         = this.headerColor;
        t.borderColor         = this.borderColor;
        t.accentColor         = this.accentColor;
        t.enabledTextColor    = this.enabledTextColor;
        t.disabledTextColor   = this.disabledTextColor;
        t.headerTextColor     = this.headerTextColor;
        t.categoryTextColor   = this.categoryTextColor;
        t.keyBadgeBackground  = this.keyBadgeBackground;
        t.keyBadgeText        = this.keyBadgeText;
        t.showToggleSwitches  = this.showToggleSwitches;
        t.showCategoryLabels  = this.showCategoryLabels;
        t.backgroundOpacity   = this.backgroundOpacity;
        return t;
    }
}