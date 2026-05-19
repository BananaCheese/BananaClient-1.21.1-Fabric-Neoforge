package net.bananacheese.bananaclient.modules;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class RegistryListSetting {

    public enum Mode { BLOCKS, ITEMS, BOTH }

    private final String name;
    private final String description;
    private final Mode   mode;
    private final Set<ResourceLocation> entries;
    private final Set<ResourceLocation> defaults;

    public RegistryListSetting(String name, String description,
                               Mode mode, ResourceLocation... defaults) {
        this.name        = name;
        this.description = description;
        this.mode        = mode;
        this.entries     = new LinkedHashSet<>();
        this.defaults    = new LinkedHashSet<>();

        for (ResourceLocation rl : defaults) {
            this.entries.add(rl);
            this.defaults.add(rl);
        }
    }

    // Convenience constructors
    public static RegistryListSetting blocks(String name, String desc,
                                             ResourceLocation... defaults) {
        return new RegistryListSetting(name, desc, Mode.BLOCKS, defaults);
    }

    public static RegistryListSetting items(String name, String desc,
                                            ResourceLocation... defaults) {
        return new RegistryListSetting(name, desc, Mode.ITEMS, defaults);
    }

    public static RegistryListSetting both(String name, String desc,
                                           ResourceLocation... defaults) {
        return new RegistryListSetting(name, desc, Mode.BOTH, defaults);
    }

    public String getName()        { return name; }
    public String getDescription() { return description; }
    public Mode   getMode()        { return mode; }
    public int    size()           { return entries.size(); }

    public Set<ResourceLocation> getEntries() {
        return Collections.unmodifiableSet(entries);
    }

    public boolean contains(ResourceLocation id) { return entries.contains(id); }
    public void add(ResourceLocation id)         { entries.add(id); }
    public void remove(ResourceLocation id)      { entries.remove(id); }

    public void toggle(ResourceLocation id) {
        if (entries.contains(id)) entries.remove(id);
        else entries.add(id);
    }

    public void reset() {
        entries.clear();
        entries.addAll(defaults);
    }
}
