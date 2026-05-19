package net.bananacheese.bananaclient.modules;

public class ModuleSetting<T> {

    private final String name;
    private final String description;
    private T value;
    private final T defaultValue;

    // Optional min/max for numeric sliders — null means unconstrained
    private final T min;
    private final T max;

    public ModuleSetting(String name, String description, T defaultValue) {
        this(name, description, defaultValue, null, null);
    }

    public ModuleSetting(String name, String description, T defaultValue, T min, T max) {
        this.name         = name;
        this.description  = description;
        this.value        = defaultValue;
        this.defaultValue = defaultValue;
        this.min          = min;
        this.max          = max;
    }

    public ModuleSetting(String name, T defaultValue) {
        this(name, "", defaultValue, null, null);
    }

    public ModuleSetting(String name, T defaultValue, T min, T max) {
        this(name, "", defaultValue, min, max);
    }

    public String getName()        { return name; }
    public String getDescription() { return description; }
    public T      getValue()       { return value; }
    public T      getDefault()     { return defaultValue; }
    public T      getMin()         { return min; }
    public T      getMax()         { return max; }
    public boolean hasBounds()     { return min != null && max != null; }

    public void setValue(T value)  { this.value = value; }
    public void resetToDefault()   { this.value = defaultValue; }

    public boolean isBoolean() { return defaultValue instanceof Boolean; }
    public boolean isInteger() { return defaultValue instanceof Integer; }
    public boolean isFloat()   { return defaultValue instanceof Float; }
    public boolean isColor()   { return defaultValue instanceof Integer && name.toLowerCase().contains("color"); }
    public boolean isString()  { return defaultValue instanceof String; }

    /** Clamps a float value to [min, max] if bounds are set */
    @SuppressWarnings("unchecked")
    public float clampFloat(float v) {
        if (!hasBounds() || !isFloat()) return v;
        float lo = (Float) min;
        float hi = (Float) max;
        return Math.max(lo, Math.min(hi, v));
    }

    @SuppressWarnings("unchecked")
    public int clampInt(int v) {
        if (!hasBounds() || !isInteger()) return v;
        int lo = (Integer) min;
        int hi = (Integer) max;
        return Math.max(lo, Math.min(hi, v));
    }
}