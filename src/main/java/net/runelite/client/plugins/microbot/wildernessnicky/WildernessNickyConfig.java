package net.runelite.client.plugins.microbot.wildernessnicky;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.ConfigInformation;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("wildernessagility")
@ConfigInformation(
    "<h3>🌊 Nickywest Wilderness Agility v2.0.0</h3>" +
    "<b>🎮 Choose Your Play Mode:</b><br>" +
    "• <b>Proactive Only:</b> Scans for PKers, escapes before attack<br>" +
    "• <b>Solo Mode:</b> Instant logout + world hops (max safety)<br>" +
    "• <b>Mass Mode:</b> Wait for hits, clan-friendly thresholds<br>" +
    "<br>" +
    "<b>🛡️ ADVANCED ANTI-PK FEATURES:</b><br>" +
    "• Detects attackable players (skulled OR non-skulled)<br>" +
    "• Safe spot detection - finds safe tiles to logout<br>" +
    "• Skeleton vs PKer detection - different handling<br>" +
    "• Auto-prayers during escape (1-tick projectile switching)<br>" +
    "• Logout priority: tries 3s before running to bank<br>" +
    "• Eats food automatically while escaping<br>" +
    "<br>" +
    "<b>💰 Other Features:</b><br>" +
    "• Real-time Looting Bag Tracking (GUI shows contents)<br>" +
    "• Auto-regear on death or missing coins<br>" +
    "• Entrance fee validation & management<br>" +
    "<br>" +
    "<b>Quick Start:</b><br>" +
    "• Inventory: 150k coins, knife, looting bag, phoenix necklace<br>" +
    "• Set Play Mode to match your playstyle<br>" +
    "• Enable 'Start at Course' if already at wilderness agility<br>"
)
public interface WildernessNickyConfig extends Config {

    // === Core Settings ===
    @ConfigSection(
        name = "⚙️ Core Settings",
        description = "Essential plugin configuration",
        position = 0
    )
    String coreSection = "coreSection";

    @ConfigItem(
        keyName = "startAtCourse",
        name = "Start at Course?",
        description = "<html>Skip banking and walk directly to course.<br>" +
                      "<b>⚠️ IMPORTANT:</b> Only enable if you ALREADY PAID the 150k entrance fee!<br>" +
                      "If not paid, bot will detect 0 value loot and auto-go to bank.</html>",
        position = 1,
        section = coreSection
    )
    default boolean startAtCourse() { return false; }

    // === Escape Settings ===
    @ConfigSection(
        name = "🛡️ Escape & Safety",
        description = "PKer detection, escape triggers, and prayer switching",
        position = 10
    )
    String escapeSection = "escapeSection";

    enum PlayMode {
        PROACTIVE_ONLY("Proactive Detection Only"),
        SOLO("Solo Mode - Instant Logout"),
        MASS("Mass Mode - Wait for Hits");

        private final String displayName;

        PlayMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @ConfigItem(
        keyName = "playMode",
        name = "🎮 Play Mode",
        description = "<html><b>Choose your playstyle:</b><br>" +
                      "<b>Proactive Only:</b> Scans for PKers, escapes before attack<br>" +
                      "<b>Solo Mode:</b> Instant logout on ANY player, world hops 3x<br>" +
                      "<b>Mass Mode:</b> Wait for hits, clan-friendly thresholds</html>",
        position = 11,
        section = escapeSection
    )
    default PlayMode playMode() { return PlayMode.PROACTIVE_ONLY; }

    @ConfigItem(
        keyName = "phoenixEscape",
        name = "Phoenix Necklace Escape",
        description = "Emergency escape to Mage Bank: equips phoenix necklace, climbs rocks, opens gates when necklace is missing.",
        position = 12,
        section = escapeSection
    )
    default boolean phoenixEscape() { return false; }

