package net.bananacheese.bananaclient.modules;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {

    protected static final Minecraft mc = Minecraft.getInstance();

    public enum Category {
        COMBAT, MOVEMENT, RENDER, MISC
    }

    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled = false;
    private int keyCode;

    protected final List<ModuleSetting<?>> settings = new ArrayList<>();

    public Module(String name, String description, Category category, int defaultKey) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.keyCode = defaultKey;
    }

    public Module(String name, String description, Category category) {
        this(name, description, category, GLFW.GLFW_KEY_UNKNOWN);
    }

    public void onEnable()  {}
    public void onDisable() {}

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable();
        else onDisable();
    }

    // Subclasses add their settings in their constructor via this
    protected <T> ModuleSetting<T> addSetting(ModuleSetting<T> setting) {
        settings.add(setting);
        return setting;
    }

    public List<ModuleSetting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public boolean hasSettings() { return !settings.isEmpty(); }

    public boolean  isEnabled()      { return enabled; }
    public String   getName()        { return name; }
    public String   getDescription() { return description; }
    public Category getCategory()    { return category; }
    public int      getKeyCode()     { return keyCode; }
    public void     setKeyCode(int k){ keyCode = k; }

    public String getKeyName() {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) return "NONE";
        String n = GLFW.glfwGetKeyName(keyCode, 0);
        return n != null ? n.toUpperCase() : "KEY_" + keyCode;
    }
}