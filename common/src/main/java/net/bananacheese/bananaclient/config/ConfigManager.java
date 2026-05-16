package net.bananacheese.bananaclient.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Resolves to .minecraft/BananaClient/
    public static Path getRootDir() {
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("BananaClient");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("[BananaClient] Failed to create config dir: " + e.getMessage());
        }
        return dir;
    }

    public static Path getProfilesDir() {
        Path dir = getRootDir().resolve("profiles");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("[BananaClient] Failed to create profiles dir: " + e.getMessage());
        }
        return dir;
    }

    public static <T> void writeJson(Path path, T object) {
        try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(object, writer);
        } catch (IOException e) {
            System.err.println("[BananaClient] Failed to write " + path + ": " + e.getMessage());
        }
    }

    public static <T> T readJson(Path path, Class<T> clazz) {
        if (!Files.exists(path)) return null;
        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, clazz);
        } catch (IOException e) {
            System.err.println("[BananaClient] Failed to read " + path + ": " + e.getMessage());
            return null;
        }
    }

    public static boolean exists(Path path) {
        return Files.exists(path);
    }
}