    @ConfigItem(
        keyName = "leaveAtHealthPercent",
        name = "Escape at Health %",
        description = "Trigger emergency escape when health drops below this %. 0 = disabled.",
        position = 13,
        section = escapeSection
    )
    @Range(min = 0, max = 100)
    default int leaveAtHealthPercent() { return 0; }

    // === MASS MODE SPECIFIC OPTIONS ===
    @ConfigItem(
        keyName = "clanMemberHitThreshold",
        name = "  ⚔️ Clan Hit Threshold",
        description = "<html><b>MASS MODE ONLY:</b><br>Hits from FC/clan members before escaping.<br>Higher = more lenient for mass runs</html>",
        position = 14,
        section = escapeSection,
        hidden = true
    )
    @Range(min = 2, max = 20)
    default int clanMemberHitThreshold() { return 6; }

    @ConfigItem(
        keyName = "clanMemberHitThresholdVisible",
        name = "",
        description = "",
        hidden = true
    )
    default boolean clanMemberHitThresholdVisible() {
        return playMode() == PlayMode.MASS;
    }

    @ConfigItem(
        keyName = "nonClanHitThreshold",
        name = "  ⚔️ Non-Clan Hit Threshold",
        description = "<html><b>MASS MODE ONLY:</b><br>Hits from NON-clan players before escaping.<br>Lower = safer from PKers<br>Default: 4 hits</html>",
        position = 15,
        section = escapeSection,
        hidden = true
    )
    @Range(min = 1, max = 10)
    default int nonClanHitThreshold() { return 4; }

    @ConfigItem(
        keyName = "nonClanHitThresholdVisible",
        name = "",
        description = "",
        hidden = true
    )
    default boolean nonClanHitThresholdVisible() {
        return playMode() == PlayMode.MASS;
    }

    @ConfigItem(
        keyName = "massWorld",
        name = "  🌍 Mass World (Stay On)",
        description = "<html><b>MASS MODE ONLY:</b><br>Stay on this world during course runs (no hopping).<br>If Random, uses any world.<br>Recommended: Pick a consistent world for mass runs</html>",
        position = 16,
        section = escapeSection,
        hidden = true
    )
    default BankWorldOption massWorld() { return BankWorldOption.Random; }

    @ConfigItem(
        keyName = "massWorldVisible",
        name = "",
        description = "",
        hidden = true
    )
    default boolean massWorldVisible() {
        return playMode() == PlayMode.MASS;
    }

    @ConfigItem(
        keyName = "massHopWorlds",
        name = "  🌍 Mass Hop Worlds (Custom)",
        description = "<html><b>MASS MODE ONLY:</b><br>Comma-separated world numbers to hop between (e.g., 416,417,418).<br>Leave empty to use dropdown selection above.<br><b>This overrides the dropdown if set!</b></html>",
        position = 17,
        section = escapeSection,
        hidden = true
    )
    default String massHopWorlds() { return ""; }

    @ConfigItem(
        keyName = "massHopWorldsVisible",
        name = "",
        description = "",
        hidden = true
    )
    default boolean massHopWorldsVisible() {
        return playMode() == PlayMode.MASS;
    }

    @ConfigItem(
        keyName = "massReloginDelayMin",
        name = "  ⏱️ Relogin Delay Min (sec)",
        description = "<html><b>MASS MODE ONLY:</b><br>Minimum seconds to wait before re-logging after logout from attack.<br>Default: 30 seconds</html>",
        position = 17,
        section = escapeSection,
        hidden = true
    )
    @Range(min = 15, max = 300)
    default int massReloginDelayMin() { return 30; }

    @ConfigItem(
        keyName = "massReloginDelayMinVisible",
        name = "",
        description = "",
        hidden = true
    )
    default boolean massReloginDelayMinVisible() {
        return playMode() == PlayMode.MASS;
    }

