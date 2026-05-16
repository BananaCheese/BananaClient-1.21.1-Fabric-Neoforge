package net.bananacheese.bananaclient.modules;

public class ModuleSetting<T> {

    private final String name;
    private final String description;
    private T value;
    private final T defaultValue;

    public ModuleSetting(String name, String description, T defaultValue) {
        this.name = name;
        this.description = description;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    // No description shorthand
    public ModuleSetting(String name, T defaultValue) {
        this(name, "", defaultValue);
    }

    public String getName()        { return name; }
    public String getDescription() { return description; }
    public T      getValue()       { return value; }
    public T      getDefault()     { return defaultValue; }

    public void setValue(T value)  { this.value = value; }
    public void resetToDefault()   { this.value = defaultValue; }

    // Type helpers used by the settings panel renderer later
    public boolean isBoolean() { return defaultValue instanceof Boolean; }
    public boolean isInteger() { return defaultValue instanceof Integer; }
    public boolean isFloat()   { return defaultValue instanceof Float; }
    public boolean isColor()   { return defaultValue instanceof Integer && name.toLowerCase().contains("color"); }
    public boolean isString()  { return defaultValue instanceof String; }
}