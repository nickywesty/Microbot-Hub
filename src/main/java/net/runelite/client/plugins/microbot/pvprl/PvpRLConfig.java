package net.runelite.client.plugins.microbot.pvprl;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("pvprl")
public interface PvpRLConfig extends Config {

    @ConfigSection(
        name = "API Connection",
        description = "Python API server connection settings",
        position = 0
    )
    String apiSection = "api";

    @ConfigSection(
        name = "Features",
        description = "Enable/disable specific features",
        position = 1
    )
    String featuresSection = "features";

    @ConfigSection(
        name = "Safety",
        description = "Safety and emergency features",
        position = 2
    )
    String safetySection = "safety";

    @ConfigSection(
        name = "Debug",
        description = "Debug and performance options",
        position = 3
    )
    String debugSection = "debug";

    // API Connection Settings
    @ConfigItem(
        keyName = "apiHost",
        name = "API Host",
        description = "Host address of the Python serve-api server",
        section = apiSection,
        position = 0
    )
    default String apiHost() {
        return "127.0.0.1";
    }

    @ConfigItem(
        keyName = "apiPort",
        name = "API Port",
        description = "Port of the Python serve-api server",
        section = apiSection,
        position = 1
    )
    default int apiPort() {
        return 8888;
    }

    @ConfigItem(
        keyName = "modelName",
        name = "Model Name",
        description = "Name of the RL model to use (must exist in models/ directory)",
        section = apiSection,
        position = 2
    )
    default String modelName() {
        return "GeneralizedNh";
    }

    @ConfigItem(
        keyName = "connectionTimeout",
        name = "Connection Timeout (ms)",
        description = "Timeout for API connection attempts",
        section = apiSection,
        position = 3
    )
    default int connectionTimeout() {
        return 5000;
    }

    @ConfigItem(
        keyName = "requestTimeout",
        name = "Request Timeout (ms)",
        description = "Timeout for API prediction requests",
        section = apiSection,
        position = 4
    )
    default int requestTimeout() {
        return 2000;
    }

    // Feature Toggles
    @ConfigItem(
        keyName = "autoStart",
        name = "Auto-Start on Combat",
        description = "Automatically start bot when attacked in LMS",
        section = featuresSection,
        position = 0
    )
    default boolean autoStart() {
        return true;
    }

    @ConfigItem(
        keyName = "enablePrayerSwitching",
        name = "Enable Prayer Switching",
        description = "Allow AI to switch prayers",
        section = featuresSection,
        position = 1
    )
    default boolean enablePrayerSwitching() {
        return true;
    }

    @ConfigItem(
        keyName = "enableGearSwitching",
        name = "Enable Gear Switching",
        description = "Allow AI to switch gear",
        section = featuresSection,
        position = 2
    )
    default boolean enableGearSwitching() {
        return true;
    }

    @ConfigItem(
        keyName = "enableMovement",
        name = "Enable Movement",
        description = "Allow AI to move the player",
        section = featuresSection,
        position = 3
    )
    default boolean enableMovement() {
        return false; // Disabled by default for safety
    }

    @ConfigItem(
        keyName = "enableAttacking",
        name = "Enable Attacking",
        description = "Allow AI to attack targets",
        section = featuresSection,
        position = 4
    )
    default boolean enableAttacking() {
        return true;
    }

    @ConfigItem(
        keyName = "enableEating",
        name = "Enable Eating",
        description = "Allow AI to eat food and drink potions",
        section = featuresSection,
        position = 5
    )
    default boolean enableEating() {
        return true;
    }

    @ConfigItem(
        keyName = "enableSpecialAttack",
        name = "Enable Special Attack",
        description = "Allow AI to use special attacks",
        section = featuresSection,
        position = 6
    )
    default boolean enableSpecialAttack() {
        return true;
    }

    // Safety Settings
    @ConfigItem(
        keyName = "emergencyLogoutHP",
        name = "Emergency Logout HP",
        description = "Auto-logout if HP drops below this value (0 = disabled)",
        section = safetySection,
        position = 0
    )
    default int emergencyLogoutHP() {
        return 0;
    }

    @ConfigItem(
        keyName = "onlyInPvpAreas",
        name = "Only in PvP Areas",
        description = "Only run bot in designated PvP areas (LMS, PvP worlds, etc.)",
        section = safetySection,
        position = 1
    )
    default boolean onlyInPvpAreas() {
        return true;
    }

    @ConfigItem(
        keyName = "requireTarget",
        name = "Require Target",
        description = "Only run when a valid target is present",
        section = safetySection,
        position = 2
    )
    default boolean requireTarget() {
        return true;
    }

    @ConfigItem(
        keyName = "maxActionsPerTick",
        name = "Max Actions Per Tick",
        description = "Maximum actions to execute per game tick (100 = unlimited for maximum speed)",
        section = safetySection,
        position = 3
    )
    default int maxActionsPerTick() {
        return 100; // UNLIMITED - execute ALL actions every tick (was 20)
    }

    // Debug Settings
    @ConfigItem(
        keyName = "showOverlay",
        name = "Show Debug Overlay",
        description = "Display debug overlay with stats and action queue",
        section = debugSection,
        position = 0
    )
    default boolean showOverlay() {
        return true;
    }

    @ConfigItem(
        keyName = "logActions",
        name = "Log Actions",
        description = "Log all executed actions to console",
        section = debugSection,
        position = 1
    )
    default boolean logActions() {
        return false;
    }

    @ConfigItem(
        keyName = "logObservations",
        name = "Log Observations",
        description = "Log observation vectors to console",
        section = debugSection,
        position = 2
    )
    default boolean logObservations() {
        return false;
    }

    @ConfigItem(
        keyName = "tickDelay",
        name = "Tick Delay Multiplier",
        description = "Slow down execution (1.0 = normal, 2.0 = half speed)",
        section = debugSection,
        position = 3
    )
    default double tickDelay() {
        return 1.0;
    }

    @ConfigItem(
        keyName = "deterministicActions",
        name = "Deterministic Actions",
        description = "Use deterministic action sampling (no randomness)",
        section = debugSection,
        position = 4
    )
    default boolean deterministicActions() {
        return true;
    }
}
