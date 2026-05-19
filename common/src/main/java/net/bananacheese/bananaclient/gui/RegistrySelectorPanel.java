package net.bananacheese.bananaclient.gui;

import net.bananacheese.bananaclient.gui.theme.Theme;
import net.bananacheese.bananaclient.gui.theme.ThemeManager;
import net.bananacheese.bananaclient.modules.RegistryListSetting;
import net.bananacheese.bananaclient.utils.RenderUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class RegistrySelectorPanel {

    private static final int WIDTH    = 230;
    private static final int HEADER_H = 18;
    private static final int TAB_H    = 16;
    private static final int SEARCH_H = 20;
    private static final int SEL_H    = 18;
    private static final int ROW_H    = 16;
    private static final int MAX_ROWS = 12;
    private static final int PAD      = 6;

    private int x, y;
    private final RegistryListSetting setting;

    // Tab — only relevant when mode == BOTH
    private int activeTab = 0; // 0 = Items, 1 = Blocks

    // Search
    private final StringBuilder searchText = new StringBuilder();
    private String lastSearch = null;
    private int scrollOffset = 0;

    // Filtered list
    private List<ResourceLocation> filtered = new ArrayList<>();

    // Dragging
    private boolean dragging = false;
    private int dragOffsetX, dragOffsetY;

    public RegistrySelectorPanel(int x, int y, RegistryListSetting setting) {
        this.x       = x;
        this.y       = y;
        this.setting = setting;
        rebuildFilter();
    }

    // ── Geometry ──────────────────────────────────────────────────────────

    private boolean hasTabs() {
        return setting.getMode() == RegistryListSetting.Mode.BOTH;
    }

    private int panelHeight() {
        int h = HEADER_H;
        if (hasTabs()) h += TAB_H;
        if (setting.size() > 0) h += SEL_H;
        h += SEARCH_H;
        h += Math.min(filtered.size(), MAX_ROWS) * ROW_H;
        h += PAD;
        return h;
    }

    public boolean isInPanel(double mx, double my) {
        return mx >= x && mx <= x + WIDTH
                && my >= y && my <= y + panelHeight();
    }

    // ── Filter ────────────────────────────────────────────────────────────

    private void rebuildFilter() {
        String query = searchText.toString().toLowerCase().trim();
        if (query.equals(lastSearch)) return;
        lastSearch = query;
        scrollOffset = 0;
        filtered = new ArrayList<>();

        // Determine which registry to scan based on mode + active tab
        boolean scanItems = setting.getMode() == RegistryListSetting.Mode.ITEMS
                || (setting.getMode() == RegistryListSetting.Mode.BOTH && activeTab == 0);
        boolean scanBlocks = setting.getMode() == RegistryListSetting.Mode.BLOCKS
                || (setting.getMode() == RegistryListSetting.Mode.BOTH && activeTab == 1);

        if (scanItems) {
            for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
                if (matches(id, query)) filtered.add(id);
            }
        }
        if (scanBlocks) {
            for (ResourceLocation id : BuiltInRegistries.BLOCK.keySet()) {
                // Skip air
                if (id.getPath().equals("air")) continue;
                if (matches(id, query)) filtered.add(id);
            }
        }

        // Sort: selected first, then namespace (minecraft first), then path
        filtered.sort((a, b) -> {
            boolean asel = setting.contains(a);
            boolean bsel = setting.contains(b);
            if (asel != bsel) return asel ? -1 : 1;
            // minecraft namespace first
            boolean avan = a.getNamespace().equals("minecraft");
            boolean bvan = b.getNamespace().equals("minecraft");
            if (avan != bvan) return avan ? -1 : 1;
            return a.getPath().compareTo(b.getPath());
        });
    }

    private boolean matches(ResourceLocation id, String query) {
        if (query.isEmpty()) return true;
        String full = id.toString().toLowerCase();
        String path = id.getPath().replace("_", " ").toLowerCase();
        return full.contains(query) || path.contains(query);
    }

    // ── Render ────────────────────────────────────────────────────────────

    public void render(GuiGraphics gfx, Font font, int mouseX, int mouseY) {
        rebuildFilter();
        Theme t = ThemeManager.get();
        int h = panelHeight();

        int bgColor = RenderUtil.applyOpacity(t.backgroundColor, t.backgroundOpacity);
        gfx.fill(x, y, x + WIDTH, y + h, bgColor);
        gfx.fill(x, y, x + WIDTH, y + HEADER_H, t.headerColor);
        gfx.renderOutline(x, y, WIDTH, h, t.borderColor);

        // Header
        gfx.drawString(font, setting.getName(), x + PAD, y + 5, t.headerTextColor, false);
        String countStr = setting.size() + " selected";
        gfx.drawString(font, countStr,
                x + WIDTH - font.width(countStr) - PAD, y + 5,
                t.disabledTextColor, false);

        int curY = y + HEADER_H;

        // ── Tabs (BOTH mode only) ─────────────────────────────────────────
        if (hasTabs()) {
            String[] tabNames = { "Items", "Blocks" };
            int tabW = WIDTH / 2;
            for (int i = 0; i < 2; i++) {
                int tabX = x + i * tabW;
                boolean active = activeTab == i;
                boolean hov = mouseX >= tabX && mouseX <= tabX + tabW
                        && mouseY >= curY && mouseY <= curY + TAB_H;
                gfx.fill(tabX, curY, tabX + tabW, curY + TAB_H,
                        active ? t.accentColor & 0x33FFFFFF | 0x33000000 : t.headerColor);
                if (hov && !active)
                    gfx.fill(tabX, curY, tabX + tabW, curY + TAB_H, 0x18FFFFFF);
                if (active)
                    gfx.fill(tabX, curY + TAB_H - 1, tabX + tabW, curY + TAB_H, t.accentColor);
                int labelX = tabX + (tabW - font.width(tabNames[i])) / 2;
                gfx.drawString(font, tabNames[i], labelX, curY + 4,
                        active ? t.accentColor : t.disabledTextColor, false);
            }
            curY += TAB_H;
        }

        // ── Selected chips ────────────────────────────────────────────────
        if (setting.size() > 0) {
            gfx.fill(x, curY, x + WIDTH, curY + SEL_H, t.headerColor & 0xBBFFFFFF);
            int chipX = x + PAD;
            for (ResourceLocation id : setting.getEntries()) {
                String label = id.getPath().replace("_", " ");
                int chipW = font.width(label) + 8;
                if (chipX + chipW > x + WIDTH - PAD) {
                    // Overflow indicator
                    gfx.drawString(font, "...", chipX + 2, curY + 5,
                            t.disabledTextColor, false);
                    break;
                }
                gfx.fill(chipX, curY + 3, chipX + chipW, curY + SEL_H - 3,
                        t.accentColor & 0x33FFFFFF);
                gfx.renderOutline(chipX, curY + 3, chipW, SEL_H - 6, t.accentColor);
                gfx.drawString(font, label, chipX + 4, curY + 5, t.accentColor, false);
                chipX += chipW + 3;
            }
            curY += SEL_H;
        }

        // ── Search field ──────────────────────────────────────────────────
        gfx.fill(x + PAD, curY + 3, x + WIDTH - PAD, curY + SEARCH_H - 3, 0xFF000000);
        gfx.renderOutline(x + PAD, curY + 3, WIDTH - PAD * 2, SEARCH_H - 6,
                t.borderColor);
        String display = searchText.length() == 0
                ? "Search..."
                : searchText.toString();
        boolean cursor = (System.currentTimeMillis() / 500) % 2 == 0;
        gfx.drawString(font, display + (cursor ? "|" : ""),
                x + PAD + 4, curY + 7,
                searchText.length() == 0 ? t.disabledTextColor : t.enabledTextColor, false);
        curY += SEARCH_H;

        // ── Entry list ────────────────────────────────────────────────────
        int startIdx = Math.max(0, Math.min(scrollOffset,
                Math.max(0, filtered.size() - MAX_ROWS)));

        for (int i = 0; i < Math.min(filtered.size(), MAX_ROWS); i++) {
            int idx = startIdx + i;
            if (idx >= filtered.size()) break;

            ResourceLocation id = filtered.get(idx);
            boolean selected = setting.contains(id);
            int rowY = curY + i * ROW_H;
            boolean hovered = mouseX >= x && mouseX <= x + WIDTH
                    && mouseY >= rowY && mouseY <= rowY + ROW_H;

            if (hovered) gfx.fill(x, rowY, x + WIDTH, rowY + ROW_H, 0x18FFFFFF);
            if (selected) gfx.fill(x, rowY, x + 2, rowY + ROW_H, t.accentColor);

            // Checkbox
            int checkX = x + WIDTH - 12 - PAD;
            int checkY = rowY + (ROW_H - 8) / 2;
            gfx.fill(checkX, checkY, checkX + 8, checkY + 8,
                    selected ? t.accentColor : t.disabledTextColor);

            // Non-vanilla namespace badge
            boolean isVanilla = id.getNamespace().equals("minecraft");
            int textX = x + PAD;
            if (!isVanilla) {
                String ns = id.getNamespace();
                int nsW = font.width(ns) + 4;
                gfx.fill(textX, rowY + (ROW_H - 10) / 2,
                        textX + nsW, rowY + (ROW_H + 10) / 2, t.headerColor);
                gfx.drawString(font, ns, textX + 2, rowY + (ROW_H - 9) / 2,
                        t.categoryTextColor, false);
                textX += nsW + 4;
            }

            // Name — replace underscores, capitalise first letter
            String raw = id.getPath().replace("_", " ");
            String name = raw.isEmpty() ? raw
                    : Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
            gfx.drawString(font, name, textX, rowY + (ROW_H - 9) / 2,
                    selected ? t.enabledTextColor : t.disabledTextColor, false);
        }

        // Scroll indicator
        if (filtered.size() > MAX_ROWS) {
            String info = (startIdx + Math.min(filtered.size(), MAX_ROWS))
                    + " / " + filtered.size();
            gfx.drawString(font, info,
                    x + WIDTH - font.width(info) - PAD,
                    y + h - 10, t.disabledTextColor, false);
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────

    public boolean mouseClicked(double mx, double my, int button) {
        if (!isInPanel(mx, my)) return false;

        if (my >= y && my <= y + HEADER_H) {
            if (button == 0) {
                dragging = true;
                dragOffsetX = (int) mx - x;
                dragOffsetY = (int) my - y;
            }
            return true;
        }

        int curY = y + HEADER_H;

        // Tab clicks
        if (hasTabs() && my >= curY && my <= curY + TAB_H) {
            int tabW = WIDTH / 2;
            int col = (int)(mx - x) / tabW;
            if (col >= 0 && col <= 1 && col != activeTab) {
                activeTab = col;
                lastSearch = null;
                rebuildFilter();
            }
            return true;
        }
        if (hasTabs()) curY += TAB_H;

        // Skip selected chips row
        if (setting.size() > 0) curY += SEL_H;

        // Skip search bar
        curY += SEARCH_H;

        // Entry rows
        int startIdx = Math.max(0, Math.min(scrollOffset,
                Math.max(0, filtered.size() - MAX_ROWS)));

        for (int i = 0; i < Math.min(filtered.size(), MAX_ROWS); i++) {
            int idx = startIdx + i;
            if (idx >= filtered.size()) break;
            int rowY = curY + i * ROW_H;
            if (my >= rowY && my <= rowY + ROW_H) {
                setting.toggle(filtered.get(idx));
                lastSearch = null;
                rebuildFilter();
                return true;
            }
        }

        return true;
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!isInPanel(mx, my)) return false;
        scrollOffset = Math.max(0,
                Math.min(Math.max(0, filtered.size() - MAX_ROWS),
                        scrollOffset - (int) Math.signum(delta)));
        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && searchText.length() > 0) {
            searchText.deleteCharAt(searchText.length() - 1);
            lastSearch = null;
            return true;
        }
        return false;
    }

    public void charTyped(char c) {
        if (c >= 32 && c != '`') {
            searchText.append(Character.toLowerCase(c));
            lastSearch = null;
        }
    }

    public void drag(double mx, double my) {
        if (!dragging) return;
        x = (int) mx - dragOffsetX;
        y = (int) my - dragOffsetY;
    }

    public void stopDrag()      { dragging = false; }
    public boolean isDragging() { return dragging; }
}