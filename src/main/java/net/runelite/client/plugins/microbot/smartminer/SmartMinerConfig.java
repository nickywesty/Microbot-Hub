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
            keyName = "mineCopper",
            name = "Mine Copper",
            description = "Mine copper ore",
            position = 0,
            section = oreSection
    )
    default boolean mineCopper() {
        return false;
    }

    @ConfigItem(
            keyName = "mineTin",
            name = "Mine Tin",
            description = "Mine tin ore",
            position = 1,
            section = oreSection
    )
    default boolean mineTin() {
        return false;
    }

    @ConfigItem(
            keyName = "mineClay",
            name = "Mine Clay",
            description = "Mine clay",
            position = 2,
            section = oreSection
    )
    default boolean mineClay() {
        return false;
    }

    @ConfigItem(
            keyName = "mineIron",
            name = "Mine Iron",
            description = "Mine iron ore",
            position = 3,
            section = oreSection
    )
    default boolean mineIron() {
        return true;
    }

    @ConfigItem(
            keyName = "mineSilver",
            name = "Mine Silver",
            description = "Mine silver ore",
            position = 4,
            section = oreSection
    )
    default boolean mineSilver() {
        return false;
    }

    @ConfigItem(
            keyName = "mineCoal",
            name = "Mine Coal",
            description = "Mine coal",
            position = 5,
            section = oreSection
    )
    default boolean mineCoal() {
        return false;
    }

    @ConfigItem(
            keyName = "mineGold",
            name = "Mine Gold",
            description = "Mine gold ore",
            position = 6,
            section = oreSection
    )
    default boolean mineGold() {
        return false;
    }

    @ConfigItem(
            keyName = "mineMithril",
            name = "Mine Mithril",
            description = "Mine mithril ore",
            position = 7,
            section = oreSection
    )
    default boolean mineMithril() {
        return false;
    }

    @ConfigItem(
            keyName = "mineAdamantite",
            name = "Mine Adamantite",
            description = "Mine adamantite ore",
            position = 8,
            section = oreSection
    )
    default boolean mineAdamantite() {
        return false;
    }

    @ConfigItem(
            keyName = "mineRunite",
            name = "Mine Runite",
            description = "Mine runite ore",
            position = 9,
            section = oreSection
    )
    default boolean mineRunite() {
        return false;
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
}
