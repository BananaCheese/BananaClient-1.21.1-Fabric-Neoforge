package net.bananacheese.bananaclient.gui.theme;

public class Theme {

    public String  name;

    // Panel background (ARGB int)
    public int     backgroundColor;
    public int     headerColor;
    public int     borderColor;

    // Accent — used for enabled strip, active text, key badge highlight
    public int     accentColor;

    // Text
    public int     enabledTextColor;
    public int     disabledTextColor;
    public int     headerTextColor;
    public int     categoryTextColor;

    // Key badge
    public int     keyBadgeBackground;
    public int     keyBadgeText;

    // Style flags
    public boolean showToggleSwitches;  // true = frosted toggle, false = left strip
    public boolean showCategoryLabels;
    public boolean roundedCorners;      // cosmetic hint for renderer
    public float   backgroundOpacity;   // 0.0 - 1.0, blended at render time

    // Deep copy — used when player creates a new profile from an existing theme
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
        t.roundedCorners      = this.roundedCorners;
        t.backgroundOpacity   = this.backgroundOpacity;
        return t;
    }
}