package net.bananacheese.bananaclient.gui.profile;

public class PanelState {

    public int     x;
    public int     y;
    public boolean visible;
    public boolean collapsed;
    public boolean locked;      // ← new

    public PanelState(int x, int y) {
        this.x         = x;
        this.y         = y;
        this.visible   = true;
        this.collapsed = false;
        this.locked    = false;
    }

    public PanelState() { this(20, 20); }

    public PanelState copy() {
        PanelState s = new PanelState();
        s.x         = this.x;
        s.y         = this.y;
        s.visible   = this.visible;
        s.collapsed = this.collapsed;
        s.locked    = this.locked;
        return s;
    }
}