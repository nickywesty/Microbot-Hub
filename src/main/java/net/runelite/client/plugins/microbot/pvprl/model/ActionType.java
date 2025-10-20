package net.runelite.client.plugins.microbot.pvprl.model;

/**
 * Types of actions that can be executed
 * Corresponds to NhEnv action heads
 */
public enum ActionType {
    ATTACK(0),              // Head 0: Attack style (no-op, mage, ranged, melee)
    MELEE_TYPE(1),          // Head 1: Melee attack type (basic, special)
    RANGED_TYPE(2),         // Head 2: Ranged attack type (basic, special)
    MAGE_SPELL(3),          // Head 3: Magic spell selection
    POTION(4),              // Head 4: Potion usage
    FOOD(5),                // Head 5: Primary food
    KARAMBWAN(6),           // Head 6: Karambwan
    VENGEANCE(7),           // Head 7: Vengeance cast
    GEAR(8),                // Head 8: Gear switching
    MOVEMENT(9),            // Head 9: Movement
    FARCAST_DISTANCE(10),   // Head 10: Farcast distance
    PRAYER(11);             // Head 11: Prayer switching

    private final int actionHead;

    ActionType(int actionHead) {
        this.actionHead = actionHead;
    }

    public int getActionHead() {
        return actionHead;
    }
}
