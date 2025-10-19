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
    "<b>🛡️ ADVANCED ANTI-PK FEATURES:</b><br>" +
    "• Detects attackable players (skulled OR non-skulled)<br>" +
    "• Safe spot detection - finds safe tiles to logout<br>" +
    "• Skeleton vs PKer detection - different handling<br>" +
    "• Auto-prayers during escape (1-tick projectile switching)<br>" +
    "• Logout priority: tries 3s before running to bank<br>" +
    "• Eats food automatically while escaping<br>" +
    "• World hops 3x in solo mode to find empty course<br>" +
    "<br>" +
    "<b>💰 Other Features:</b><br>" +
    "• Real-time Looting Bag Tracking (GUI shows contents)<br>" +
    "• Auto-regear on death or missing coins<br>" +
    "• Entrance fee validation & management<br>" +
    "• Clan vs non-clan hit detection<br>" +
    "<br>" +
    "<b>Quick Start:</b><br>" +
    "• Inventory: 150k coins, knife, looting bag, phoenix necklace<br>" +
    "• Enable 'Start at Course' if already at wilderness agility<br>" +
    "• Solo mode: Enable for world hopping safety<br>" +
    "• Mass mode: Disable solo, set clan hit threshold higher<br>"
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
        description = "Skip banking and walk directly to course. Use if already paid entrance fee or just farming tickets.",
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

    @ConfigItem(
        keyName = "soloMode",
        name = "🌍 Solo Mode (World Hopping)",
        description = "<html><b>ADVANCED v2.0:</b> Maximum anti-PK protection!<br>" +
                      "• Detects ANY attackable player (skulled or not)<br>" +
                      "• Skeleton combat: Finds safe spot to logout<br>" +
                      "• PKer detected: Instant logout attempt<br>" +
                      "• If in combat: Runs to safe spot, eats, prays<br>" +
                      "• World hops 3x to find empty course<br>" +
                      "• Stops plugin if no empty world found</html>",
        position = 11,
        section = escapeSection
    )
    default boolean soloMode() { return false; }

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

    @ConfigItem(
        keyName = "waitForHitBeforeEscape",
        name = "⚔️ Wait for Hit (Mass Mode)",
        description = "<html><b>MASS-FRIENDLY:</b> Only escape after taking PvP damage from a player.<br>" +
                      "Ignores agility fail damage. Perfect for mass agility runs.</html>",
        position = 14,
        section = escapeSection
    )
    default boolean waitForHitBeforeEscape() { return false; }

    @ConfigItem(
        keyName = "clanMemberHitThreshold",
        name = "Clan Hit Threshold",
        description = "Hits from FC/clan members before escaping (higher = more lenient for mass)",
        position = 15,
        section = escapeSection
    )
    @Range(min = 2, max = 20)
    default int clanMemberHitThreshold() { return 6; }

    @ConfigItem(
        keyName = "nonClanHitThreshold",
        name = "Non-Clan Hit Threshold",
        description = "Hits from NON-clan players before escaping (lower = safer from PKers)",
        position = 16,
        section = escapeSection
    )
    @Range(min = 1, max = 10)
    default int nonClanHitThreshold() { return 2; }

    @ConfigItem(
        keyName = "enableProactivePlayerDetection",
        name = "🔍 Proactive PKer Detection",
        description = "Scan for PKers every 5 seconds. Escape BEFORE being attacked if threatening player within 15 tiles.",
        position = 17,
        section = escapeSection
    )
    default boolean enableProactivePlayerDetection() { return true; }

    @ConfigItem(
        keyName = "useProjectilePrayerSwitching",
        name = "🎯 Projectile Prayer Switching",
        description = "<html><b>1-tick accurate</b> prayer switching based on incoming projectiles.<br>" +
                      "More accurate than weapon-based (detects actual attacks).<br>" +
                      "Supports ALL wilderness magic, ranged, and melee projectiles.</html>",
        position = 18,
        section = escapeSection
    )
    default boolean useProjectilePrayerSwitching() { return true; }

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
        description = "Enable Player Monitor during banking and emergency escape for enhanced safety against PKers.",
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