    @ConfigItem(
        keyName = "massReloginDelayMax",
        name = "  ⏱️ Relogin Delay Max (sec)",
        description = "<html><b>MASS MODE ONLY:</b><br>Maximum seconds to wait before re-logging after logout from attack.<br>Default: 60 seconds</html>",
        position = 18,
        section = escapeSection,
        hidden = true
    )
    @Range(min = 15, max = 300)
    default int massReloginDelayMax() { return 60; }

    @ConfigItem(
        keyName = "massReloginDelayMaxVisible",
        name = "",
        description = "",
        hidden = true
    )
    default boolean massReloginDelayMaxVisible() {
        return playMode() == PlayMode.MASS;
    }

    @ConfigItem(
        keyName = "soloHopWorlds",
        name = "🌍 Solo Hop Worlds",
        description = "<html><b>SOLO MODE ONLY:</b><br>Comma-separated world numbers to hop to after logout (e.g., 301,302,303,304,305).<br>Script will cycle through this list when escaping from PKers.<br>Leave empty for random world selection.</html>",
        position = 19,
        section = escapeSection
    )
    default String soloHopWorlds() { return ""; }

    @ConfigItem(
        keyName = "enableProactivePlayerDetection",
        name = "🔍 Proactive PKer Detection",
        description = "<html>Scan for PKers every 5 seconds.<br>Escape BEFORE being attacked if threatening player within 15 tiles.<br><b>Note:</b> Disabled in Mass Mode (uses hit detection instead)</html>",
        position = 19,
        section = escapeSection
    )
    default boolean enableProactivePlayerDetection() { return true; }

    @ConfigItem(
        keyName = "useProjectilePrayerSwitching",
        name = "🎯 Projectile Prayer Switching",
        description = "<html><b>1-tick accurate</b> prayer switching based on incoming projectiles.<br>" +
                      "More accurate than weapon-based (detects actual attacks).<br>" +
                      "Supports ALL wilderness magic, ranged, and melee projectiles.</html>",
        position = 20,
        section = escapeSection
    )
    default boolean useProjectilePrayerSwitching() { return true; }

    // === WIKI SAFE ZONE COORDINATES ===
    @ConfigItem(
        keyName = "iceWarriorsSafeSpot",
        name = "🧊 Ice Warriors Safe Spot",
        description = "<html><b>Ice Warriors Zone (West Route):</b><br>WorldPoint coordinates for safe logout spot (format: X,Y,Z).<br>Default: 2970,3940,0<br>Use RuneLite tile marker to find exact coordinates.</html>",
        position = 21,
        section = escapeSection
    )
    default String iceWarriorsSafeSpot() { return "2970,3940,0"; }

    @ConfigItem(
        keyName = "piratesSafeSpot",
        name = "🏴‍☠️ Pirates' Hideout Safe Spot",
        description = "<html><b>Pirates' Hideout (East Route):</b><br>WorldPoint coordinates for safe logout spot (format: X,Y,Z).<br>Default: 3045,3960,0<br>Requires lockpick to enter.</html>",
        position = 22,
        section = escapeSection
    )
    default String piratesSafeSpot() { return "3045,3960,0"; }

    @ConfigItem(
        keyName = "dungeonSafeSpot",
        name = "🕳️ Deep Dungeon Safe Spot",
        description = "<html><b>Deep Wilderness Dungeon (East Route):</b><br>WorldPoint coordinates for underground safe spot (format: X,Y,Z).<br>Default: 3045,10325,0 (Z=0 for underground)<br>Requires Wilderness Medium Diary.</html>",
        position = 23,
        section = escapeSection
    )
    default String dungeonSafeSpot() { return "3045,10325,0"; }

    // === Looting Settings ===
    @ConfigSection(
        name = "💰 Looting & Banking",
        description = "Real-time loot tracking, banking thresholds, and ticket management",
        position = 20
    )
    String lootingSection = "lootingSection";

