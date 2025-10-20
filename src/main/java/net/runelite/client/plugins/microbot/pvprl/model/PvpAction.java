package net.runelite.client.plugins.microbot.pvprl.model;

import lombok.Data;

/**
 * Represents a single action to be executed in-game
 * Parsed from the AI's action vector
 */
@Data
public class PvpAction {
    private final ActionType type;
    private final int value;
    private final ActionPriority priority;
    private final String description;

    public PvpAction(ActionType type, int value, ActionPriority priority, String description) {
        this.type = type;
        this.value = value;
        this.priority = priority;
        this.description = description;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (priority=%s, value=%d)",
            type, description, priority, value);
    }
}
