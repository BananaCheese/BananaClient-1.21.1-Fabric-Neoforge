package net.bananacheese.bananaclient.modules;

import java.util.List;

public class CycleSetting {

    private final String name;
    private final String description;
    private final List<String> options;
    private int currentIndex;

    public CycleSetting(String name, String description, String... options) {
        this.name         = name;
        this.description  = description;
        this.options      = List.of(options);
        this.currentIndex = 0;
    }

    public CycleSetting(String name, String description,
                        int defaultIndex, String... options) {
        this(name, description, options);
        this.currentIndex = Math.max(0,
                Math.min(defaultIndex, this.options.size() - 1));
    }

    public String getName()        { return name; }
    public String getDescription() { return description; }
    public String getValue()       { return options.get(currentIndex); }
    public List<String> getOptions() { return options; }
    public int getIndex()          { return currentIndex; }

    public void next() {
        currentIndex = (currentIndex + 1) % options.size();
    }

    public void prev() {
        currentIndex = (currentIndex - 1 + options.size()) % options.size();
    }

    public void set(String value) {
        int idx = options.indexOf(value);
        if (idx >= 0) currentIndex = idx;
    }

    public boolean is(String value) {
        return getValue().equals(value);
    }
}
