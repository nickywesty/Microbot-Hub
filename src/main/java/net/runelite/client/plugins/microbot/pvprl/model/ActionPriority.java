package net.runelite.client.plugins.microbot.pvprl.model;

/**
 * Priority levels for action execution
 * Used by ActionQueue to determine execution order when >12 actions requested
 */
public enum ActionPriority {
    CRITICAL(0),    // Prayer switches, eating when low HP
    HIGH(1),        // Offensive prayers, special attacks, combo food
    MEDIUM(2),      // Gear switches, attack actions
    LOW(3);         // Movement, misc actions

    private final int value;

    ActionPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