    @ConfigItem(
        keyName = "leaveAtValue",
        name = "📦 Bank at Looting Bag Value",
        description = "<html><b>NEW v2.0:</b> Real-time looting bag tracking!<br>" +
                      "Triggers banking when looting bag reaches this value.<br>" +
                      "GUI shows current value and top 3 items.</html>",
        position = 21,
        section = lootingSection
    )
    @Range(min = 1, max = 50_000_000)
    default int leaveAtValue() { return 5_000_000; }

    @ConfigItem(
        keyName = "bankAfterDispensers",
        name = "Bank After Dispensers",
        description = "Bank after this many dispenser loots. 0 = disabled.",
        position = 22,
        section = lootingSection
    )
    @Range(min = 0, max = 1000)
    default int bankAfterDispensers() { return 0; }

    @ConfigItem(
        keyName = "useTicketsWhen",
        name = "Use Tickets When",
        description = "Spend tickets if you have this many in inventory.",
        position = 23,
        section = lootingSection
    )
    @Range(min = 1, max = 200_000)
    default int useTicketsWhen() { return 101; }

    @ConfigItem(
        keyName = "minimumAnglerfish",
        name = "Maximum Anglerfish",
        description = "Maximum anglerfish to keep in inventory. Any above this amount will be eaten.",
        position = 24,
        section = lootingSection
    )
    @Range(min = 1, max = 10)
    default int minimumAnglerfish() { return 1; }

    @ConfigItem(
        keyName = "minimumKarambwan",
        name = "Maximum Karambwan",
        description = "Maximum karambwan to keep in inventory. Any above this amount will be eaten.",
        position = 25,
        section = lootingSection
    )
    @Range(min = 1, max = 10)
    default int minimumKarambwan() { return 1; }

    @ConfigItem(
        keyName = "minimumMantaRay",
        name = "Maximum Manta Ray",
        description = "Maximum manta ray to keep in inventory. Any above this amount will be eaten.",
        position = 26,
        section = lootingSection
    )
    @Range(min = 1, max = 10)
    default int minimumMantaRay() { return 2; }

    @ConfigItem(
        keyName = "minimumRestorePot",
        name = "Maximum Restore Pot",
        description = "Maximum restore potions to keep in inventory. Any above this amount will be dropped.",
        position = 27,
        section = lootingSection
    )
    @Range(min = 1, max = 10)
    default int minimumRestorePot() { return 2; }

    enum DropLocationOption {
        AfterDispenser("After Dispenser"),
        BeforeLog("Before Log Obstacle"),
        Random("Random");

        private final String displayName;

