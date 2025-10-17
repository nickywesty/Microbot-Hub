package net.runelite.client.plugins.microbot.smartminer;

import net.runelite.client.config.*;
import net.runelite.client.plugins.microbot.smartminer.enums.MiningLocation;
import net.runelite.client.plugins.microbot.smartminer.enums.OreType;

@ConfigGroup("SmartMiner")
@ConfigInformation("<h2>Smart Miner</h2>" +
        "<h3>Version: 1.0.0</h3>" +
        "<p>A comprehensive mining bot with webwalker, preset locations, and advanced features.</p>" +
        "<ul>" +
        "<li><strong>Auto-Walk:</strong> Automatically walks to the nearest bank to get your best pickaxe</li>" +
        "<li><strong>Preset Locations:</strong> Choose from pre-configured mining locations</li>" +
        "<li><strong>Smart Mining:</strong> Mines rocks within your configured radius</li>" +
        "<li><strong>Rock Highlighting:</strong> Highlights available rocks in your mining radius</li>" +
        "<li><strong>Antiban:</strong> Natural mouse movements and human-like behavior</li>" +
        "</ul>")
public interface SmartMinerConfig extends Config {

    @ConfigSection(
            name = "General Settings",
            description = "General mining configuration",
            position = 0
    )
    String generalSection = "general";

    @ConfigSection(
            name = "Location Settings",
            description = "Mining location configuration",
            position = 1
    )
    String locationSection = "location";

    @ConfigSection(
            name = "Ore Selection",
            description = "Select which ores to mine",
            position = 2
    )
    String oreSection = "ore";

    @ConfigSection(
            name = "Banking & Dropping",
            description = "Configure banking and dropping behavior",
            position = 3
    )
    String bankingSection = "banking";

    @ConfigSection(
            name = "Antiban Settings",
            description = "Configure human-like behavior to avoid detection",
            position = 4
    )
    String antibanSection = "antiban";

    @ConfigSection(
            name = "Debug & Display",
            description = "Advanced debugging and display options",
            position = 5
    )
    String debugSection = "debug";

    // ===== GENERAL SETTINGS =====

    @ConfigItem(
            keyName = "miningRadius",
            name = "Mining Radius",
            description = "Maximum distance (in tiles) to mine rocks from starting position",
            position = 0,
            section = generalSection
    )
    @Range(min = 1, max = 20)
    default int miningRadius() {
        return 10;
    }

    @ConfigItem(
            keyName = "autoGetPickaxe",
            name = "Auto-Get Pickaxe",
            description = "Automatically walk to bank and get best pickaxe if not in inventory",
            position = 1,
            section = generalSection
    )
    default boolean autoGetPickaxe() {
        return true;
    }

    @ConfigItem(
            keyName = "hopOnPlayersNearby",
            name = "Hop on Players Nearby",
            description = "Hop worlds if too many players are mining nearby (0 = disabled)",
            position = 2,
            section = generalSection
    )
    @Range(min = 0, max = 10)
    default int hopOnPlayersNearby() {
        return 0;
    }

    // ===== LOCATION SETTINGS =====

    @ConfigItem(
            keyName = "usePresetLocation",
            name = "Use Preset Location",
            description = "Use a preset mining location instead of current position",
            position = 0,
            section = locationSection
    )
    default boolean usePresetLocation() {
        return false;
    }

    @ConfigItem(
            keyName = "miningLocation",
            name = "Mining Location",
            description = "Select a preset mining location (only used if 'Use Preset Location' is enabled)",
            position = 1,
            section = locationSection
    )
    default MiningLocation miningLocation() {
        return MiningLocation.VARROCK_WEST;
    }

    // ===== ORE SELECTION =====

    @ConfigItem(
            keyName = "oreType",
            name = "Ore Type",
            description = "Select which ore type to mine",
            position = 0,
            section = oreSection
    )
    default OreType oreType() {
        return OreType.IRON;
    }

    // ===== BANKING & DROPPING =====

    @ConfigItem(
            keyName = "useBank",
            name = "Use Bank",
            description = "Bank ores instead of dropping them",
            position = 0,
            section = bankingSection
    )
    default boolean useBank() {
        return false;
    }

    @ConfigItem(
            keyName = "dropOres",
            name = "Drop Ores",
            description = "Drop ores when inventory is full (only works if 'Use Bank' is disabled)",
            position = 1,
            section = bankingSection
    )
    default boolean dropOres() {
        return true;
    }

