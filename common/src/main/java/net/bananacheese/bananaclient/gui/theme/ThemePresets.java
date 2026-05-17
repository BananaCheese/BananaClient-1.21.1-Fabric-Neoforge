package net.bananacheese.bananaclient.gui.theme;

import java.util.ArrayList;
import java.util.List;

public class ThemePresets {

    public static Theme sharp() {
        Theme t = new Theme();
        t.name                = "Sharp";
        t.backgroundColor     = 0xF0101010;
        t.headerColor         = 0xFF1A1A1A;
        t.borderColor         = 0xFF2A2A2A;
        t.accentColor         = 0xFFF5C518;
        t.enabledTextColor    = 0xFFF5C518;
        t.disabledTextColor   = 0xFF555555;
        t.headerTextColor     = 0xFFF5C518;
        t.categoryTextColor   = 0xFF444444;
        t.keyBadgeBackground  = 0xFF1E1E1E;
        t.keyBadgeText        = 0xFF666666;
        t.showToggleSwitches  = false;
        t.showCategoryLabels  = true;
        t.backgroundOpacity   = 0.95f;
        return t;
    }

    public static Theme frosted() {
        Theme t = new Theme();
        t.name                = "Frosted";
        t.backgroundColor     = 0xED14141E;
        t.headerColor         = 0xFF1A1A2A;
        t.borderColor         = 0x1AFFFFFF;
        t.accentColor         = 0xFF63B3ED;
        t.enabledTextColor    = 0xFFFFFFFF;
        t.disabledTextColor   = 0xFF555577;
        t.headerTextColor     = 0xFFFFFFFF;
        t.categoryTextColor   = 0x44FFFFFF;
        t.keyBadgeBackground  = 0x00000000;
        t.keyBadgeText        = 0xFF63B3ED;
        t.showToggleSwitches  = true;
        t.showCategoryLabels  = true;
        t.backgroundOpacity   = 0.93f;
        return t;
    }

    public static Theme amethyst() {
        Theme t = new Theme();
        t.name                = "Amethyst";
        t.backgroundColor     = 0xF013131A;
        t.headerColor         = 0xFF6C3FC5;
        t.borderColor         = 0xFF1F1F2E;
        t.accentColor         = 0xFF8855E8;
        t.enabledTextColor    = 0xFFDDDDDD;
        t.disabledTextColor   = 0xFF444444;
        t.headerTextColor     = 0xFFFFFFFF;
        t.categoryTextColor   = 0xFF6C3FC5;
        t.keyBadgeBackground  = 0x331A1A28;
        t.keyBadgeText        = 0xFF8855E8;
        t.showToggleSwitches  = false;
        t.showCategoryLabels  = true;
        t.backgroundOpacity   = 0.94f;
        return t;
    }

    // Returns fresh copies — players always edit copies, never the originals
    public static List<Theme> all() {
        List<Theme> list = new ArrayList<>();
        list.add(sharp());
        list.add(frosted());
        list.add(amethyst());
        return list;
    }
}
