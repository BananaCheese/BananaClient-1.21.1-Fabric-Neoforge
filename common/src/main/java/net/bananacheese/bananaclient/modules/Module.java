package net.bananacheese.bananaclient.modules;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {

    protected static final Minecraft mc = Minecraft.getInstance();

    public enum Category { COMBAT, PLAYER, MOVEMENT, RENDER, MISC }

    private final String   name;
    private final String   description;
    private final Category category;
    private final int      defaultKey;   // fallback if no profile entry exists
    private boolean        enabled = false;
    private int            keyCode;

    protected final List<ModuleSetting<?>> settings = new ArrayList<>();

    public Module(String name, String description, Category category, int defaultKey) {
        this.name       = name;
        this.description = description;
        this.category   = category;
        this.defaultKey = defaultKey;
        // Key loaded from profile in ModuleManager.init() after profile is ready
        this.keyCode    = defaultKey;
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
    public int      getDefaultKey()  { return defaultKey; }
    public int      getKeyCode()     { return keyCode; }

    // Called by ModuleManager when profile loads — restores persisted key
    public void loadKeyCode(int code) { this.keyCode = code; }

    // Called when player rebinds — updates in-memory AND profile
    public void setKeyCode(int code) {
        this.keyCode = code;
        // Write through to active profile immediately
        net.bananacheese.bananaclient.gui.profile.ProfileManager
                .getActive().setKeybind(name, code);
        net.bananacheese.bananaclient.gui.profile.ProfileManager.saveActive();
    }

    public String getKeyName() {
        if (keyCode == GLFW.GLFW_KEY_UNKNOWN) return "NONE";
        String n = GLFW.glfwGetKeyName(keyCode, 0);
        return n != null ? n.toUpperCase() : "KEY_" + keyCode;
    }
}