        DropLocationOption(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @ConfigItem(
        keyName = "dropLocation",
        name = "Drop/Eat Items Location",
        description = "Choose when to drop/eat items to make inventory space: after looting dispenser, before log obstacle, or randomly.",
        position = 28,
        section = lootingSection
    )
    default DropLocationOption dropLocation() { return DropLocationOption.Random; }

    @ConfigItem(
        keyName = "maxInventorySize",
        name = "Maximum Inventory Size",
        description = "Maximum inventory size before dropping/eating items. Lower values = more aggressive inventory management.",
        position = 29,
        section = lootingSection
    )
    @Range(min = 10, max = 27)
    default int maxInventorySize() { return 26; }

    // === Banking Settings ===
    @ConfigSection(
        name = "Banking Settings",
        description = "Settings for world hopping and item preparation during banking.",
        position = 30
    )
    String bankingSection = "bankingSection";

    @ConfigItem(
        keyName = "enablePlayerMonitor",
        name = "Enable Player Monitor?",
        description = "<html><b>⚠️ WARNING:</b> Player Monitor can interfere with logout attempts!<br>" +
                      "Only enable if you have the Player Monitor plugin installed.<br>" +
                      "<b>Leave DISABLED for most users.</b><br><br>" +
                      "Enables Player Monitor during banking/escape for extra PKer detection.</html>",
        position = 31,
        section = bankingSection
    )
    default boolean enablePlayerMonitor() { return false; }

    @ConfigItem(
        keyName = "useIcePlateauTp",
        name = "Withdraw Ice Plateau TP",
        description = "Take teleport from bank during banking (if used for return).",
        position = 32,
        section = bankingSection
    )
    default boolean useIcePlateauTp() { return true; }

    @ConfigItem(
        keyName = "withdrawCoins",
        name = "Withdraw Coins",
        description = "If enabled, the script will withdraw coins during banking.",
        position = 33,
        section = bankingSection
    )
    default boolean withdrawCoins() { return true; }

    @ConfigItem(
        keyName = "withdrawKnife",
        name = "Withdraw Knife",
        description = "If enabled, the script will withdraw a knife during banking.",
        position = 34,
        section = bankingSection
    )
    default boolean withdrawKnife() { return true; }

    @ConfigItem(
        keyName = "withdrawLootingBag",
        name = "Withdraw Looting Bag",
        description = "If enabled, the script will withdraw a looting bag during banking.",
        position = 35,
        section = bankingSection
    )
    default boolean withdrawLootingBag() { return true; }

    enum VenomProtectionOption {
        None,
        AntidotePlusPlus(5952, "Antidote++"),
        AntiVenom(12905, "Anti-venom"),
        AntiVenomPlus(12913, "Anti-venom+"),
        ExtendedAntiVenomPlus(29824, "Extd anti-venom+"),
        AraxyteVenomSack(29784, "Venom sack");

        private final int itemId;
        private final String displayName;

        VenomProtectionOption() {
            this.itemId = -1; // None option
            this.displayName = "None";
        }

        VenomProtectionOption(int itemId, String displayName) {
            this.itemId = itemId;
            this.displayName = displayName;
        }

        public int getItemId() {
            return itemId;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @ConfigItem(
        keyName = "withdrawVenomProtection",
        name = "Withdraw Venom Protection",
        description = "Withdraw one venom protection item during banking.",
        position = 36,
        section = bankingSection
    )
    default VenomProtectionOption withdrawVenomProtection() { return VenomProtectionOption.None; }

    @ConfigItem(
        keyName = "enableWorldHop",
        name = "Enable World Hop",
        description = "Hop to alternate worlds during banking for anti-ban.",
        position = 37,
        section = bankingSection
    )
    default boolean enableWorldHop() { return true; }

    @ConfigItem(
        keyName = "leaveFcOnWorldHop",
        name = "Leave FC?",
        description = "Leave friends chat when world hopping. Disable if you don't use FC.",
        position = 38,
        section = bankingSection
    )
    default boolean leaveFcOnWorldHop() { return true; }

    enum BankWorldOption {
        Random,
        W303, W304, W305, W306, W307, W309, W310, W311, W312, W313, W314, W315, W317, W320,
        W321, W322, W323, W324, W325, W327, W328, W329, W330, W331, W332, W333, W334, W336,
        W337, W338, W339, W340, W341, W342, W343, W344, W346, W347, W348, W350, W351,
        W352, W354, W355, W356, W357, W358, W359, W360, W362, W365, W367, W368, W369, W370,
        W371, W374, W375, W376, W377, W378, W385, W386, W387, W388, W389, W394, W395, W421,
        W422, W423, W424, W425, W426, W438, W439, W440, W441, W443, W444, W445, W446, W458,
        W459, W463, W464, W465, W466, W474, W477, W478, W479, W480, W481, W482, W484, W485,
        W486, W487, W488, W489, W490, W491, W492, W493, W494, W495, W496, W505, W506, W507,
        W508, W509, W510, W511, W512, W513, W514, W515, W516, W517, W518, W519, W520, W521,
        W522, W523, W524, W525, W529, W531, W532, W533, W534, W535, W567, W570, W573, W578
    }

    @ConfigItem(
        keyName = "bankWorld1",
        name = "Bank World #1",
        description = "First world to hop to for banking.",
        position = 39,
        section = bankingSection
    )
    default BankWorldOption bankWorld1() { return BankWorldOption.Random; }

    @ConfigItem(
        keyName = "bankWorld2",
        name = "Bank World #2",
        description = "Second world to hop to for banking.",
        position = 40,
        section = bankingSection
    )
    default BankWorldOption bankWorld2() { return BankWorldOption.Random; }

    @ConfigItem(
        keyName = "swapBack",
        name = "Swap back to original world?",
        description = "Return to the original world after banking.",
        position = 41,
        section = bankingSection
    )
    default boolean swapBack() { return true; }

    @ConfigItem(
        keyName = "joinFc",
        name = "Join fc?",
        description = "If enabled, the script will join the friends chat after banking/world hop.",
        position = 42,
        section = bankingSection
    )
    default boolean joinFc() { return true; }

    @ConfigItem(
        keyName = "fcChannel",
        name = "FC Name",
        description = "The friends chat channel to join after banking/world hop (if enabled).",
        position = 43,
        section = bankingSection
    )
    default String fcChannel() { return "agility fc"; }

    @ConfigItem(
        keyName = "bankNow",
        name = "Force Bank Next Loot",
        description = "Immediately bank on next dispenser loot, regardless of thresholds.",
        position = 44,
        section = bankingSection
    )
    default boolean bankNow() { return false; }

    // === Death Settings ===
    @ConfigSection(
        name = "Death Settings",
        description = "Settings for handling player death.",
        position = 40
    )
    String deathSection = "deathSection";

    @ConfigItem(
        keyName = "logoutAfterDeath",
        name = "Log Out After Death",
        description = "If enabled, the script will log out after dying instead of stopping or running back.",
        position = 41,
        section = deathSection
    )
    default boolean logoutAfterDeath() { return true; }

    @ConfigItem(
        keyName = "runBack",
        name = "Run Back After Death",
        description = "If enabled, script will walk back from death and resume.",
        position = 42,
        section = deathSection
    )
    default boolean runBack() { return false; }

    // === Fail-Safe Settings ===
    @ConfigSection(
        name = "Fail-Safe Settings",
        description = "Timeouts, recovery, and debug options for failed interactions.",
        position = 50,
        closedByDefault = true
    )
    String failSafeSection = "failSafeSection";

    @ConfigItem(
        keyName = "failTimeoutMs",
        name = "Animation Fail Timeout (ms)",
        description = "If no animation starts after interacting, retry after this timeout.",
        position = 201,
        section = failSafeSection
    )
    @Range(min = 5000, max = 15000)
    default int failTimeoutMs() { return 9500; }

    @ConfigItem(
        keyName = "debugMode",
        name = "Debug Mode",
        description = "Enable debug mode to start the script in a specific state.",
        position = 210,
        section = failSafeSection
    )
    default boolean debugMode() { return false; }

    enum DebugStateOption {
        INIT, START, PIPE, ROPE, STONES, LOG, ROCKS, DISPENSER, CONFIG_CHECKS,
        WORLD_HOP_1, WORLD_HOP_2, WALK_TO_LEVER, INTERACT_LEVER, BANKING, POST_BANK_CONFIG, WALK_TO_COURSE, SWAP_BACK, PIT_RECOVERY, EMERGENCY_ESCAPE
    }

    @ConfigItem(
        keyName = "debugStartState",
        name = "Start in which state",
        description = "State to start in when Debug Mode is enabled.",
        position = 211,
        section = failSafeSection
    )
    default DebugStateOption debugStartState() { return DebugStateOption.START; }

    @ConfigItem(
        keyName = "debugStartStateVisible",
        name = "Show Debug Start State",
        description = "Show the debug start state dropdown only if debug mode is enabled.",
        hidden = true
    )
    default boolean debugStartStateVisible() { return debugMode(); }
}
