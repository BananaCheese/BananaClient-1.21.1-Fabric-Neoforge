package net.bananacheese.bananaclient.gui.profile;

import net.bananacheese.bananaclient.config.ConfigManager;
import net.bananacheese.bananaclient.gui.theme.ThemePresets;
import net.bananacheese.bananaclient.modules.ModuleManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ProfileManager {

    private static final List<Profile> profiles    = new ArrayList<>();
    private static Profile             active       = null;
    private static final String        SETTINGS_FILE = "settings.json";

    public static void init() {
        loadAll();
        loadActiveFromDisk();
        active.loadModuleSettings(ModuleManager.getAll());

        // Always ensure at least a default profile exists
        if (profiles.isEmpty()) {
            Profile def = new Profile("default", ThemePresets.sharp());
            profiles.add(def);
            save(def);
        }

        if (active == null) {
            active = profiles.get(0);
        }

        saveSettings();
    }

    // ── Queries ────────────────────────────────────────────────────────────

    public static Profile getActive()           { return active; }
    public static List<Profile> getAll()        { return profiles; }
    public static List<String>  getAllNames() {
        List<String> names = new ArrayList<>();
        for (Profile p : profiles) names.add(p.name);
        return names;
    }

    // ── Switch ─────────────────────────────────────────────────────────────

    public static void switchTo(String name) {
        for (Profile p : profiles) {
            if (p.name.equals(name)) {
                active = p;
                saveSettings();
                return;
            }
        }
    }

    // ── Save / Delete ──────────────────────────────────────────────────────

    public static void save(Profile profile) {
        Path file = ConfigManager.getProfilesDir().resolve(profile.name + ".json");
        ConfigManager.writeJson(file, profile);

        // Add to list if not already there
        if (profiles.stream().noneMatch(p -> p.name.equals(profile.name))) {
            profiles.add(profile);
        }
    }

    public static void saveActive() {
        if (active != null) {
            active.saveModuleSettings(
                    net.bananacheese.bananaclient.modules.ModuleManager.getAll());
            save(active);
        }
    }

    public static void delete(String name) {
        profiles.removeIf(p -> p.name.equals(name));
        Path file = ConfigManager.getProfilesDir().resolve(name + ".json");
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            System.err.println("[BananaClient] Failed to delete profile: " + e.getMessage());
        }

        // Fall back to first remaining profile
        if (active != null && active.name.equals(name)) {
            active = profiles.isEmpty() ? null : profiles.get(0);
            saveSettings();
        }
    }

    // ── Disk I/O ───────────────────────────────────────────────────────────

    private static void loadAll() {
        Path dir = ConfigManager.getProfilesDir();
        try (var stream = Files.list(dir)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        Profile profile = ConfigManager.readJson(p, Profile.class);
                        if (profile != null) profiles.add(profile);
                    });
        } catch (IOException e) {
            System.err.println("[BananaClient] Failed to list profiles: " + e.getMessage());
        }
    }

    private static void loadActiveFromDisk() {
        Path settingsPath = ConfigManager.getRootDir().resolve(SETTINGS_FILE);
        Settings settings = ConfigManager.readJson(settingsPath, Settings.class);
        if (settings != null) {
            switchTo(settings.activeProfile);
        }
    }

    private static void saveSettings() {
        Path settingsPath = ConfigManager.getRootDir().resolve(SETTINGS_FILE);
        Settings s = new Settings();
        s.activeProfile = active != null ? active.name : "default";
        ConfigManager.writeJson(settingsPath, s);
    }

    // Small inner class just for settings.json
    private static class Settings {
        public String activeProfile = "default";
    }
}