    @ConfigItem(
            keyName = "itemsToKeep",
            name = "Items to Keep",
            description = "Items to keep in inventory (comma separated)",
            position = 2,
            section = bankingSection
    )
    default String itemsToKeep() {
        return "pickaxe";
    }

    // ===== ANTIBAN SETTINGS =====

    @ConfigItem(
            keyName = "naturalMouse",
            name = "Natural Mouse",
            description = "Simulates human-like mouse movements with curves and variations",
            position = 0,
            section = antibanSection
    )
    default boolean naturalMouse() {
        return true;
    }

    @ConfigItem(
            keyName = "moveMouseOffScreen",
            name = "Move Mouse Off Screen",
            description = "Occasionally moves mouse off-screen like a real player",
            position = 1,
            section = antibanSection
    )
    default boolean moveMouseOffScreen() {
        return true;
    }

    @ConfigItem(
            keyName = "moveMouseRandomly",
            name = "Move Mouse Randomly",
            description = "Randomly moves mouse to different areas of the screen",
            position = 2,
            section = antibanSection
    )
    default boolean moveMouseRandomly() {
        return true;
    }

    @ConfigItem(
            keyName = "actionCooldowns",
            name = "Action Cooldowns",
            description = "Adds random delays between actions to simulate thinking time",
            position = 3,
            section = antibanSection
    )
    default boolean actionCooldowns() {
        return true;
    }

    @ConfigItem(
            keyName = "microBreaks",
            name = "Micro Breaks",
            description = "Takes random micro-breaks during activity (1-3 seconds)",
            position = 4,
            section = antibanSection
    )
    default boolean microBreaks() {
        return true;
    }

    @ConfigItem(
            keyName = "simulateFatigue",
            name = "Simulate Fatigue",
            description = "Gradually slows down actions over time like a human getting tired",
            position = 5,
            section = antibanSection
    )
    default boolean simulateFatigue() {
        return false;
    }

    @ConfigItem(
            keyName = "simulateAttentionSpan",
            name = "Simulate Attention Span",
            description = "Varies focus and reaction times based on playtime duration",
            position = 6,
            section = antibanSection
    )
    default boolean simulateAttentionSpan() {
        return true;
    }

    @ConfigItem(
            keyName = "behavioralVariability",
            name = "Behavioral Variability",
            description = "Introduces random variations in behavior patterns",
            position = 7,
            section = antibanSection
    )
    default boolean behavioralVariability() {
        return true;
    }

    @ConfigItem(
            keyName = "nonLinearIntervals",
            name = "Non-Linear Intervals",
            description = "Uses non-predictable timing intervals between actions",
            position = 8,
            section = antibanSection
    )
    default boolean nonLinearIntervals() {
        return true;
    }

    @ConfigItem(
            keyName = "profileSwitching",
            name = "Profile Switching",
            description = "Periodically switches behavior profiles for more realistic patterns",
            position = 9,
            section = antibanSection
    )
    default boolean profileSwitching() {
        return true;
    }

    @ConfigItem(
            keyName = "simulateMistakes",
            name = "Simulate Mistakes",
            description = "Occasionally makes human-like mistakes (misclicks, wrong targets)",
            position = 10,
            section = antibanSection
    )
    default boolean simulateMistakes() {
        return false;
    }

    @ConfigItem(
            keyName = "usePlayStyle",
            name = "Use Play Style",
            description = "Adapts behavior based on configured play style preferences",
            position = 11,
            section = antibanSection
    )
    default boolean usePlayStyle() {
        return true;
    }

    // ===== DEBUG & DISPLAY SETTINGS =====

    @ConfigItem(
            keyName = "debugMode",
            name = "Debug Mode",
            description = "Shows detailed debug overlay with antiban activity tracking",
            position = 0,
            section = debugSection
    )
    default boolean debugMode() {
        return false;
    }

    @ConfigItem(
            keyName = "showXpTracker",
            name = "Show XP Tracker",
            description = "Displays XP/hour and progress to next level",
            position = 1,
            section = debugSection
    )
    default boolean showXpTracker() {
        return true;
    }

    @ConfigItem(
            keyName = "showSessionStats",
            name = "Show Session Stats",
            description = "Displays runtime, ores mined, trips completed",
            position = 2,
            section = debugSection
    )
    default boolean showSessionStats() {
        return true;
    }

    @ConfigItem(
            keyName = "randomizeLocation",
            name = "Randomize Mining Spot",
            description = "Randomly picks nearby mining locations of same type when banking (adds variety)",
            position = 0,
            section = locationSection
    )
    default boolean randomizeLocation() {
        return false;
    }
}
