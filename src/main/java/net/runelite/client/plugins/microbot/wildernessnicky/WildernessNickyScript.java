package net.runelite.client.plugins.microbot.wildernessnicky;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import java.util.LinkedHashMap;

import net.runelite.api.coords.WorldPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.TileObject;
import net.runelite.api.World;
import net.runelite.client.plugins.microbot.*;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.*;
import net.runelite.client.plugins.microbot.util.npc.Rs2Npc;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
import net.runelite.client.plugins.microbot.util.tile.Rs2Tile;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.globval.WidgetIndices;
import net.runelite.client.plugins.microbot.globval.enums.InterfaceTab;
import static net.runelite.api.Skill.AGILITY;
import lombok.Getter;

// Advanced Wilderness APIs
import net.runelite.client.plugins.microbot.util.player.Rs2Pvp;
import net.runelite.client.plugins.microbot.util.player.Rs2PlayerModel;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.api.Player;
import net.runelite.api.Actor;
import net.runelite.api.kit.KitType;
import net.runelite.api.GraphicID;
import net.runelite.api.Projectile;
import net.runelite.client.plugins.microbot.wildernessnicky.enums.WildernessProjectileType;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Wilderness Agility Script for RuneLite
 */
public final class WildernessNickyScript extends Script {
    public static final String VERSION = "2.0.0";

    // --- Constants ---
    private static final int ACTION_DELAY = 3000;
    private static final int XP_TIMEOUT = 8000;
    private static final int DISPENSER_ID = 53224;
    private static final int TICKET_ITEM_ID = 29460;
    
    // Regex patterns for dispenser chat messages
    private static final Pattern WILDY_DISPENSER_REGEX = Pattern.compile(
        "You have been awarded <[A-Za-z0-9=\\/]+>([\\d]+) x ([ a-zA-Z(4)]+)<[A-Za-z0-9=\\/]+> and <[A-Za-z0-9=\\/]+>([\\d]+) x ([ a-zA-Z]+)<[A-Za-z0-9=\\/]+> from the Agility dispenser\\."
    );
    
    private static final Pattern WILDY_DISPENSER_EXTRA_REGEX = Pattern.compile(
        "You have been awarded <[A-Za-z0-9=\\/]+>([\\d]+) x ([ a-zA-Z(4)]+)<[A-Za-z0-9=\\/]+> and <[A-Za-z0-9=\\/]+>([\\d]+) x ([ a-zA-Z(4)]+)<[A-Za-z0-9=\\/]+>, and an extra <[A-Za-z0-9=\\/]+>([ a-zA-Z(4)]+)<[A-Za-z0-9=\\/]+> from the Agility dispenser\\."
    );
    private static final int FOOD_PRIMARY = 24592; //anglerfish
    private static final int FOOD_SECONDARY = 24595; //karambwan
    private static final int FOOD_TERTIARY = 24589; //manta ray
    private static final int FOOD_DROP = 24598;  //blighted super restore
    private static final int KNIFE_ID = 946;
    private static final int TELEPORT_ID = 24963;
    private static final int COINS_ID = 995;
    private static final int LOOTING_BAG_CLOSED_ID = 11941;
    private static final int LOOTING_BAG_OPEN_ID = 22586;
    private static final int UNDERGROUND_OBJECT_ID = 53225;
    private static final int FALLING_ANIMATION = 7155; // Animation ID when player falls from obstacle
    private static final WorldPoint START_POINT = new WorldPoint(3004, 3936, 0);
    private static final WorldPoint DISPENSER_POINT = new WorldPoint(3004, 3936, 0);

    // Skeleton escape zone constants
    private static final int LADDER_OBJECT_ID = 17385; // Pit ladder (go underground to break combat)
    private static final WorldPoint LADDER_AREA = new WorldPoint(3004, 3950, 0); // Approx ladder location
    private static final WorldPoint ROCK_EXIT_POINT = new WorldPoint(2995, 3945, 0); // Exit rock area to break aggro

    // ===== WIKI SAFE ZONE ESCAPE ROUTES =====
    // Safe Zone 1: Frozen Waste Plateau (Ice Warriors/Giants) - WEST route
    private static final WorldPoint ICE_WARRIOR_ZONE = new WorldPoint(2978, 3935, 0); // West gate area
    private static final int WEST_GATE_ID = 26760; // West gate from course
    private static final WorldPoint ICE_WARRIOR_SAFE_SPOT = new WorldPoint(2970, 3940, 0); // Deep in ice warrior zone

    // Safe Zone 2: Pirates' Hideout - EAST route (requires lockpick)
    private static final WorldPoint PIRATES_HIDEOUT = new WorldPoint(3038, 3958, 0); // Pirates hideout entrance
    private static final int LOCKPICK_ID = 1523;
    private static final WorldPoint PIRATES_SAFE_SPOT = new WorldPoint(3045, 3960, 0); // Inside hideout

    // Safe Zone 3: Deep Wilderness Dungeon - EAST route (requires diary)
    private static final WorldPoint DEEP_WILDY_DUNGEON = new WorldPoint(3044, 3927, 0); // Dungeon entrance
    private static final WorldPoint DUNGEON_SAFE_SPOT = new WorldPoint(3045, 10325, 0); // Inside dungeon (underground)
    private static final int WILDY_MEDIUM_DIARY_VARBIT = 4500; // Wilderness Medium Diary completion

    // Safe Zone 4: Mage Bank - CURRENT route (requires knife/slash weapon)
    // Already defined in phoenixEscape constants

    // Escape route tracking
    private enum EscapeRoute {
        SKELETON_AGGRO,      // Use skeletons in course as protection
        ICE_WARRIORS,        // West to Frozen Waste Plateau
        PIRATES_HIDEOUT,     // East to Pirates (needs lockpick)
        DEEP_DUNGEON,        // East to dungeon (needs diary)
        MAGE_BANK           // East to Mage Bank (current route)
    }

    private EscapeRoute selectedEscapeRoute = null;
    private boolean hasReachedSafeZone = false;
    private long safeZoneReachedTime = 0;

    // ===== SAFE LOGOUT LOCATIONS (WILDERNESS AGILITY COURSE) =====
    // These are specific safe spots within the course where skeletons won't attack
    // All locations are in Region ID 11837 (Wilderness Agility Course area)
    private static final List<WorldPoint> SAFE_LOGOUT_LOCATIONS = Arrays.asList(
        new WorldPoint(3005, 3933, 0), // Region location (61,29) - Near start
        new WorldPoint(3002, 3940, 0), // Region location (58,36) - Mid course
        new WorldPoint(3006, 3941, 0), // Region location (62,37) - Mid course
        new WorldPoint(3006, 3946, 0), // Region location (62,42) - Upper course
        new WorldPoint(3005, 3963, 0), // Region location (61,59) - Far north
        new WorldPoint(3001, 3944, 0)  // Region location (54,40) - Final safe spot (no skeletons)
    );
    private static final int SAFE_LOGOUT_RADIUS = 2; // Must be within 2 tiles of safe spot

    // --- Config & Plugin ---
    private WildernessNickyConfig config;
    @Inject
    private WildernessNickyPlugin plugin;

    // --- Obstacle Models ---
    private final List<WildernessNickyObstacleModel> obstacles = List.of(
        new WildernessNickyObstacleModel(23137, false),
        new WildernessNickyObstacleModel(23132, true),
        new WildernessNickyObstacleModel(23556, false),
        new WildernessNickyObstacleModel(23542, true),
        new WildernessNickyObstacleModel(23640, false)
    );

    // --- Dispenser Tracking ---
    private int dispenserLoots = 0;
    private boolean waitingForDispenserLoot = false;
    private int dispenserLootAttempts = 0;
    private int dispenserTicketsBefore = 0;
    private int dispenserPreValue = 0;
    private TileObject cachedDispenserObj = null;
    private long lastObjectCheck = 0;

    // Incomplete lap detection
    private boolean incompleteLapDetected = false;
    private int dispenserInteractAttempts = 0;
    private static final int MAX_DISPENSER_INTERACT_ATTEMPTS = 5;

    // Entrance fee tracking
    private boolean entranceFeePaid = false;
    
    // --- Rock Climbing Pose Detection ---
    private boolean waitingForRockClimbCompletion = false;

    // --- Lap & XP Tracking ---
    public int lapCount = 0;
    private int logStartXp = 0;
    private int pipeStartXp = 0;
    private int ropeStartXp = 0;
    private int stonesStartXp = 0;
    private long previousLapTime = 0;
    private long fastestLapTime = Long.MAX_VALUE;
    private long lastLapTimestamp = 0;
    private long startTime = 0;

    // --- State & Progress ---
    private enum ObstacleState {
        INIT,
        STARTUP_INVENTORY_CHECK,
        STARTUP_BANKING,
        START,
        PIPE,
        ROPE,
        STONES,
        LOG,
        ROCKS,
        DISPENSER,
        CONFIG_CHECKS,
        WORLD_HOP_1,
        WORLD_HOP_2,
        WALK_TO_LEVER,
        INTERACT_LEVER,
        BANKING,
        POST_BANK_CONFIG,
        WALK_TO_COURSE,
        SWAP_BACK,
        PIT_RECOVERY,
        EMERGENCY_ESCAPE,
        GE_BUY_LOOTING_BAG,  // Death recovery - buy new looting bag from GE
        GE_BUY_NOTED_LOOTING_BAG,  // Startup - buy noted looting bag from GE
        UNNOTE_LOOTING_BAG  // Startup - unnote looting bag at bank with RoW
    }
    private ObstacleState currentState = ObstacleState.START;
    private ObstacleState pitRecoveryTarget = null;
    private boolean isWaitingForPipe = false;
    private boolean isWaitingForRope = false;
    private boolean isWaitingForStones = false;
    private boolean isWaitingForLog = false;
    private boolean pipeJustCompleted = false;
    private boolean ropeRecoveryWalked = false;
    private boolean forceBankNextLoot = false;
    private boolean forceStartAtCourse = false;
    private boolean needsToBuyLootingBag = false; // Set true after death
    private boolean needsToBuyNotedLootingBag = false; // Set true if no looting bag in bank on startup
    private boolean hasWithdrawnRingOfWealth = false; // Track RoW withdrawal for unnoting
    private int originalWorld = -1;
    private int bankWorld1 = -1;
    private int bankWorld2 = -1;
    private long lastLadderInteractTime = 0;
    private int cachedInventoryValue = 0;
    private long lastObstacleInteractTime = 0;
    private WorldPoint lastObstaclePosition = null;
    @Getter
    private volatile long lastFcJoinMessageTime = 0;
    private long pipeInteractionStartTime = 0;
    
    // World hopping retry tracking
    private int worldHopRetryCount = 0;
    private long worldHopRetryStartTime = 0;
    private static final int MAX_WORLD_HOP_RETRIES = 3;
    private static final long WORLD_HOP_RETRY_TIMEOUT = 30000; // 30 seconds
    
    // Web walking timeout tracking
    private long webWalkStartTime = 0;
    private static final long WEB_WALK_TIMEOUT = 60000; // 60 seconds
    
    // Phoenix Escape tracking
    private boolean phoenixEscapeTriggered = false;
    private long phoenixEscapeStartTime = 0;
    private static final long PHOENIX_ESCAPE_TIMEOUT = 120000; // 2 minutes
    private static final int PHOENIX_NECKLACE_ID = 11090;

    // Emergency Escape tracking
    private boolean emergencyEscapeTriggered = false;
    private long emergencyEscapeStartTime = 0;
    private static final long EMERGENCY_ESCAPE_TIMEOUT = 180000; // 3 minutes
    private boolean hasEquippedPhoenixNecklace = false;
    private boolean hasClimbedRocks = false;
    private boolean hasOpenedGate = false;
    private long escapeStep2StartTime = 0;
    private static final long ESCAPE_STEP_TIMEOUT = 10000; // 10 seconds per step

    // Startup grace period - prevents false escape triggers when config toggles on
    private long scriptStartTime = 0;
    private static final long STARTUP_GRACE_PERIOD = 10000; // 10 seconds grace period
    
    // Escape route constants (from Netoxic's script)
    private static final WorldPoint GATE_AREA = new WorldPoint(2998, 3931, 0);
    private static final int GATE_OBJECT_ID = 23552;
    private static final int ROCKS_OBJECT_ID = 23640;
    private static final WorldPoint SOUTH_WEST_CORNER = new WorldPoint(2991, 3936, 0);
    private static final WorldPoint NORTH_EAST_CORNER = new WorldPoint(3001, 3945, 0);
    private static final int ROCK_AREA_WIDTH = (NORTH_EAST_CORNER.getX() - SOUTH_WEST_CORNER.getX()) + 1;
    private static final int ROCK_AREA_HEIGHT = (NORTH_EAST_CORNER.getY() - SOUTH_WEST_CORNER.getY()) + 1;
    
    // Location tracking for stuck detection
    private WorldPoint lastPlayerLocation = null;
    private long lastLocationChangeTime = 0;
    private static final long LOCATION_STUCK_TIMEOUT = 8000; // 8 seconds
    private ObstacleState lastStateBeforeStuck = null;
    
    // Looting bag tracking
    private boolean needsLootingBagActivation = false;
    
    // Drop location tracking for random mode
    private boolean shouldDropAfterDispenser = false;
    
    // Death tracking
    private boolean deathDetected = false;

    // Proactive Player Detection
    private long lastPlayerScanTime = 0;
    private static final long PLAYER_SCAN_INTERVAL = 5000; // Scan every 5 seconds
    private static final int THREAT_SCAN_RADIUS = 15; // Tiles to scan around player

    // ===== ADVANCED WILDERNESS COMBAT SYSTEM =====
    // Prayer switching system (weapon-based)
    private Rs2PlayerModel currentAttacker = null;
    private long lastPrayerSwitchTime = 0;
    private static final long PRAYER_SWITCH_COOLDOWN = 600; // 1 game tick
    private Rs2PrayerEnum activeCombatPrayer = null;

    // Projectile-based prayer switching system (1-tick accurate)
    public final Map<Integer, Projectile> incomingProjectiles = new ConcurrentHashMap<>();
    private long lastProjectileCheckTime = 0;
    private static final long PROJECTILE_CHECK_INTERVAL = 50; // Check every 50ms for maximum accuracy
    public int lastProjectileId = -1;
    public long lastProjectileDetectionTime = 0;
    public int projectilesDetectedCount = 0;

    // Logout reason tracking
    private String lastLogoutReason = "";
    private long lastLogoutTime = 0;
    private boolean shouldShowLogoutPopup = true;

    // Teleblock tracking
    private boolean isTeleBlocked = false;
    private long teleBlockDetectedTime = 0;

    // ===== ENHANCED ANTI-PK SYSTEM =====
    // PKer equipment tracking for smarter prayer prediction
    private Map<String, Integer> pkerWeaponCache = new ConcurrentHashMap<>();
    private Map<String, Integer> pkerCapeCache = new ConcurrentHashMap<>();
    private long lastEquipmentScanTime = 0;
    private static final long EQUIPMENT_SCAN_INTERVAL = 600; // Scan every tick

    // Freeze detection (entangle, ice barrage, etc.)
    private boolean isFrozen = false;
    private long freezeStartTime = 0;
    private static final long FREEZE_MAX_DURATION = 20000; // 20 seconds max freeze

    // Gap closing detection (PKer rushing)
    private Map<String, Integer> pkerLastDistance = new ConcurrentHashMap<>();
    private Map<String, Long> pkerRushDetectionTime = new ConcurrentHashMap<>();
    private static final int RUSH_DISTANCE_THRESHOLD = 5; // If PKer closes 5+ tiles in 2 seconds
    private static final long RUSH_DETECTION_WINDOW = 2000; // 2 second window

    // Multi-PKer panic mode
    private Set<String> nearbyPkers = new HashSet<>();
    private long lastPanicModeCheck = 0;
    private static final long PANIC_MODE_CHECK_INTERVAL = 1000; // Check every second
    private static final int PANIC_MODE_PKER_THRESHOLD = 2; // 2+ PKers = panic

    // Combat state tracking (using Rs2Combat API)
    private boolean wasInCombatLastTick = false;
    private long lastCombatStateChangeTime = 0;

    // Attack range calculation constants
    private static final int BARRAGE_RANGE = 10;
    private static final int MSB_RANGE = 10;
    private static final int MELEE_RANGE = 1;

    // Combat tracking
    private long lastCombatActionTime = 0;
    private static final long COMBAT_TIMEOUT = 10000; // 10 seconds

    // Wilderness level tracking
    private int currentWildernessLevel = 0;
    private long lastWildernessLevelCheck = 0;
    private static final long WILDERNESS_LEVEL_CHECK_INTERVAL = 1000; // Check every second

    // Equipment-based threat assessment
    private boolean lastThreatHadHighTierGear = false;

    // Looting bag value tracking
    private int lootingBagValue = 0;
    private WildernessNickyItems wildyItems;
    private boolean waitingForLootingBagSync = false;
    private boolean hasCheckedLootingBagOnStartup = false; // Only check once per script run
    private static final int LOOTING_BAG_CONTAINER_ID = 516; // Container ID for looting bag interface
    @Getter
    private java.util.Map<String, Integer> lootingBagContents = new java.util.HashMap<>(); // Item name -> quantity

    // --- Position-Based Timeout & Retry System ---
    private WorldPoint lastTrackedPosition = null;
    private long positionLastChangedTime = 0;
    private long lastPositionCheckTime = 0;
    private int currentStateRetryAttempts = 0;
    private boolean isInRetryMode = false;
    private static final long POSITION_CHECK_INTERVAL = 1000; // Check position every 1 second
    private static final int MAX_RETRY_ATTEMPTS = 1; // Only retry once before moving to next state

    // ===== BANKED LOOT TRACKING SYSTEM =====
    @Getter
    private HashMap<String, Integer> bankedLoot = new HashMap<>();
    private int totalBankedValue = 0;
    private int totalBankingTrips = 0;

    // ===== ENHANCED PKER DETECTION (WAIT FOR HIT) =====
    private int previousHealth = 100;
    private long lastHealthCheckTime = 0;
    private static final long HEALTH_CHECK_INTERVAL = 100; // Check health every tick
    private boolean recentlyTookPvPDamage = false;
    private long lastPvPDamageTime = 0;
    private static final long PVP_DAMAGE_TIMEOUT = 30000; // 30 seconds
    private int pvpHitCount = 0; // Track number of PvP hits (total)
    private int clanMemberHitCount = 0; // Track hits from clan/FC members
    private int nonClanHitCount = 0; // Track hits from non-clan players
    private String lastAttackerName = ""; // Track who hit us last

    // ===== SOLO MODE / LOGOUT PRIORITY =====
    private boolean attemptingLogout = false;
    private long logoutAttemptStartTime = 0;
    private static final long MAX_LOGOUT_ATTEMPT_TIME = 3000; // Try to logout for 3 seconds (5 game ticks) before running

    // ===== SOLO MODE WORLD HOP LOGIC =====
    private int soloModeWorldHopAttempts = 0;
    private static final int MAX_SOLO_WORLD_HOP_ATTEMPTS = 3;

    // ===== MASS MODE RELOGIN LOGIC =====
    private boolean massLoggedOutDueToAttack = false;
    private long massLogoutTime = 0;
    private int massWorldToReturn = -1;
    private WorldPoint massLogoutLocation = null;

    // ===== ESCAPE STEP RETRY COUNTERS (FAILSAFE) =====
    private int escapeEquipNecklaceAttempts = 0;
    private int escapeClimbRocksAttempts = 0;
    private int escapeOpenGateAttempts = 0;
    private int escapeWalkToMageBankAttempts = 0;
    private static final int MAX_ESCAPE_STEP_ATTEMPTS = 5; // Max 5 attempts per step before skipping

    // ===== ESCAPE/LOGOUT REASON TRACKING =====
    @Getter
    private String lastEscapeReason = "Not escaped yet";
    @Getter
    private long lastEscapeTime = 0;

    // ===== CUSTOM WORLD LIST TRACKING =====
    private List<Integer> parsedSoloWorlds = new ArrayList<>();
    private List<Integer> parsedMassWorlds = new ArrayList<>();
    private int currentSoloWorldIndex = 0;
    private int currentMassWorldIndex = 0;

    // ===== FC JOIN COOLDOWN TRACKING =====
    private long lastFcJoinAttemptTime = 0;
    private static final long FC_JOIN_COOLDOWN = 60000; // 60 seconds cooldown between join attempts

    // =============================================================================
    // WORLD PARSING AND VALIDATION HELPERS
    // =============================================================================

    /**
     * Parses a comma-separated string of world numbers into a validated list.
     * Filters out invalid worlds (Deadman, Tournament, skill total, etc.)
     *
     * @param worldString Comma-separated world numbers (e.g., "301,302,303")
     * @return List of valid world numbers
     */
    private List<Integer> parseAndValidateWorlds(String worldString) {
        List<Integer> validWorlds = new ArrayList<>();

        if (worldString == null || worldString.trim().isEmpty()) {
            return validWorlds;
        }

        // Split by comma and parse
        String[] worldTokens = worldString.split(",");
        for (String token : worldTokens) {
            try {
                int worldNum = Integer.parseInt(token.trim());

                // Validate world number range (300-600)
                if (worldNum < 300 || worldNum > 600) {
                    Microbot.log("[WildernessNicky] ⚠️ Invalid world number: " + worldNum + " (must be 300-600)");
                    continue;
                }

                // Check if world is valid (not Deadman, Tournament, etc.)
                if (isValidWorld(worldNum)) {
                    validWorlds.add(worldNum);
                } else {
                    Microbot.log("[WildernessNicky] ⚠️ Skipping invalid/restricted world: " + worldNum);
                }
            } catch (NumberFormatException e) {
                Microbot.log("[WildernessNicky] ⚠️ Invalid world format: " + token);
            }
        }

        return validWorlds;
    }

    /**
     * Validates if a world number is a valid OSRS world
     * Simple validation - checks if world is in valid range
     *
     * @param worldNum World number to validate
     * @return true if world is valid for regular play
     */
    private boolean isValidWorld(int worldNum) {
        // Basic validation: world number must be between 300-600
        // This covers all regular OSRS worlds
        // Excludes special worlds (Deadman, Tournament, etc.) which are typically outside this range
        return worldNum >= 300 && worldNum <= 600;
    }

    /**
     * Gets the next world from the solo hop world list (cycling through)
     *
     * @return Next valid world number, or -1 if no worlds configured
     */
    private int getNextSoloWorld() {
        if (parsedSoloWorlds.isEmpty()) {
            return -1;
        }

        int world = parsedSoloWorlds.get(currentSoloWorldIndex);
        currentSoloWorldIndex = (currentSoloWorldIndex + 1) % parsedSoloWorlds.size();

        return world;
    }

    /**
     * Gets the next world from the mass hop world list (cycling through)
     *
     * @return Next valid world number, or -1 if no worlds configured
     */
    private int getNextMassWorld() {
        if (parsedMassWorlds.isEmpty()) {
            return -1;
        }

        int world = parsedMassWorlds.get(currentMassWorldIndex);
        currentMassWorldIndex = (currentMassWorldIndex + 1) % parsedMassWorlds.size();

        return world;
    }

    /**
     * Parses WorldPoint coordinates from config string (format: "X,Y,Z")
     *
     * @param coordString Coordinate string from config (e.g., "2970,3940,0")
     * @param defaultPoint Fallback WorldPoint if parsing fails
     * @return Parsed WorldPoint or default if invalid
     */
    private WorldPoint parseWorldPoint(String coordString, WorldPoint defaultPoint) {
        if (coordString == null || coordString.trim().isEmpty()) {
            return defaultPoint;
        }

        try {
            String[] parts = coordString.trim().split(",");
            if (parts.length != 3) {
                Microbot.log("[WildernessNicky] ⚠️ Invalid coordinate format (expected X,Y,Z): " + coordString);
                return defaultPoint;
            }

            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            int z = Integer.parseInt(parts[2].trim());

            // Basic validation
            if (x < 0 || y < 0 || z < 0 || z > 3) {
                Microbot.log("[WildernessNicky] ⚠️ Invalid coordinate values: " + coordString);
                return defaultPoint;
            }

            return new WorldPoint(x, y, z);
        } catch (NumberFormatException e) {
            Microbot.log("[WildernessNicky] ⚠️ Failed to parse coordinates: " + coordString);
            return defaultPoint;
        }
    }

    // =============================================================================
    // ENHANCED ANTI-PK DETECTION SYSTEM
    // =============================================================================

    /**
     * Detects PKer's primary attack style based on equipped weapon and gear.
     * Used for predictive prayer switching before projectiles are even fired.
     *
     * @param pker The player to analyze
     * @return Recommended protection prayer, or null if unknown
     */
    private Rs2PrayerEnum detectPkerThreatType(Player pker) {
        if (pker == null || pker.getPlayerComposition() == null) {
            return null;
        }

        String pkerName = pker.getName();
        int weapon = pker.getPlayerComposition().getEquipmentId(KitType.WEAPON);
        int cape = pker.getPlayerComposition().getEquipmentId(KitType.CAPE);

        // Cache equipment for tracking changes
        pkerWeaponCache.put(pkerName, weapon);
        pkerCapeCache.put(pkerName, cape);

        // MAGIC DETECTION (highest priority - most dangerous in wildy)
        // Toxic staff of the dead / Staff of the dead / Kodai wand
        if (weapon == 12904 || weapon == 11791 || weapon == 21006) {
            return Rs2PrayerEnum.PROTECT_MAGIC;
        }

        // RANGED DETECTION
        // Webweaver bow / Zaryte crossbow / Toxic blowpipe / Magic shortbow / Dark bow
        if (weapon == 27219 || weapon == 26374 || weapon == 12926 || weapon == 861 || weapon == 11235) {
            return Rs2PrayerEnum.PROTECT_RANGE;
        }

        // MELEE DETECTION (look for melee-specific indicators)
        // Infernal cape (99% melee) or Fire cape + melee weapon
        if (cape == 21282 || cape == 13342) {
            // AGS / Voidwaker / Dragon claws / Dragon dagger / Saradomin sword
            int[] meleeWeapons = {11802, 27690, 13652, 1215, 11730};
            for (int meleeWeap : meleeWeapons) {
                if (weapon == meleeWeap) {
                    return Rs2PrayerEnum.PROTECT_MELEE;
                }
            }
        }

        return null; // Unknown - rely on projectile detection
    }

    /**
     * Checks if player is currently frozen (entangle, ice barrage, etc.)
     * Detects freeze by checking for movement restriction graphics
     *
     * @return true if frozen
     */
    private boolean checkIfFrozen() {
        Player localPlayer = Microbot.getClient().getLocalPlayer();
        if (localPlayer == null) {
            return false;
        }

        // Check for freeze graphics (ice barrage = 369, entangle = 179, snare = 178, bind = 181)
        int[] freezeGraphics = {369, 179, 178, 181};
        int currentGraphic = localPlayer.getGraphic();

        for (int freezeGraphic : freezeGraphics) {
            if (currentGraphic == freezeGraphic) {
                if (!isFrozen) {
                    isFrozen = true;
                    freezeStartTime = System.currentTimeMillis();
                    Microbot.log("[WildernessNicky] ❄️ FROZEN DETECTED! Graphic: " + currentGraphic);
                }
                return true;
            }
        }

        // Auto-expire freeze after max duration
        if (isFrozen && (System.currentTimeMillis() - freezeStartTime) > FREEZE_MAX_DURATION) {
            isFrozen = false;
            Microbot.log("[WildernessNicky] ❄️ Freeze expired");
        }

        return isFrozen;
    }

    /**
     * Detects if a PKer is gap-closing (rushing toward player)
     * Tracks distance changes over time to detect aggressive movement
     *
     * @param pker The player to check
     * @return true if PKer is rushing (closing 5+ tiles in 2 seconds)
     */
    private boolean isPkerRushing(Player pker) {
        if (pker == null) {
            return false;
        }

        String pkerName = pker.getName();
        WorldPoint pkerLoc = pker.getWorldLocation();
        WorldPoint myLoc = Rs2Player.getWorldLocation();

        if (pkerLoc == null || myLoc == null) {
            return false;
        }

        int currentDistance = myLoc.distanceTo(pkerLoc);

        // Check if we have previous distance data
        if (pkerLastDistance.containsKey(pkerName)) {
            int lastDistance = pkerLastDistance.get(pkerName);
            long lastCheckTime = pkerRushDetectionTime.getOrDefault(pkerName, 0L);
            long timeSinceLastCheck = System.currentTimeMillis() - lastCheckTime;

            // If distance decreased by 5+ tiles within 2 seconds = RUSH
            if (timeSinceLastCheck <= RUSH_DETECTION_WINDOW) {
                int distanceClosed = lastDistance - currentDistance;
                if (distanceClosed >= RUSH_DISTANCE_THRESHOLD) {
                    Microbot.log("[WildernessNicky] 🏃 RUSH DETECTED: " + pkerName + " closed " + distanceClosed + " tiles!");
                    return true;
                }
            }
        }

        // Update tracking
        pkerLastDistance.put(pkerName, currentDistance);
        pkerRushDetectionTime.put(pkerName, System.currentTimeMillis());

        return false;
    }

    /**
     * Checks if we should activate PANIC MODE (multiple PKers detected)
     * Panic mode = instant logout, no hesitation
     *
     * @return true if panic mode should activate
     */
    private boolean shouldActivatePanicMode() {
        long currentTime = System.currentTimeMillis();

        // Only check every second to reduce overhead
        if (currentTime - lastPanicModeCheck < PANIC_MODE_CHECK_INTERVAL) {
            return nearbyPkers.size() >= PANIC_MODE_PKER_THRESHOLD;
        }

        lastPanicModeCheck = currentTime;
        nearbyPkers.clear();

        // Scan for attackable players within threat radius
        WorldPoint myLoc = Rs2Player.getWorldLocation();
        if (myLoc == null) {
            return false;
        }

        List<Player> players = Microbot.getClient().getPlayers();
        if (players == null) {
            return false;
        }

        for (Player player : players) {
            if (player == null || player == Microbot.getClient().getLocalPlayer()) {
                continue;
            }

            WorldPoint pLoc = player.getWorldLocation();
            if (pLoc == null) {
                continue;
            }

            // Check if within threat radius and attackable
            if (pLoc.distanceTo(myLoc) <= THREAT_SCAN_RADIUS) {
                if (Rs2Pvp.isAttackable(player)) {
                    nearbyPkers.add(player.getName());
                }
            }
        }

        boolean panicMode = nearbyPkers.size() >= PANIC_MODE_PKER_THRESHOLD;
        if (panicMode) {
            Microbot.log("[WildernessNicky] 🚨 PANIC MODE: " + nearbyPkers.size() + " PKers detected!");
        }

        return panicMode;
    }

    /**
     * Calculates if a PKer is within attack range based on their weapon type
     *
     * @param pker The player to check
     * @return true if PKer can attack from current distance
     */
    private boolean isPkerInAttackRange(Player pker) {
        if (pker == null) {
            return false;
        }

        WorldPoint pkerLoc = pker.getWorldLocation();
        WorldPoint myLoc = Rs2Player.getWorldLocation();

        if (pkerLoc == null || myLoc == null) {
            return false;
        }

        int distance = myLoc.distanceTo(pkerLoc);

        // Get cached weapon to determine range
        String pkerName = pker.getName();
        Integer weapon = pkerWeaponCache.get(pkerName);

        // If we don't know weapon, assume max range (barrage/range)
        if (weapon == null) {
            return distance <= BARRAGE_RANGE;
        }

        // Magic weapons (barrage range = 10)
        if (weapon == 12904 || weapon == 11791 || weapon == 21006) {
            return distance <= BARRAGE_RANGE;
        }

        // Ranged weapons (MSB/crossbow range = 10)
        if (weapon == 27219 || weapon == 26374 || weapon == 12926 || weapon == 861) {
            return distance <= MSB_RANGE;
        }

        // Melee weapons (range = 1, or 2 with halberd)
        return distance <= MELEE_RANGE;
    }

    /**
     * Enhanced combat state detection using Rs2Combat API
     * More reliable than manual checks
     *
     * @return true if in combat
     */
    private boolean isInCombatEnhanced() {
        boolean inCombat = Rs2Combat.inCombat();

        // Track combat state changes
        if (inCombat != wasInCombatLastTick) {
            wasInCombatLastTick = inCombat;
            lastCombatStateChangeTime = System.currentTimeMillis();

            if (inCombat) {
                Microbot.log("[WildernessNicky] ⚔️ Entered combat!");
            } else {
                Microbot.log("[WildernessNicky] ✅ Exited combat");
            }
        }

        return inCombat;
    }

    // =============================================================================
    // WIKI SAFE ZONE ESCAPE SYSTEM
    // =============================================================================

    /**
     * Selects the best escape route based on:
     * 1. Player position (closest route)
     * 2. Inventory requirements (lockpick, knife)
     * 3. Diary completion (for dungeon shortcut)
     *
     * @return Best escape route for current situation
     */
    private EscapeRoute selectBestEscapeRoute() {
        WorldPoint myLoc = Rs2Player.getWorldLocation();
        if (myLoc == null) {
            return EscapeRoute.MAGE_BANK; // Default fallback
        }

        // Check inventory requirements
        boolean hasLockpick = Rs2Inventory.hasItem(LOCKPICK_ID);
        boolean hasKnife = Rs2Inventory.hasItem(KNIFE_ID) || Rs2Equipment.isWearing("Knife");
        boolean hasSlashWeapon = hasKnife; // Simplified - knife is most common
        boolean hasDiary = Microbot.getVarbitValue(WILDY_MEDIUM_DIARY_VARBIT) == 1;

        // Calculate distances to each safe zone
        int distToIceWarriors = myLoc.distanceTo(ICE_WARRIOR_ZONE);
        int distToPirates = myLoc.distanceTo(PIRATES_HIDEOUT);
        int distToDungeon = myLoc.distanceTo(DEEP_WILDY_DUNGEON);
        int distToMageBank = myLoc.distanceTo(new WorldPoint(3090, 3960, 0)); // Mage bank approx

        Microbot.log("[WildernessNicky] 🗺️ Escape Route Distances:");
        Microbot.log("  Ice Warriors (West): " + distToIceWarriors + " tiles");
        Microbot.log("  Pirates (East, lockpick=" + hasLockpick + "): " + distToPirates + " tiles");
        Microbot.log("  Dungeon (East, diary=" + hasDiary + "): " + distToDungeon + " tiles");
        Microbot.log("  Mage Bank (East, slash=" + hasSlashWeapon + "): " + distToMageBank + " tiles");

        // Priority 1: SKELETON_AGGRO if already near skeletons in course
        WorldArea courseArea = new WorldArea(SOUTH_WEST_CORNER, ROCK_AREA_WIDTH, ROCK_AREA_HEIGHT);
        if (courseArea.contains(myLoc)) {
            Microbot.log("[WildernessNicky] 💀 Using SKELETON AGGRO strategy (inside course)");
            return EscapeRoute.SKELETON_AGGRO;
        }

        // Priority 2: ICE_WARRIORS (closest and no requirements)
        if (distToIceWarriors <= 25) {
            Microbot.log("[WildernessNicky] ❄️ Using ICE WARRIORS route (closest, no requirements)");
            return EscapeRoute.ICE_WARRIORS;
        }

        // Priority 3: PIRATES (if has lockpick and closer than others)
        if (hasLockpick && distToPirates < distToMageBank && distToPirates < distToDungeon) {
            Microbot.log("[WildernessNicky] 🏴‍☠️ Using PIRATES HIDEOUT route (has lockpick)");
            return EscapeRoute.PIRATES_HIDEOUT;
        }

        // Priority 4: DUNGEON (if has diary and close)
        if (hasDiary && distToDungeon < distToMageBank) {
            Microbot.log("[WildernessNicky] 🕳️ Using DEEP DUNGEON route (has diary)");
            return EscapeRoute.DEEP_DUNGEON;
        }

        // Priority 5: MAGE_BANK (default, works for everyone)
        Microbot.log("[WildernessNicky] 🏦 Using MAGE BANK route (default)");
        return EscapeRoute.MAGE_BANK;
    }

    // NOTE: isInSafeZone() method defined later in file (line ~3265)
    // Removed duplicate definition here to fix compilation error

    /**
     * Executes the selected escape route
     * Each route navigates to NPC-dense areas for protection
     */
    private void executeEscapeRoute() {
        if (selectedEscapeRoute == null) {
            selectedEscapeRoute = selectBestEscapeRoute();
        }

        WorldPoint myLoc = Rs2Player.getWorldLocation();
        if (myLoc == null) {
            return;
        }

        switch (selectedEscapeRoute) {
            case SKELETON_AGGRO:
                executeSkeletonAggroRoute();
                break;

            case ICE_WARRIORS:
                executeIceWarriorsRoute();
                break;

            case PIRATES_HIDEOUT:
                executePiratesRoute();
                break;

            case DEEP_DUNGEON:
                executeDungeonRoute();
                break;

            case MAGE_BANK:
                // Already handled by phoenixEscape logic
                // This fallback is if other routes fail
                break;
        }
    }

    /**
     * SKELETON AGGRO Route: Stay in course, deliberately get skeleton aggro
     * Wiki: "Try to get NPCs to attack you in single-combat area"
     */
    private void executeSkeletonAggroRoute() {
        WorldPoint myLoc = Rs2Player.getWorldLocation();
        if (myLoc == null) {
            return;
        }

        // If already have skeleton aggro, we're safe!
        if (isInSkeletonCombat()) {
            Microbot.log("[WildernessNicky] 💀 Skeleton is protecting us - attempting logout");
            Rs2Player.logout();
            sleep(100);
            return;
        }

        // Move to skeleton-dense area (near ladder/rocks)
        if (myLoc.distanceTo(LADDER_AREA) > 3) {
            Microbot.log("[WildernessNicky] 💀 Moving to skeleton zone for protection");
            Rs2Walker.walkTo(LADDER_AREA, 2);
            sleep(600);
        }

        // Wait for skeleton to attack (don't break combat this time!)
        Microbot.log("[WildernessNicky] 💀 Waiting for skeleton aggro...");
        sleep(1200);
    }

    /**
     * ICE WARRIORS Route: West through gate to Frozen Waste Plateau
     * Wiki: "run west through Frozen Waste Plateau and south through gate"
     */
    private void executeIceWarriorsRoute() {
        WorldPoint myLoc = Rs2Player.getWorldLocation();
        if (myLoc == null) {
            return;
        }

        // Parse custom safe spot from config (or use default)
        WorldPoint targetSafeSpot = parseWorldPoint(config.iceWarriorsSafeSpot(), ICE_WARRIOR_SAFE_SPOT);

        // Step 1: Head west toward gate
        if (myLoc.distanceTo(ICE_WARRIOR_ZONE) > 5) {
            Microbot.log("[WildernessNicky] ❄️ Running west to Ice Warrior zone");
            Rs2Walker.walkTo(ICE_WARRIOR_ZONE, 2);
            sleep(600);
            return;
        }

        // Step 2: Open west gate if needed
        TileObject westGate = Rs2GameObject.findObjectById(WEST_GATE_ID);
        if (westGate != null && myLoc.distanceTo(westGate.getWorldLocation()) < 5) {
            Microbot.log("[WildernessNicky] 🚪 Opening west gate");
            Rs2GameObject.interact(westGate, "Open");
            sleep(1200);
            return;
        }

        // Step 3: Move to custom ice warrior safe spot
        if (myLoc.distanceTo(targetSafeSpot) > 3) {
            Microbot.log("[WildernessNicky] ❄️ Moving to safe spot: " + targetSafeSpot);
            Rs2Walker.walkTo(targetSafeSpot, 2);
            sleep(600);
            return;
        }

        // Step 4: Wait for ice warrior aggro, then logout
        if (isInSafeZone()) {
            Microbot.log("[WildernessNicky] ❄️ Ice Warriors protecting - attempting logout");
            Rs2Player.logout();
            sleep(100);
        }
    }

    /**
     * PIRATES Route: East to Pirates' Hideout (requires lockpick)
     * Wiki: "run east to Pirates' Hideout. This requires a lockpick"
     */
    private void executePiratesRoute() {
        WorldPoint myLoc = Rs2Player.getWorldLocation();
        if (myLoc == null) {
            return;
        }

        // Verify we still have lockpick
        if (!Rs2Inventory.hasItem(LOCKPICK_ID)) {
            Microbot.log("[WildernessNicky] ⚠️ Lost lockpick - switching to Mage Bank route");
            selectedEscapeRoute = EscapeRoute.MAGE_BANK;
            return;
        }

        // Parse custom safe spot from config (or use default)
        WorldPoint targetSafeSpot = parseWorldPoint(config.piratesSafeSpot(), PIRATES_SAFE_SPOT);

        // Navigate to custom pirates safe spot
        if (myLoc.distanceTo(targetSafeSpot) > 3) {
            Microbot.log("[WildernessNicky] 🏴‍☠️ Running to safe spot: " + targetSafeSpot);
            Rs2Walker.walkTo(targetSafeSpot, 2);
            sleep(600);
            return;
        }

        // Wait for pirate aggro, then logout
        if (isInSafeZone()) {
            Microbot.log("[WildernessNicky] 🏴‍☠️ Pirates protecting - attempting logout");
            Rs2Player.logout();
            sleep(100);
        }
    }

    /**
     * DEEP DUNGEON Route: East to Deep Wilderness Dungeon (requires diary)
     * Wiki: "run east to Deep Wilderness Dungeon. Complete Wilderness Medium Diary"
     */
    private void executeDungeonRoute() {
        WorldPoint myLoc = Rs2Player.getWorldLocation();
        if (myLoc == null) {
            return;
        }

        // Verify we have diary
        boolean hasDiary = Microbot.getVarbitValue(WILDY_MEDIUM_DIARY_VARBIT) == 1;
        if (!hasDiary) {
            Microbot.log("[WildernessNicky] ⚠️ No Wilderness Medium Diary - switching to Mage Bank route");
            selectedEscapeRoute = EscapeRoute.MAGE_BANK;
            return;
        }

        // Parse custom safe spot from config (or use default)
        WorldPoint targetSafeSpot = parseWorldPoint(config.dungeonSafeSpot(), DUNGEON_SAFE_SPOT);

        // Navigate to custom dungeon safe spot
        if (myLoc.distanceTo(targetSafeSpot) > 3) {
            Microbot.log("[WildernessNicky] 🕳️ Running to safe spot: " + targetSafeSpot);
            Rs2Walker.walkTo(targetSafeSpot, 2);
            sleep(600);
            return;
        }

        // Wait for monster aggro, then logout
        if (isInSafeZone()) {
            Microbot.log("[WildernessNicky] 🕳️ Dungeon monsters protecting - attempting logout");
            Rs2Player.logout();
            sleep(100);
        }
    }

    /**
     * Starts the Wilderness Agility script.
     * @param config The script configuration
     * @return true if started successfully
     */
    public boolean run(WildernessNickyConfig config) {
        this.config = config;
        forceStartAtCourse = false; // Always reset on run

        // Initialize wilderness agility items for looting bag value tracking
        wildyItems = new WildernessNickyItems(Microbot.getItemManager());

        // Initialize startup grace period timer
        scriptStartTime = System.currentTimeMillis();

        // Parse custom world lists
        parsedSoloWorlds = parseAndValidateWorlds(config.soloHopWorlds());
        parsedMassWorlds = parseAndValidateWorlds(config.massHopWorlds());

        if (!parsedSoloWorlds.isEmpty()) {
            Microbot.log("[WildernessNicky] 🌍 Solo Hop Worlds: " + parsedSoloWorlds);
        }
        if (!parsedMassWorlds.isEmpty()) {
            Microbot.log("[WildernessNicky] 🌍 Mass Hop Worlds: " + parsedMassWorlds);
        }

        // Log selected play mode
        Microbot.log("[WildernessNicky] 🎮 Play Mode: " + config.playMode().toString());

        // ===== FC STARTUP CHECK =====
        // If FC is enabled but not in chat, auto-join
        if (config.joinFc() && !isInFriendChat()) {
            Microbot.log("[WildernessNicky] 🔗 FC enabled but not in chat - auto-joining");
            sleep(1000); // Wait for client to fully initialize
            joinFriendChat();
        }
        // ===================================

        if (config.debugMode()) {
            currentState = ObstacleState.valueOf(config.debugStartState().name());
            Microbot.log("[DEBUG MODE] Starting in state: " + currentState);

            // Initialize emergency escape variables if starting in EMERGENCY_ESCAPE state
            if (currentState == ObstacleState.EMERGENCY_ESCAPE) {
                emergencyEscapeTriggered = true;
                emergencyEscapeStartTime = System.currentTimeMillis();
                hasEquippedPhoenixNecklace = false;
                hasClimbedRocks = false;
                hasOpenedGate = false;
                escapeStep2StartTime = 0;
                Microbot.log("[DEBUG MODE] Emergency escape variables initialized");
            }
        } else {
            // If NOT starting at course, do inventory check first
            if (!config.startAtCourse()) {
                currentState = ObstacleState.STARTUP_INVENTORY_CHECK;
                Microbot.log("[WildernessNicky] Starting with inventory check...");
            } else {
                // Starting at course - assume entrance fee already paid
                currentState = ObstacleState.START;
                entranceFeePaid = true; // Assume paid when starting at course
                Microbot.log("[WildernessNicky] Starting at course (skipping inventory check)");
                Microbot.log("[WildernessNicky] ⚠️ ASSUMING ENTRANCE FEE ALREADY PAID!");
                Microbot.log("[WildernessNicky] ⚠️ If you get 0 value loot, fee wasn't paid and bot will auto-bank");
            }
        }
        startTime = System.currentTimeMillis();
        Microbot.log("[WildernessNickyScript] startup called - grace period active for " + (STARTUP_GRACE_PERIOD/1000) + " seconds");
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                // ===== MASS MODE RELOGIN HANDLER =====
                if (!Microbot.isLoggedIn()) {
                    // Check if we should try to re-login (mass mode only)
                    if (config.playMode() == WildernessNickyConfig.PlayMode.MASS && massLoggedOutDueToAttack) {
                        long timeSinceLogout = System.currentTimeMillis() - massLogoutTime;
                        int minDelay = config.massReloginDelayMin() * 1000;
                        int maxDelay = config.massReloginDelayMax() * 1000;

                        // Random delay between min and max
                        int randomDelay = minDelay + (new Random().nextInt(maxDelay - minDelay + 1));

                        if (timeSinceLogout >= randomDelay) {
                            Microbot.log("[WildernessNicky] 🔄 Mass Mode: Re-logging after " + (timeSinceLogout/1000) + " seconds...");

                            // The login happens automatically by the client when not logged in
                            // We just need to wait and check if we're logged in
                            sleep(2000); // Wait a bit for client to attempt login

                            // Check if we're logged in now
                            if (Microbot.isLoggedIn()) {
                                Microbot.log("[WildernessNicky] ✅ Successfully re-logged in!");

                                // Return to mass world if configured (custom worlds or text box)
                                if ((!parsedMassWorlds.isEmpty() || getMassWorldFromConfig() > 0) && massWorldToReturn > 0) {
                                    int targetWorld = massWorldToReturn;
                                    int currentWorld = Rs2Player.getWorld();

                                    if (currentWorld != targetWorld) {
                                        Microbot.log("[WildernessNicky] 🌍 Hopping back to mass world " + targetWorld + "...");
                                        boolean hopSuccess = Microbot.hopToWorld(targetWorld);
                                        if (hopSuccess) {
                                            sleepUntil(() -> Rs2Player.getWorld() == targetWorld, 15000);
                                            sleep(2000, 3000);
                                            Microbot.log("[WildernessNicky] ✅ Returned to mass world " + targetWorld);
                                        }
                                    }
                                }

                                // Reset escape state and return to course
                                massLoggedOutDueToAttack = false;
                                emergencyEscapeTriggered = false;
                                scriptStartTime = System.currentTimeMillis(); // Reset grace period

                                // Reset hit counters
                                pvpHitCount = 0;
                                clanMemberHitCount = 0;
                                nonClanHitCount = 0;
                                lastPvPDamageTime = 0;

                                // Walk back to course if we have a saved location
                                if (massLogoutLocation != null) {
                                    Microbot.log("[WildernessNicky] 🚶 Walking back to course...");
                                    Rs2Walker.walkTo(massLogoutLocation, 0);
                                    sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(massLogoutLocation) <= 5, 10000);
                                }

                                // Resume course - figure out which obstacle is next
                                currentState = ObstacleState.START;
                                Microbot.log("[WildernessNicky] 🏃 Resuming wilderness agility course!");
                            } else {
                                Microbot.log("[WildernessNicky] ⚠️ Not logged in yet - will check again next cycle");
                            }
                        }
                    }
                    return;
                }

                // Check for death via health percentage or chat message detection
                if (Rs2Player.getHealthPercentage() <= 0 || deathDetected) {
                    if (deathDetected) {
                        Microbot.log("[WildernessNicky] Death detected - triggering death handler");
                    }
                    handlePlayerDeath();
                    deathDetected = false; // Reset flag
                    return;
                }
                
        // Rock climbing pose detection - wait for pose animation 737 to finish
        if (waitingForRockClimbCompletion) {
            int currentPoseAnimation = Rs2Player.getPoseAnimation();
            if (currentPoseAnimation != 737) {
                waitingForRockClimbCompletion = false;

                // Now interact with dispenser
                TileObject dispenser = cachedDispenserObj;
                if (dispenser != null) {
                    dispenserTicketsBefore = Rs2Inventory.itemQuantity(TICKET_ITEM_ID);
                    dispenserPreValue = getInventoryValue();
                    dispenserLootAttempts = 1;
                    waitingForDispenserLoot = true;
                    Rs2GameObject.interact(dispenser, "Search");
                }
            }
        }
                
                // Pitfall detection logic - using game object detection only
                if (isInUndergroundPit()) {
                    if (isWaitingForRope) {
                        pitRecoveryTarget = ObstacleState.ROPE;
                        isWaitingForRope = false;
                    } else if (currentState.ordinal() <= ObstacleState.ROPE.ordinal()) {
                        pitRecoveryTarget = ObstacleState.ROPE;
                    } else if (isWaitingForLog) {
                        pitRecoveryTarget = ObstacleState.LOG;
                        isWaitingForLog = false;
                    } else if (currentState == ObstacleState.LOG) {
                        pitRecoveryTarget = ObstacleState.LOG;
                    }
                    currentState = ObstacleState.PIT_RECOVERY;
                }
                if (System.currentTimeMillis() - lastObjectCheck > 1000) {
                    cachedDispenserObj = getDispenserObj();
                    lastObjectCheck = System.currentTimeMillis();
                }
                
                // Check if grace period has passed
                boolean gracePeriodActive = (System.currentTimeMillis() - scriptStartTime) < STARTUP_GRACE_PERIOD;

                // ===== ENHANCED PVP DAMAGE DETECTION =====
                // Track health changes to detect PvP damage
                detectPvPDamage();

                // Check for Phoenix Escape trigger - Phoenix necklace missing (skip during grace period)
                if (config.phoenixEscape() && !emergencyEscapeTriggered && !gracePeriodActive) {
                    if (!hasPhoenixNecklace()) {
                        // If already in safe zone (Mage Bank), just go to banking instead of escape
                        if (isInSafeZone()) {
                            Microbot.log("[WildernessNicky] ⚠️ Phoenix necklace missing but already in safe zone - going to bank to regear");
                            currentState = ObstacleState.BANKING;
                        } else {
                            triggerPhoenixEscape("Phoenix necklace missing from inventory/equipment");
                        }
                    }
                }

                // ===== MASS MODE - WAIT FOR HIT THRESHOLDS =====
                // Separate thresholds for clan members vs non-clan players
                if (config.playMode() == WildernessNickyConfig.PlayMode.MASS && !emergencyEscapeTriggered && !gracePeriodActive) {
                    boolean shouldLogout = false;
                    String logoutReason = "";

                    // Check non-clan hits first (more dangerous)
                    if (nonClanHitCount >= config.nonClanHitThreshold()) {
                        shouldLogout = true;
                        logoutReason = "Took " + nonClanHitCount + " hits from NON-CLAN player (" + lastAttackerName + ")";
                    }
                    // Check clan member hits (less urgent)
                    else if (clanMemberHitCount >= config.clanMemberHitThreshold()) {
                        shouldLogout = true;
                        logoutReason = "Took " + clanMemberHitCount + " hits from CLAN member (" + lastAttackerName + ")";
                    }

                    if (shouldLogout) {
                        // TOUGH IT OUT: Only logout if we have >= 150k GP in looting bag
                        if (lootingBagValue >= 150000) {
                            Microbot.log("[WildernessNicky] ⚠️ MASS MODE: " + logoutReason + " - logging out!");
                            Microbot.log("[WildernessNicky] 💰 Looting bag value: " + NumberFormat.getIntegerInstance().format(lootingBagValue) + " gp (>= 150k threshold)");

                            // Save state for re-login
                            massLoggedOutDueToAttack = true;
                            massLogoutTime = System.currentTimeMillis();
                            massLogoutLocation = Rs2Player.getWorldLocation();

                            // Save mass world if configured
                            if (!parsedMassWorlds.isEmpty()) {
                                // Use custom mass world list
                                massWorldToReturn = parsedMassWorlds.get(0); // Always use first world for consistency
                                Microbot.log("[WildernessNicky] 📌 Will return to custom mass world " + massWorldToReturn + " after re-login");
                            } else {
                                int configWorld = getMassWorldFromConfig();
                                if (configWorld > 0) {
                                    // Use text box world number
                                    massWorldToReturn = configWorld;
                                    Microbot.log("[WildernessNicky] 📌 Will return to world " + massWorldToReturn + " after re-login");
                                }
                            }

                            // Try to logout
                            Rs2Player.logout();
                            sleep(600); // Wait 1 tick

                            // Check if logout succeeded
                            if (!Microbot.isLoggedIn()) {
                                Microbot.log("[WildernessNicky] ✅ Successfully logged out! Will re-login in " +
                                    config.massReloginDelayMin() + "-" + config.massReloginDelayMax() + " seconds");
                            } else {
                                // Logout failed (in combat), trigger escape instead
                                Microbot.log("[WildernessNicky] ❌ Logout failed (in combat) - triggering escape");
                                triggerPhoenixEscape(logoutReason + " - logout failed, escaping instead");
                            }
                        } else {
                            // Not enough value yet - tough it out!
                            Microbot.log("[WildernessNicky] 💪 TOUGHING IT OUT: " + logoutReason + " but only " +
                                NumberFormat.getIntegerInstance().format(lootingBagValue) + " gp (need 150k)");
                            // Reset hit counters to give more time before next check
                            nonClanHitCount = Math.max(0, nonClanHitCount - 1);
                            clanMemberHitCount = Math.max(0, clanMemberHitCount - 1);
                        }
                    }
                }

                // ===== SOLO MODE - INSTANT LOGOUT FOR ATTACKABLE PLAYERS OR COMBAT =====
                // Only check if not already attempting logout to avoid resetting timer
                if (config.playMode() == WildernessNickyConfig.PlayMode.SOLO && !emergencyEscapeTriggered && !gracePeriodActive && !attemptingLogout) {
                    // Skip solo mode checks if in safe zone (Mage Bank, banking areas, etc.)
                    if (!isInSafeZone()) {
                        // Only do player detection if NOT in safe zone

                    // Check for ANY attackable player threat (not just skulled)
                    boolean playerThreat = detectAttackablePlayer();
                    Actor interacting = Microbot.getClient().getLocalPlayer().getInteracting();
                    boolean inCombat = interacting != null;

                    // Differentiate between skeleton/NPC combat vs PKer combat
                    boolean isNpcCombat = inCombat && !(interacting instanceof net.runelite.api.Player);
                    boolean isPkerCombat = inCombat && (interacting instanceof net.runelite.api.Player);

                    if (playerThreat || isPkerCombat) {
                        // CRITICAL FIX: If already in PKer combat, ALWAYS escape regardless of GP value
                        // "Tough it out" logic only applies to nearby threats, not active attacks
                        if (!isPkerCombat && lootingBagValue < 150000) {
                            // Only skip escape if it's a NEARBY threat (not attacking yet) and low GP value
                            Microbot.log("[WildernessNicky] 💪 TOUGHING IT OUT: Attackable player nearby but only " +
                                NumberFormat.getIntegerInstance().format(lootingBagValue) + " gp (need 150k) - continuing");
                            return; // Skip logout, continue running course
                        }

                        // REAL PKer THREAT - Need to logout and world hop
                        // This triggers for: (1) In PKer combat regardless of GP, OR (2) Nearby player with >= 150k GP
                        attemptingLogout = true;
                        logoutAttemptStartTime = System.currentTimeMillis();

                        String reason = isPkerCombat ? "In PKer combat" : "Attackable player detected";
                        Microbot.log("[WildernessNicky] ⚠️ " + reason + " - need to logout and world hop!");
                        if (isPkerCombat) {
                            Microbot.log("[WildernessNicky] ⚔️ UNDER ATTACK! Escaping regardless of GP value");
                        } else {
                            Microbot.log("[WildernessNicky] 💰 Looting bag value: " + NumberFormat.getIntegerInstance().format(lootingBagValue) + " gp (>= 150k threshold)");
                        }

                        // Check if we're in combat with NPCs (skeletons) that prevent logout
                        Actor interactingWith = Microbot.getClient().getLocalPlayer().getInteracting();
                        boolean inNpcCombat = interactingWith != null && !(interactingWith instanceof net.runelite.api.Player);

                        if (inNpcCombat) {
                            // In combat with NPCs - need to run away from skeletons first
                            Microbot.log("[WildernessNicky] 🦴 In NPC combat (skeleton) - running to safe area before logout...");

                            // Run back toward start/dispenser area (safe from skeletons)
                            WorldPoint currentLoc = Rs2Player.getWorldLocation();
                            if (currentLoc != null && currentLoc.distanceTo(START_POINT) > 5) {
                                Microbot.log("[WildernessNicky] 🏃 Running toward start area to escape skeleton combat");
                                Rs2Walker.walkTo(START_POINT, 3);
                                sleep(600);
                            }

                            // Try logout anyway (might work if we broke combat)
                            Rs2Player.logout();
                            sleep(600);

                            // If still in combat, keep moving and return to try again next cycle
                            if (Microbot.isLoggedIn() && Microbot.getClient().getLocalPlayer().getInteracting() != null) {
                                Microbot.log("[WildernessNicky] ⚠️ Still in skeleton combat - will retry next cycle");
                                return; // Return to keep trying next cycle
                            }
                        } else {
                            // Not in NPC combat - try direct logout
                            Rs2Player.logout();
                            sleep(600); // Wait 1 tick
                        }

                        // Check if logout succeeded (player is no longer logged in)
                        if (!Microbot.isLoggedIn()) {
                            Microbot.log("[WildernessNicky] ✅ Successfully logged out!");

                            // World hop to find empty course (up to 3 attempts)
                            if (soloModeWorldHopAttempts < MAX_SOLO_WORLD_HOP_ATTEMPTS) {
                                soloModeWorldHopAttempts++;
                                Microbot.log("[WildernessNicky] 🌍 Solo mode world hop attempt " + soloModeWorldHopAttempts + "/" + MAX_SOLO_WORLD_HOP_ATTEMPTS);
                                sleep(2000); // Wait 2 seconds before world hop

                                // Get current world before logout
                                int currentWorld = Rs2Player.getWorld();

                                int nextWorld = -1;

                                // Try custom world list first
                                if (!parsedSoloWorlds.isEmpty()) {
                                    nextWorld = getNextSoloWorld();
                                    Microbot.log("[WildernessNicky] Using custom solo hop world: " + nextWorld);

                                    // If same world, get next one
                                    if (nextWorld == currentWorld && parsedSoloWorlds.size() > 1) {
                                        nextWorld = getNextSoloWorld();
                                    }
                                } else {
                                    // Fallback to random world
                                    nextWorld = net.runelite.client.plugins.microbot.util.security.Login.getRandomWorld(Rs2Player.isMember());

                                    // Make sure we don't hop to the same world
                                    int attempts = 0;
                                    while (nextWorld == currentWorld && attempts < 10) {
                                        nextWorld = net.runelite.client.plugins.microbot.util.security.Login.getRandomWorld(Rs2Player.isMember());
                                        attempts++;
                                    }
                                }

                                if (nextWorld > 0 && nextWorld != currentWorld) {
                                    Microbot.log("[WildernessNicky] Attempting to hop from world " + currentWorld + " to world " + nextWorld);

                                    // Reset cantHopWorld flag before attempting hop
                                    Microbot.cantHopWorld = false;

                                    // Perform world hop
                                    boolean hopSuccess = Microbot.hopToWorld(nextWorld);
                                    if (hopSuccess) {
                                        // Wait for world hop to complete
                                        final int targetWorld = nextWorld;
                                        boolean hopConfirmed = sleepUntil(() -> Rs2Player.getWorld() == targetWorld && Microbot.isLoggedIn(), 15000);

                                        if (hopConfirmed) {
                                            Microbot.log("[WildernessNicky] ✅ Successfully hopped to world " + nextWorld);
                                            // Reset logout flags to allow detection again
                                            attemptingLogout = false;
                                            // Wait a bit before resuming to let world load
                                            sleep(2000, 3000);
                                            return;
                                        } else {
                                            Microbot.log("[WildernessNicky] ⚠️ World hop not confirmed - may have failed");
                                        }
                                    } else {
                                        Microbot.log("[WildernessNicky] ⚠️ Failed to initiate world hop");
                                    }
                                } else {
                                    Microbot.log("[WildernessNicky] ⚠️ No valid next world found");
                                }
                            } else {
                                // Max world hop attempts reached - kill plugin
                                Microbot.log("[WildernessNicky] ❌ Max world hop attempts reached (" + MAX_SOLO_WORLD_HOP_ATTEMPTS + ")");
                                Microbot.log("[WildernessNicky] No empty course found - stopping plugin");
                                sleep(2000);
                                Microbot.stopPlugin(plugin);
                            }
                            return;
                        } else {
                            // Logout failed (in combat) - keep trying to logout while moving
                            Microbot.log("[WildernessNicky] ❌ Logout failed (in combat) - will keep spamming logout attempts");

                            // Check if we've been trying to logout for too long
                            long timeSinceLogoutAttempt = System.currentTimeMillis() - logoutAttemptStartTime;

                            if (timeSinceLogoutAttempt > 10000) {
                                // After 10 seconds of trying, give up and stop plugin
                                Microbot.log("[WildernessNicky] ⚠️ Failed to logout after 10 seconds - stopping plugin to prevent death");
                                sleep(1000);
                                Microbot.stopPlugin(plugin);
                                return;
                            }

                            // Keep spamming logout while moving randomly to break combat
                            Rs2Player.logout();

                            // Move randomly to try to break combat
                            if (!Rs2Player.isMoving()) {
                                WorldPoint currentLoc = Rs2Player.getWorldLocation();
                                if (currentLoc != null) {
                                    // Move in a random direction
                                    int randomX = currentLoc.getX() + (new Random().nextInt(3) - 1);
                                    int randomY = currentLoc.getY() + (new Random().nextInt(3) - 1);
                                    Rs2Walker.walkTo(new WorldPoint(randomX, randomY, currentLoc.getPlane()), 0);
                                }
                            }
                        }
                    } else if (isNpcCombat) {
                        // SKELETON/NPC COMBAT - Run to safe spot to logout
                        Microbot.log("[WildernessNicky] 🦴 Skeleton/NPC combat - running to safe spot to logout!");
                        attemptingLogout = true;
                        logoutAttemptStartTime = System.currentTimeMillis();
                        triggerPhoenixEscape("Skeleton/NPC combat - finding safe spot to logout");
                    }
                    } // End of !isInSafeZone() check
                }

                // PROACTIVE PLAYER DETECTION - Scan for PKers before they attack
                // Only active in PROACTIVE_ONLY mode (disabled in MASS and SOLO modes)
                if (config.enableProactivePlayerDetection() &&
                    config.playMode() == WildernessNickyConfig.PlayMode.PROACTIVE_ONLY &&
                    !emergencyEscapeTriggered) {
                    if (detectNearbyThreat()) {
                        // TOUGH IT OUT: Only escape if we have >= 150k GP in looting bag
                        if (lootingBagValue < 150000) {
                            Microbot.log("[WildernessNicky] 💪 TOUGHING IT OUT: Threatening player nearby but only " +
                                NumberFormat.getIntegerInstance().format(lootingBagValue) + " gp (need 150k) - continuing");
                        } else {
                            Microbot.log("[WildernessNicky] 💰 Looting bag value: " + NumberFormat.getIntegerInstance().format(lootingBagValue) + " gp (>= 150k threshold)");
                            triggerPhoenixEscape("Threatening player detected within 15 tiles (Proactive Detection)");
                        }
                    }
                }

                // SMARTER ESCAPE LOGIC - Check for health percentage emergency escape (skip during grace period)
                if (config.leaveAtHealthPercent() > 0 && !emergencyEscapeTriggered && !gracePeriodActive) {
                    if (Rs2Player.getHealthPercentage() <= config.leaveAtHealthPercent()) {
                        // SMART: Differentiate between PKer attack vs agility fail
                        boolean threatNearby = isPlayerThreatNearby(); // Quick check without full scan
                        boolean hasFood = Rs2Inventory.contains("Anglerfish") ||
                                         Rs2Inventory.contains("Manta ray") ||
                                         Rs2Inventory.contains("Karambwan");

                        if (threatNearby) {
                            // REAL THREAT - Escape immediately
                            triggerPhoenixEscape("Low HP (" + (int)Rs2Player.getHealthPercentage() + "%) + PKer nearby");
                        } else if (!hasFood) {
                            // No food and low HP (even if no threat) - better escape
                            triggerPhoenixEscape("Low HP (" + (int)Rs2Player.getHealthPercentage() + "%) + no food available");
                        } else {
                            // Just agility damage + have food - heal and continue
                            Microbot.log("[WildernessNicky] Low HP from agility fail - eating food and continuing");
                            eatFood();
                            sleep(1000, 1500); // Wait for food
                        }
                    }
                }

                // ===== SAFETY CHECK: Reset escape if already safe (e.g., after relogging) =====
                if (emergencyEscapeTriggered && isInSafeZone() && canExitEscapeMode()) {
                    Microbot.log("[WildernessNicky] ✅ Already in safe zone after login - resetting escape mode");
                    resetEscapeMode();
                    currentState = ObstacleState.BANKING; // Go to banking to regear
                }

                // Handle Emergency Escape if triggered
                if (emergencyEscapeTriggered) {
                    handleEmergencyEscape();
                    return; // Skip normal state handling during escape
                }

                // ===== ADVANCED WILDERNESS COMBAT SYSTEMS =====
                // Update wilderness level
                updateWildernessLevel();

                // Update teleblock status
                updateTeleBlockStatus();

                // Handle prayer switching (if in wilderness)
                // Note: We attempt prayer switching even if prayer points check fails, as the check itself may fail due to skill cache issues
                if (Rs2Pvp.isInWilderness()) {
                    try {
                        // Only skip if we can reliably determine player has 0 prayer points
                        boolean hasPrayer = true;
                        try {
                            hasPrayer = Rs2Player.hasPrayerPoints();
                        } catch (Exception e) {
                            // If prayer check fails, assume we have prayer and continue (better to try than skip)
                            Microbot.log("[WildernessNicky] Prayer check failed, assuming prayer available: " + e.getMessage());
                        }

                        if (hasPrayer) {
                            long currentTime = System.currentTimeMillis();

                            if (config.useProjectilePrayerSwitching()) {
                                // PRIMARY: Projectile-based switching (check every 50ms for ranged/magic)
                                if (currentTime - lastProjectileCheckTime >= PROJECTILE_CHECK_INTERVAL) {
                                    handleProjectilePrayerSwitching();
                                    lastProjectileCheckTime = currentTime;
                                }

                                // FALLBACK: Melee detection for attacks without projectiles
                                // Only runs if no projectiles are being tracked
                                if (incomingProjectiles.isEmpty()) {
                                    handleMeleeDetection();
                                }
                            } else {
                                // Legacy: weapon-based switching only (not recommended)
                                handle1TickPrayerSwitching();
                            }
                        }
                    } catch (Exception e) {
                        Microbot.log("[WildernessNicky] Error in prayer switching handler: " + e.getMessage());
                    }
                }

                // Location tracking for stuck detection
                handleLocationTracking();
                
                // Position-based timeout and retry system
                handlePositionTimeoutLogic();
                
                switch (currentState) {
                    case INIT: 
                        // DISABLED: Looting bag check corrupts inventory action data
                        // checkLootingBagOnStartup(); // Sync initial looting bag value if present
                        currentState = ObstacleState.PIPE;
                        break;
                    case STARTUP_INVENTORY_CHECK: handleStartupInventoryCheck(); break;
                    case STARTUP_BANKING: handleStartupBanking(); break;
                    case START: handleStart(); break;
                    case PIPE: handlePipe(); break;
                    case ROPE: handleRope(); break;
                    case STONES: pipeJustCompleted = false; handleStones(); break;
                    case LOG: handleLog(); break;
                    case ROCKS: handleRocks(); break;
                    case DISPENSER: handleDispenser(); break;
                    case CONFIG_CHECKS: handleConfigChecks(); break;
                    case WORLD_HOP_1: handleWorldHop1(); break;
                    case WORLD_HOP_2: handleWorldHop2(); break;
                    case WALK_TO_LEVER: handleWalkToLever(); break;
                    case INTERACT_LEVER: handleInteractLever(); break;
                    case BANKING: handleBanking(); break;
                    case POST_BANK_CONFIG: handlePostBankConfig(); break;
                    case WALK_TO_COURSE: handleWalkToCourse(); break;
                    case SWAP_BACK: handleSwapBack(); break;
                    case PIT_RECOVERY: recoverFromPit(); break;
                    case EMERGENCY_ESCAPE: handleEmergencyEscape(); break;
                    case GE_BUY_LOOTING_BAG: handleGEBuyLootingBag(); break;
                    case GE_BUY_NOTED_LOOTING_BAG: handleGEBuyNotedLootingBag(); break;
                    case UNNOTE_LOOTING_BAG: handleUnnoteLootingBag(); break;
                }
                if (lastObstacleInteractTime > 0 && lastObstaclePosition != null && System.currentTimeMillis() - lastObstacleInteractTime > 2000) {
                    WorldPoint currentPos = Rs2Player.getWorldLocation();
                    if (currentPos != null && currentPos.equals(lastObstaclePosition)) {
                        switch (currentState) {
                            case ROPE: isWaitingForRope = false; break;
                            case LOG: isWaitingForLog = false; break;
                            case STONES: isWaitingForStones = false; break;
                            case PIPE: if (!pipeJustCompleted) { isWaitingForPipe = false; } break;
                        }
                        lastObstacleInteractTime = 0;
                        lastObstaclePosition = null;
                    }
                }
            } catch (Exception ex) {
                Microbot.logStackTrace(this.getClass().getSimpleName(), ex);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
        return true;
    }

    /**
     * Shuts down the script and resets state.
     */
    @Override
    public void shutdown() {
        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled()) {
            mainScheduledFuture.cancel(true);
        }
        lapCount = 0;
        dispenserLoots = 0;
        startTime = 0;
        currentState = ObstacleState.START;
        pitRecoveryTarget = null;
        isWaitingForPipe = false;
        isWaitingForRope = false;
        isWaitingForStones = false;
        isWaitingForLog = false;
        dispenserLootAttempts = 0;
        ropeRecoveryWalked = false;
        pipeJustCompleted = false;
        // Reset position tracking variables
        lastTrackedPosition = null;
        positionLastChangedTime = 0;
        lastPositionCheckTime = 0;
        currentStateRetryAttempts = 0;
        isInRetryMode = false;
        
        // Reset world hopping variables
        worldHopRetryCount = 0;
        worldHopRetryStartTime = 0;
        soloModeWorldHopAttempts = 0;

        // Reset mass mode variables
        massLoggedOutDueToAttack = false;
        massLogoutTime = 0;
        massWorldToReturn = -1;
        massLogoutLocation = null;
        
        // Reset web walking variables
        webWalkStartTime = 0;
        
        // Reset Phoenix Escape variables
        phoenixEscapeTriggered = false;
        phoenixEscapeStartTime = 0;
        
        // Reset Emergency Escape variables
        emergencyEscapeTriggered = false;
        emergencyEscapeStartTime = 0;
        hasEquippedPhoenixNecklace = false;
        hasClimbedRocks = false;
        hasOpenedGate = false;
        escapeStep2StartTime = 0;
        
        // Reset location tracking variables
        lastPlayerLocation = null;
        lastLocationChangeTime = 0;
        lastStateBeforeStuck = null;
        
        // Reset looting bag variables
        needsLootingBagActivation = false;

        // Reset banked loot tracking (keep data across shutdowns - don't clear)
        // bankedLoot.clear(); // Commented out to persist loot data
        // totalBankedValue = 0;
        // totalBankingTrips = 0;

        // Reset PvP damage detection
        previousHealth = 100;
        lastHealthCheckTime = 0;
        recentlyTookPvPDamage = false;
        lastPvPDamageTime = 0;
        pvpHitCount = 0;

        // Reset escape step retry counters
        escapeEquipNecklaceAttempts = 0;
        escapeClimbRocksAttempts = 0;
        escapeOpenGateAttempts = 0;
        escapeWalkToMageBankAttempts = 0;
        
        // Reset drop location variables
        shouldDropAfterDispenser = false;
        
        // Reset death tracking variables
        deathDetected = false;

        // Reset looting bag value tracking
        lootingBagValue = 0;
        lootingBagContents.clear();
        hasCheckedLootingBagOnStartup = false;

        // Reset incomplete lap detection
        incompleteLapDetected = false;
        dispenserInteractAttempts = 0;

        // Reset PvP hit tracking
        pvpHitCount = 0;
        clanMemberHitCount = 0;
        nonClanHitCount = 0;
        lastAttackerName = "";

        // Reset solo mode / logout attempts
        attemptingLogout = false;
        logoutAttemptStartTime = 0;

        // Reset projectile-based prayer switching
        incomingProjectiles.clear();
        lastProjectileCheckTime = 0;
        lastProjectileId = -1;
        lastProjectileDetectionTime = 0;
        projectilesDetectedCount = 0;
        
        Microbot.log("[WildernessNickyScript] shutdown called");
        super.shutdown();
    }

    private void info(String msg) {
        // Only log dispenser value
        Microbot.log(msg);
        System.out.println(msg);
    }

    public boolean runeliteClientPluginsMicrobotWildernessagilityWildernessNickyScript_run(WildernessNickyConfig config) {
        return run(config);
    }

    /**
     * ENHANCED DEATH WALKING SYSTEM
     * Handles player death with smart respawn detection and auto-return
     */
    private void handlePlayerDeath() {
        Microbot.log("[WildernessNicky] ☠️ DEATH DETECTED - Initiating death recovery");

        // Option 1: User wants to logout after death
        if (!config.runBack() && config.logoutAfterDeath()) {
            Microbot.log("[WildernessNicky] Config: Logout after death enabled - logging out");
            sleep(12000); // Wait for respawn
            attemptLogoutUntilLoggedOut();
            Microbot.stopPlugin(plugin);
            return;
        }

        // Option 2: User wants to stop plugin after death
        if (!config.runBack() && !config.logoutAfterDeath()) {
            Microbot.log("[WildernessNicky] Config: Run back disabled - stopping plugin");
            sleep(12000);
            Microbot.stopPlugin(plugin);
            return;
        }

        // Option 3: AUTO DEATH WALKING - Run back and resume
        Microbot.log("[WildernessNicky] Config: Auto death walking enabled - preparing to run back");

        // Wait for respawn animation to complete
        sleep(12000, 15000);

        // Detect respawn location
        WorldPoint respawnLocation = Rs2Player.getWorldLocation();
        String respawnArea = detectRespawnArea(respawnLocation);

        Microbot.log("[WildernessNicky] Respawned at: " + respawnArea + " (" + respawnLocation + ")");

        // Check if we have items to recover (shouldn't in wilderness, but check anyway)
        boolean hasItemsToRecover = !Rs2Inventory.isEmpty();

        // Set flag to buy looting bag from GE (we lost it on death)
        needsToBuyLootingBag = true;

        // Go to GE first to buy looting bag, then bank
        Microbot.log("[WildernessNicky] Going to GE to buy new looting bag after death");
        currentState = ObstacleState.GE_BUY_LOOTING_BAG;

        // Reset emergency flags since we died
        emergencyEscapeTriggered = false;
        phoenixEscapeTriggered = false;

        // Force re-gear by going through banking state after GE
        forceBankNextLoot = true;
    }

    /**
     * Detect which area player respawned in
     * Returns: "Edgeville", "Ferox Enclave", "Lumbridge", "Unknown"
     */
    private String detectRespawnArea(WorldPoint location) {
        if (location == null) return "Unknown";

        int x = location.getX();
        int y = location.getY();
        int plane = location.getPlane();

        // Edgeville respawn (most common for wilderness)
        if (x >= 3087 && x <= 3098 && y >= 3488 && y <= 3500 && plane == 0) {
            return "Edgeville";
        }

        // Ferox Enclave
        if (x >= 3125 && x <= 3155 && y >= 3625 && y <= 3655 && plane == 0) {
            return "Ferox Enclave";
        }

        // Lumbridge
        if (x >= 3218 && x <= 3231 && y >= 3211 && y <= 3230 && plane == 0) {
            return "Lumbridge";
        }

        // Falador
        if (x >= 2964 && x <= 2970 && y >= 3378 && y <= 3387 && plane == 0) {
            return "Falador";
        }

        return "Unknown (" + x + ", " + y + ")";
    }

    private boolean waitForInventoryChanges(int timeoutMs) {
        List<Rs2ItemModel> before = Rs2Inventory.items().collect(Collectors.toList());
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs && isRunning()) {
            List<Rs2ItemModel> after = Rs2Inventory.items().collect(Collectors.toList());
            if (after.size() != before.size()) return true;
            sleep(50);
        }
        return false;
    }

    public int getInventoryValue() {
        int mainInventoryValue = Rs2Inventory.items().filter(Objects::nonNull).mapToInt(Rs2ItemModel::getPrice).sum();
        
        // Now we track looting bag value via chat messages - much more accurate!
        return mainInventoryValue + lootingBagValue;
    }
    
    /**
     * Gets just the looting bag value
     */
    public int getLootingBagValue() {
        return lootingBagValue;
    }
    
    /**
     * Gets the total value including looting bag contents (tracked via chat messages)
     * @deprecated Use getInventoryValue() instead - it now includes looting bag value
     */
    @Deprecated
    public int getTotalValueWithLootingBag() {
        return getInventoryValue();
    }
    

    public String getRunningTime() {
        long elapsed = System.currentTimeMillis() - startTime;
        long seconds = (elapsed / 1000) % 60;
        long minutes = (elapsed / (1000 * 60)) % 60;
        long hours = (elapsed / (1000 * 60 * 60));
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void setPlugin(WildernessNickyPlugin plugin) {
        this.plugin = plugin;
    }

    private TileObject getDispenserObj() {
        return Rs2GameObject.getAll(o -> o.getId() == DISPENSER_ID, 104).stream().findFirst().orElse(null);
    }
    private TileObject getObstacleObj(int index) {
        return Rs2GameObject.getAll(o -> o.getId() == obstacles.get(index).getObjectId(), 104).stream().findFirst().orElse(null);
    }

    /**
     * Checks if the player is currently falling from an obstacle
     * @return true if player is in the falling animation
     */
    private boolean isFalling() {
        return Rs2Player.getAnimation() == FALLING_ANIMATION;
    }

    /**
     * Checks if the player is in the underground pit OR currently falling
     * @return true if player is in pit or falling
     */
    private boolean isInUndergroundPit() {
        // Check for the underground object that only exists in the pit
        boolean inPit = Rs2GameObject.getAll(o -> o.getId() == UNDERGROUND_OBJECT_ID, 104).stream().findFirst().orElse(null) != null;
        // Also check if player is falling - early detection before hitting the pit
        boolean falling = isFalling();
        return inPit || falling;
    }
    private void recoverFromPit() {
        // If player is currently falling, wait for them to land
        if (isFalling()) {
            Microbot.log("[WildernessNicky] Player is falling - waiting for landing...");
            sleepUntil(() -> !isFalling(), 3000);
            return;
        }

        // Check if we're still in the pit using game object detection
        if (isInUndergroundPit()) {
            // Immediately refresh ladder object before attempting to interact
            List<TileObject> ladders = Rs2GameObject.getAll(o -> o.getId() == 17385, 104);
            TileObject ladderObj = ladders.isEmpty() ? null : ladders.get(0);
            long now = System.currentTimeMillis();
            if (ladderObj != null && Rs2Player.getWorldLocation().distanceTo(ladderObj.getWorldLocation()) <= 50) {
                // Only attempt to interact with the ladder every 2 seconds
                if (now - lastLadderInteractTime > 2000) {
                    // Refresh ladder object again just before interaction
                    List<TileObject> laddersNow = Rs2GameObject.getAll(o -> o.getId() == 17385, 104);
                    ladderObj = laddersNow.isEmpty() ? null : laddersNow.get(0);
                    Microbot.log("[WildernessNicky] Climbing out of pit...");
                    Rs2GameObject.interact(ladderObj, "Climb-up");
                    lastLadderInteractTime = now;
                }
            }
            return; // Return here to let the next tick handle the state transition
        }

        // If we're here, we've successfully climbed out of the pit
        if (pitRecoveryTarget != null) {
            switch (pitRecoveryTarget) {
                case ROPE:
                    // Fast walk back to rope, but only once
                    WorldPoint ropePoint = new WorldPoint(3005, 3953, 0);
                    if (!ropeRecoveryWalked) {
                        Rs2Walker.walkFastCanvas(ropePoint);
                        ropeRecoveryWalked = true;
                    }
                    if (Rs2Player.getWorldLocation().distanceTo(ropePoint) > 1) {
                        // Not close enough yet? wait for main loop to walk
                        return;
                    }
                    sleep(300, 600);

                    // Now interact with rope
                    TileObject rope = getObstacleObj(1);
                    if (rope != null && !Rs2Player.isMoving()) {
                        isWaitingForRope = false;
                        ropeStartXp = Microbot.getClient().getSkillExperience(AGILITY);
                        boolean interacted = Rs2GameObject.interact(rope);
                        if (interacted) {
                            isWaitingForRope = true;
                        }
                    }
                    break;

                case LOG:
                    // Wide range detection for log, just like ladder detection
                    List<TileObject> logs = Rs2GameObject.getAll(o -> o.getId() == obstacles.get(3).getObjectId(), 104);
                    TileObject log = logs.isEmpty() ? null : logs.get(0);
                    if (log != null) {
                        isWaitingForLog = false;
                        boolean interacted = Rs2GameObject.interact(log);
                        if (interacted) {
                            isWaitingForLog = true;
                            sleep(300, 600);
                        }
                    }
                    break;

                default:
                    break;
            }

            currentState = pitRecoveryTarget;
            pitRecoveryTarget = null;
            ropeRecoveryWalked = false; // Reset after recovery
        } else {
            // This should never happen now that we properly set pitRecoveryTarget
            WorldPoint ropeStart = new WorldPoint(3005, 3953, 0);
            Rs2Walker.walkFastCanvas(ropeStart);
            sleepUntil(() -> !Rs2Player.isMoving(), 5000);
            currentState = ObstacleState.ROPE;
            isWaitingForRope = false;
            ropeRecoveryWalked = false; // Reset if no recovery target
        }
    }
    private void handlePipe() {
        if (isWaitingForPipe) {
            // Use XP drop to confirm pipe completion
            if (waitForXpChange(pipeStartXp, getXpTimeout())) {
                isWaitingForPipe = false;
                pipeJustCompleted = false; // Clear after XP drop
                currentState = ObstacleState.ROPE;
                return;
            }
            // Fail fast: if no animation/movement after failTimeoutMs, abort and retry
            if (hasTimedOutSince(pipeInteractionStartTime, config.failTimeoutMs()) && !Rs2Player.isAnimating() && !Rs2Player.isMoving()) {
                isWaitingForPipe = false;
                pipeJustCompleted = false;
                return;
            }
            return;
        }
        WorldPoint loc = Rs2Player.getWorldLocation();
        if (!Rs2Player.isAnimating() && !Rs2Player.isMoving()) {
            // Player must be within 4 tiles of (3004, 3937, 0) to interact with the pipe at (3004, 3938, 0)
            WorldPoint pipeTile = new WorldPoint(3004, 3938, 0);
            WorldPoint pipeFrontTile = new WorldPoint(3004, 3937, 0);
            int distanceToPipeFront = loc.distanceTo(pipeFrontTile);
            if (distanceToPipeFront > 4) {
                if (!isAt(pipeFrontTile, 4)) {
                    Rs2Walker.walkTo(pipeFrontTile, 2);
                }
                return;
            }
            // Find the pipe object at the exact tile (3004, 3938, 0)
            TileObject pipe = Rs2GameObject.getAll(o -> o.getId() == obstacles.get(0).getObjectId() &&
                                                    o.getWorldLocation().equals(pipeTile), 10)
                                        .stream().findFirst().orElse(null);
            if (pipe == null) {
                return;
            }
            boolean interacted = Rs2GameObject.interact(pipe);
            if (interacted) {
                isWaitingForPipe = true;
                pipeJustCompleted = true; // Set immediately after interaction
                pipeStartXp = Microbot.getClient().getSkillExperience(AGILITY);
                pipeInteractionStartTime = System.currentTimeMillis();
            }
        }
    }
    private void handleRope() {
        pipeJustCompleted = false;
        WorldPoint loc = Rs2Player.getWorldLocation();
        if (isWaitingForRope) {
            // Check for pitfall while waiting using game object detection
            if (isInUndergroundPit()) {
                if (pitRecoveryTarget != ObstacleState.ROPE) {
                    pitRecoveryTarget = ObstacleState.ROPE;
                    currentState = ObstacleState.PIT_RECOVERY;
                }
                isWaitingForRope = false;
                return;
            }
            // Check for XP gain (completion) - but don't wait for XP orb, use immediate detection
            if (Microbot.getClient().getSkillExperience(AGILITY) > ropeStartXp) {
                isWaitingForRope = false;
                currentState = ObstacleState.STONES;
                return;
            }
            // Fail fast: if no animation/movement after failTimeoutMs, abort and retry
            if (hasTimedOutSince(lastObstacleInteractTime, config.failTimeoutMs()) && !Rs2Player.isAnimating() && !Rs2Player.isMoving()) {
                isWaitingForRope = false;
                return;
            }
        }
        if (!Rs2Player.isAnimating() && !Rs2Player.isMoving() && !isWaitingForRope) {
            TileObject rope = getObstacleObj(1);
            if (rope != null) {
                boolean interacted = Rs2GameObject.interact(rope);
                if (interacted) {
                    isWaitingForRope = true;
                    ropeStartXp = Microbot.getClient().getSkillExperience(AGILITY);
                    lastObstacleInteractTime = System.currentTimeMillis();
                    lastObstaclePosition = Rs2Player.getWorldLocation();
                }
            }
        } else {
            // Check for pit fall while moving/animating using game object detection
            if (isInUndergroundPit()) {
                isWaitingForRope = false;
                pitRecoveryTarget = ObstacleState.ROPE;
                currentState = ObstacleState.PIT_RECOVERY;
            }
        }
    }
    private void handleStones() {
        if (isWaitingForStones) {
            WorldPoint loc = Rs2Player.getWorldLocation();
            int currentXp = Microbot.getClient().getSkillExperience(AGILITY);
            boolean yPassed = loc != null && loc.getY() > 3961;
            boolean xPassed = loc != null && loc.getX() == 2996;
            boolean xpPassed = currentXp > stonesStartXp;
            if (yPassed) {
                isWaitingForStones = false;
                return;
            }
            if (xPassed) {
                isWaitingForStones = false;
                currentState = ObstacleState.LOG;
                return;
            }
            if (xpPassed) {
                isWaitingForStones = false;
                currentState = ObstacleState.LOG;
                return;
            }
            // Fail fast: only if not animating, not moving, and not making progress
            if (hasTimedOutSince(lastObstacleInteractTime, config.failTimeoutMs())
                && !Rs2Player.isAnimating()
                && !Rs2Player.isMoving()
                && !yPassed && !xPassed && !xpPassed) {
                isWaitingForStones = false;
                return;
            }
            return;
        }
        
        // Only attempt interaction if not already waiting and not animating/moving
        if (!Rs2Player.isAnimating() && !Rs2Player.isMoving() && !isWaitingForStones) {
            WorldPoint loc = Rs2Player.getWorldLocation();
            TileObject stones = getObstacleObj(2);
            if (stones != null) {
                boolean interacted = Rs2GameObject.interact(stones);
                if (interacted) {
                    isWaitingForStones = true;
                    stonesStartXp = Microbot.getClient().getSkillExperience(AGILITY);
                    lastObstacleInteractTime = System.currentTimeMillis();
                    lastObstaclePosition = Rs2Player.getWorldLocation();
                }
            }
        }
    }
    private void handleLog() {
        if (isWaitingForLog) {
            WorldPoint loc = Rs2Player.getWorldLocation();
            boolean xCoordPassed = loc != null && loc.getX() == 2994;
            boolean xpPassed = Microbot.getClient().getSkillExperience(AGILITY) > logStartXp;
            if (xCoordPassed || xpPassed) {
                isWaitingForLog = false;
                currentState = ObstacleState.ROCKS;
                return;
            }
            if (isInUndergroundPit()) {
                if (pitRecoveryTarget != ObstacleState.LOG) {
                    pitRecoveryTarget = ObstacleState.LOG;
                    currentState = ObstacleState.PIT_RECOVERY;
                }
                return;
            }
            // Fail fast: if no animation/movement after failTimeoutMs, abort and retry
            if (hasTimedOutSince(lastObstacleInteractTime, config.failTimeoutMs()) && !Rs2Player.isAnimating() && !Rs2Player.isMoving()) {
                isWaitingForLog = false;
                return;
            }
        }
        if (!Rs2Player.isAnimating() && !Rs2Player.isMoving()) {
            // Clear inventory before log obstacle if configured (not after dispenser)
            if (!shouldDropAfterDispenserNow()) {
                clearInventoryIfNeeded();
            }
            
            TileObject log = getObstacleObj(3);
            if (log == null) {
                List<TileObject> logs = Rs2GameObject.getAll(o -> o.getId() == obstacles.get(3).getObjectId(), 104);
                log = logs.isEmpty() ? null : logs.get(0);
            }
            if (log != null) {
                boolean interacted = Rs2GameObject.interact(log);
                if (interacted) {
                    isWaitingForLog = true;
                    logStartXp = Microbot.getClient().getSkillExperience(AGILITY);
                    lastObstacleInteractTime = System.currentTimeMillis();
                    lastObstaclePosition = Rs2Player.getWorldLocation();
                }
            }
        } else {
            // Check for pit fall while moving/animating using game object detection
            if (isInUndergroundPit()) {
                isWaitingForLog = false;
                pitRecoveryTarget = ObstacleState.LOG;  // Set recovery target for wide detection
                currentState = ObstacleState.PIT_RECOVERY;
            }
        }
    }
    private void handleRocks() {
        WorldPoint loc = Rs2Player.getWorldLocation();
        if (loc != null && loc.getY() <= 3933) {
            // Get fresh dispenser object for immediate use
            TileObject freshDispenser = getDispenserObj();
            cachedDispenserObj = freshDispenser;
            lastObjectCheck = System.currentTimeMillis();

            currentState = ObstacleState.DISPENSER;

            // Immediate interaction without waiting for next tick
            if (!Rs2Player.isAnimating() && freshDispenser != null) {
                dispenserTicketsBefore = Rs2Inventory.itemQuantity(TICKET_ITEM_ID);
                dispenserPreValue = getInventoryValue();
                dispenserLootAttempts = 1;
                waitingForDispenserLoot = true;
                Rs2GameObject.interact(freshDispenser, "Search");
            }
            return;
        }

        if (!Rs2Player.isAnimating() && !Rs2Player.isMoving()) {
            // Direct world point interaction - 50/50 chance between the two valid rocks
            WorldPoint rock1 = new WorldPoint(2995, 3936, 0); // Valid rock 1
            WorldPoint rock2 = new WorldPoint(2994, 3936, 0); // Valid rock 2
            
            WorldPoint targetRock = new Random().nextBoolean() ? rock1 : rock2;
            
            if (Rs2GameObject.interact(targetRock, "Climb")) {
                // Monitor Y coordinate in real-time for immediate transition
                boolean transitioned = sleepUntil(() -> {
                    WorldPoint currentLoc = Rs2Player.getWorldLocation();
                    if (currentLoc != null && currentLoc.getY() <= 3934) {
                        return true;
                    }
                    return false;
                }, 5000); // 5 second timeout for coordinate monitoring
                
                if (transitioned) {
                    // Immediate transition to dispenser
                    TileObject freshDispenser = getDispenserObj();
                    cachedDispenserObj = freshDispenser;
                    lastObjectCheck = System.currentTimeMillis();
                    currentState = ObstacleState.DISPENSER;
                    if (freshDispenser != null) {
                        dispenserTicketsBefore = Rs2Inventory.itemQuantity(TICKET_ITEM_ID);
                        dispenserPreValue = getInventoryValue();
                        dispenserLootAttempts = 1;
                        waitingForDispenserLoot = true;
                        
                        // Start monitoring rock climb pose animation
                        waitingForRockClimbCompletion = true;
                        // Don't interact with dispenser yet - wait for pose animation 737 to finish
                    }
                    return;
                } else {
                    // Fallback: use XP detection if coordinate monitoring fails
                    int startExp = Microbot.getClient().getSkillExperience(AGILITY);
                    if (waitForXpChange(startExp, 3000)) { // Shorter timeout for fallback
                        Microbot.log("[WildernessNicky] XP fallback successful, transitioning to dispenser");
                        TileObject freshDispenser = getDispenserObj();
                        cachedDispenserObj = freshDispenser;
                        lastObjectCheck = System.currentTimeMillis();
                        currentState = ObstacleState.DISPENSER;
                        if (freshDispenser != null) {
                            dispenserTicketsBefore = Rs2Inventory.itemQuantity(TICKET_ITEM_ID);
                            dispenserPreValue = getInventoryValue();
                            dispenserLootAttempts = 1;
                            waitingForDispenserLoot = true;
                            
                            // Start monitoring rock climb pose animation
                            Microbot.log("[WildernessNicky] Starting rock climb pose animation monitoring...");
                            waitingForRockClimbCompletion = true;
                            // Don't interact with dispenser yet - wait for pose animation 737 to finish
                        }
                        return;
                    }
                }
            }
            // Fallback: if player Y < 3934, consider rocks completed
            loc = Rs2Player.getWorldLocation();
            if (loc != null && loc.getY() < 3934) {
                currentState = ObstacleState.DISPENSER;
            }
        }
    }
    private void handleDispenser() {
        TileObject dispenser = cachedDispenserObj;
        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        if (dispenser == null || playerLoc == null) return;
        if (playerLoc.distanceTo(dispenser.getWorldLocation()) > 20) return;

        // CHECK 1: If incomplete lap was detected, start a new lap immediately
        if (incompleteLapDetected) {
            Microbot.log("[WildernessNicky] Incomplete lap - resetting to PIPE to complete full lap");
            currentState = ObstacleState.PIPE;
            incompleteLapDetected = false;
            dispenserInteractAttempts = 0;
            waitingForDispenserLoot = false;
            dispenserLootAttempts = 0;
            return;
        }

        // CHECK 2: If we've tried to interact too many times without success, do a new lap
        if (dispenserInteractAttempts >= MAX_DISPENSER_INTERACT_ATTEMPTS) {
            Microbot.log("[WildernessNicky] ⚠️ Failed to loot dispenser after " + MAX_DISPENSER_INTERACT_ATTEMPTS + " attempts");
            Microbot.log("[WildernessNicky] Starting fresh lap - likely incomplete lap OR entrance fee not paid");

            // Reset entrance fee flag - if we couldn't loot, we probably didn't pay
            if (entranceFeePaid) {
                Microbot.log("[WildernessNicky] Resetting entrance fee flag - no loot received suggests fee wasn't actually paid");
                entranceFeePaid = false;

                // Check if we have enough coins to pay again
                int coinCount = Rs2Inventory.itemQuantity(COINS_ID);
                if (coinCount < 150000) {
                    Microbot.log("[WildernessNicky] ⚠️ Not enough coins to re-pay entrance fee (" + coinCount + " < 150000)");
                    Microbot.log("[WildernessNicky] Going to Mage Bank to regear...");
                    currentState = ObstacleState.BANKING;
                    dispenserInteractAttempts = 0;
                    waitingForDispenserLoot = false;
                    dispenserLootAttempts = 0;
                    return;
                }
            }

            currentState = ObstacleState.PIPE;
            dispenserInteractAttempts = 0;
            waitingForDispenserLoot = false;
            dispenserLootAttempts = 0;
            return;
        }

        // Looting bag value is tracked via chat messages
        cachedInventoryValue = getInventoryValue();

        int currentTickets = Rs2Inventory.itemQuantity(TICKET_ITEM_ID);

        // If we're waiting for loot, check for ticket gain regardless of animation state
        if (waitingForDispenserLoot) {
            if (Rs2Inventory.itemQuantity(TICKET_ITEM_ID) > dispenserTicketsBefore) {
                long now = System.currentTimeMillis();
                if (lastLapTimestamp > 0) {
                    previousLapTime = now - lastLapTimestamp;
                    if (previousLapTime < fastestLapTime) {
                        fastestLapTime = previousLapTime;
                    }
                }
                lastLapTimestamp = now;
                dispenserLoots++;
                lapCount++;
                int dispenserValue = getInventoryValue() - dispenserPreValue;
                String formattedValue = NumberFormat.getIntegerInstance().format(dispenserValue);
                info("Dispenser Value: " + formattedValue);

                // Randomly sync looting bag contents for GUI display (every 3-7 laps)
                if (shouldSyncLootingBag()) {
                    Microbot.log("[WildernessNicky] 📦 Checking looting bag to sync contents for GUI...");
                    openLootingBagForSync();
                }

                // Generate new random drop location for this lap (if in Random mode)
                generateRandomDropLocation();

                // Clear inventory after dispenser if configured
                if (shouldDropAfterDispenserNow()) {
                    clearInventoryIfNeeded();
                }

                // Reset dispenser attempt counter on success
                dispenserInteractAttempts = 0;

                currentState = ObstacleState.CONFIG_CHECKS;
                dispenserLootAttempts = 0;
                waitingForDispenserLoot = false;
            }
            return;
        }

        // If the player is already animating (interacting with the dispenser), do not interact again
        if (Rs2Player.isAnimating()) return;

        // Try to interact with the dispenser every tick until animation starts
        if (dispenserLootAttempts == 0) {
            dispenserPreValue = getInventoryValue();
            dispenserTicketsBefore = currentTickets;
            Rs2GameObject.interact(dispenser, "Search");
            waitingForDispenserLoot = true;
            dispenserLootAttempts = 1; // Only try once, now wait for loot
            dispenserInteractAttempts++; // Increment attempt counter
            Microbot.log("[WildernessNicky] Dispenser interact attempt " + dispenserInteractAttempts + "/" + MAX_DISPENSER_INTERACT_ATTEMPTS);
        } else if (dispenserLootAttempts == 1) {
            // If for some reason we didn't get loot after a while, allow retry or fallback
            // (Optional: add a timeout here if needed)
        }
    }
    private void handleConfigChecks() {
        TileObject dispenser = cachedDispenserObj;
        if (dispenser == null) return;
        int ticketCount = Rs2Inventory.itemQuantity(TICKET_ITEM_ID);
        if (ticketCount >= config.useTicketsWhen()) {
            boolean didInteract = Rs2Inventory.interact(TICKET_ITEM_ID, "Use");
            if (didInteract) {
                didInteract = Rs2GameObject.interact(dispenser, "Use");
                if (didInteract) {
                    sleepUntil(() -> Rs2Inventory.itemQuantity(TICKET_ITEM_ID) < ticketCount, 2000);
                }
            }
        }
        // Force banking if config.bankAfterDispensers() > 0 and dispenserLoots >= threshold
        if (config.bankAfterDispensers() > 0 && dispenserLoots >= config.bankAfterDispensers()) {
            // Player Monitor not used - built-in anti-PK system is sufficient
            if (config.enableWorldHop()) {
                setupWorldHop();
                currentState = ObstacleState.WORLD_HOP_1;
            } else {
                currentState = ObstacleState.WALK_TO_LEVER;
            }
            return;
        }
        // Force banking if config.bankNow() is enabled
        if (config.bankNow() || forceBankNextLoot) {
            forceBankNextLoot = false;
            // Player Monitor not used - built-in anti-PK system is sufficient
            if (config.enableWorldHop()) {
                setupWorldHop();
                currentState = ObstacleState.WORLD_HOP_1;
            } else {
                currentState = ObstacleState.WALK_TO_LEVER;
            }
            return;
        }
        // Only check banking threshold here - use looting bag value
        if (lootingBagValue >= config.leaveAtValue()) {
            // Player Monitor not used - built-in anti-PK system is sufficient
            if (config.enableWorldHop()) {
                setupWorldHop();
                currentState = ObstacleState.WORLD_HOP_1;
            } else {
                currentState = ObstacleState.WALK_TO_LEVER;
            }
            return;
        }
        currentState = ObstacleState.PIPE;
        dispenserLootAttempts = 0;
        // Immediately call handlePipe() if player is ready
        if (!Rs2Player.isAnimating() && !Rs2Player.isMoving()) {
            WorldPoint pipeFrontTile = new WorldPoint(3004, 3937, 0);
            WorldPoint loc = Rs2Player.getWorldLocation();
            if (loc != null && loc.distanceTo(pipeFrontTile) <= 4) {
                handlePipe();
            }
        }
    }
    /**
     * STARTUP INVENTORY CHECK
     * Checks if player has required items on startup (knife, looting bag, 150k coins, phoenix necklace)
     * If missing any, goes to bank to withdraw them
     */
    private void handleStartupInventoryCheck() {
        Microbot.log("[WildernessNicky] 📋 Checking startup requirements...");

        // ===== REQUIREMENT CHECK 1: AGILITY LEVEL =====
        int currentAgility = Rs2Player.getRealSkillLevel(AGILITY);
        boolean hasAgilityLevel = currentAgility >= 52; // Wilderness Agility requires 52 agility
        Microbot.log("[WildernessNicky] Agility Level: " + currentAgility + " / 52 " + (hasAgilityLevel ? "✅" : "❌"));

        if (!hasAgilityLevel) {
            Microbot.log("[WildernessNicky] ❌ INSUFFICIENT AGILITY LEVEL!");
            Microbot.log("[WildernessNicky] Wilderness Agility Course requires 52 Agility (current: " + currentAgility + ")");
            Microbot.log("[WildernessNicky] Plugin will stop.");
            Microbot.stopPlugin(plugin);
            return;
        }

        // ===== REQUIREMENT CHECK 2: LOCATION CHECK (only if startAtCourse is enabled) =====
        if (config.startAtCourse()) {
            WorldPoint playerLoc = Rs2Player.getWorldLocation();
            if (playerLoc != null) {
                // Wilderness Agility Course area (roughly 2990-3015 X, 3930-3970 Y)
                boolean atCourse = playerLoc.getX() >= 2990 && playerLoc.getX() <= 3015 &&
                                   playerLoc.getY() >= 3930 && playerLoc.getY() <= 3970 &&
                                   playerLoc.getPlane() == 0;

                Microbot.log("[WildernessNicky] Location Check: " + (atCourse ? "✅ At Wilderness Agility Course" : "❌ Not at course"));

                if (!atCourse) {
                    Microbot.log("[WildernessNicky] ❌ START AT COURSE ENABLED BUT YOU'RE NOT AT THE COURSE!");
                    Microbot.log("[WildernessNicky] Current location: " + playerLoc);
                    Microbot.log("[WildernessNicky] Either disable 'Start at Course' or move to the Wilderness Agility Course.");
                    Microbot.log("[WildernessNicky] Plugin will stop.");
                    Microbot.stopPlugin(plugin);
                    return;
                }
            }
        }

        // ===== REQUIREMENT CHECK 3: RING OF WEALTH CHARGES (if using for teleports) =====
        boolean hasRingOfWealth = false;
        int rowCharges = 0;

        // Check inventory first
        for (String rowName : new String[]{"Ring of wealth (5)", "Ring of wealth (4)", "Ring of wealth (3)", "Ring of wealth (2)", "Ring of wealth (1)"}) {
            if (Rs2Inventory.hasItem(rowName)) {
                hasRingOfWealth = true;
                rowCharges = Integer.parseInt(rowName.replaceAll("[^0-9]", ""));
                break;
            }
        }

        // Check equipment if not in inventory
        if (!hasRingOfWealth) {
            for (String rowName : new String[]{"Ring of wealth (5)", "Ring of wealth (4)", "Ring of wealth (3)", "Ring of wealth (2)", "Ring of wealth (1)"}) {
                if (Rs2Equipment.isWearing(rowName)) {
                    hasRingOfWealth = true;
                    rowCharges = Integer.parseInt(rowName.replaceAll("[^0-9]", ""));
                    break;
                }
            }
        }

        if (hasRingOfWealth) {
            Microbot.log("[WildernessNicky] Ring of Wealth: ✅ (" + rowCharges + " charges)");
            if (rowCharges < 1) {
                Microbot.log("[WildernessNicky] ⚠️ WARNING: Ring of Wealth has no charges! You may need this for teleporting.");
            }
        } else {
            Microbot.log("[WildernessNicky] Ring of Wealth: ⚠️ Not found (optional, but useful for Grand Exchange teleports)");
        }

        // ===== INVENTORY CHECKS =====
        boolean hasKnife = Rs2Inventory.hasItem("Knife");
        boolean hasLootingBag = Rs2Inventory.hasItem("Looting bag");
        int coinCount = Rs2Inventory.itemQuantity(COINS_ID);
        boolean hasEnoughCoins = coinCount >= 150000;
        boolean hasPhoenix = Rs2Inventory.hasItem("Phoenix necklace") || Rs2Equipment.isWearing("Phoenix necklace");

        Microbot.log("[WildernessNicky] Knife: " + (hasKnife ? "✅" : "❌"));
        Microbot.log("[WildernessNicky] Looting Bag: " + (hasLootingBag ? "✅" : "❌"));
        Microbot.log("[WildernessNicky] Coins: " + coinCount + " / 150000 " + (hasEnoughCoins ? "✅" : "❌"));
        Microbot.log("[WildernessNicky] Phoenix Necklace: " + (hasPhoenix ? "✅" : "❌"));

        // If all items present, skip to START
        if (hasKnife && hasLootingBag && hasEnoughCoins && hasPhoenix) {
            Microbot.log("[WildernessNicky] ✅ All required items present! Proceeding to course...");
            currentState = ObstacleState.START;
            return;
        }

        // Otherwise, go to bank
        Microbot.log("[WildernessNicky] ⚠️ Missing required items - going to bank...");
        currentState = ObstacleState.STARTUP_BANKING;
    }

    /**
     * STARTUP BANKING
     * Withdraws required items for wilderness agility
     */
    private void handleStartupBanking() {
        // Walk to nearest bank if not there
        if (!Rs2Bank.isOpen()) {
            if (!Rs2Bank.walkToBank()) {
                Microbot.log("[WildernessNicky] Walking to nearest bank...");
                sleep(1000);
                return;
            }

            if (!Rs2Bank.openBank()) {
                Microbot.log("[WildernessNicky] Opening bank...");
                sleep(600);
                return;
            }
        }

        // Bank is open, withdraw items
        Microbot.log("[WildernessNicky] 🏦 Bank open - withdrawing required items...");

        // Withdraw knife (if missing)
        if (config.withdrawKnife() && !Rs2Inventory.hasItem("Knife")) {
            if (Rs2Bank.hasItem("Knife")) {
                Microbot.log("[WildernessNicky] Withdrawing knife...");
                Rs2Bank.withdrawOne("Knife");
                sleepUntil(() -> Rs2Inventory.hasItem("Knife"), 2000);
            } else {
                Microbot.log("[WildernessNicky] ⚠️ No knife found in bank!");
            }
        }

        // Withdraw looting bag (if missing)
        if (config.withdrawLootingBag() && !Rs2Inventory.hasItem("Looting bag")) {
            // Check for both unnoted and noted looting bags
            boolean hasUnnotedBag = Rs2Bank.hasItem(LOOTING_BAG_CLOSED_ID) || Rs2Bank.hasItem(LOOTING_BAG_OPEN_ID);
            boolean hasNotedBag = Rs2Bank.hasItem("Looting bag (noted)");

            if (hasUnnotedBag) {
                Microbot.log("[WildernessNicky] Withdrawing looting bag...");
                Rs2Bank.withdrawOne("Looting bag");
                sleepUntil(() -> Rs2Inventory.hasItem("Looting bag"), 2000);
            } else if (hasNotedBag) {
                // Has noted bag - withdraw it and set flag to unnote later
                Microbot.log("[WildernessNicky] Found noted looting bag - will unnote after withdrawing RoW");
                Rs2Bank.withdrawOne("Looting bag (noted)");
                sleepUntil(() -> Rs2Inventory.hasItem("Looting bag (noted)"), 2000);
                needsToBuyNotedLootingBag = false; // We have one, just need to unnote
                // Don't transition yet - we'll handle unnoting after all withdrawals
            } else {
                // No looting bag at all - need to buy noted version from GE
                Microbot.log("[WildernessNicky] ⚠️ No looting bag found in bank - will buy noted version from GE!");
                needsToBuyNotedLootingBag = true;
                // Will withdraw RoW below for unnoting after GE purchase
            }
        }

        // Withdraw 150k coins (if missing)
        if (config.withdrawCoins()) {
            int currentCoins = Rs2Inventory.itemQuantity(COINS_ID);
            if (currentCoins < 150000) {
                int coinsNeeded = 150000 - currentCoins;
                if (Rs2Bank.hasItem("Coins")) {
                    Microbot.log("[WildernessNicky] Withdrawing " + coinsNeeded + " coins...");
                    Rs2Bank.withdrawX("Coins", coinsNeeded);
                    sleepUntil(() -> Rs2Inventory.itemQuantity(COINS_ID) >= 150000, 2000);
                } else {
                    Microbot.log("[WildernessNicky] ⚠️ Not enough coins in bank!");
                }
            }
        }

        // Withdraw phoenix necklace (if missing and phoenixEscape enabled)
        if (config.phoenixEscape() && !Rs2Inventory.hasItem("Phoenix necklace") && !Rs2Equipment.isWearing("Phoenix necklace")) {
            if (Rs2Bank.hasItem("Phoenix necklace")) {
                Microbot.log("[WildernessNicky] Withdrawing phoenix necklace...");
                Rs2Bank.withdrawOne("Phoenix necklace");
                sleepUntil(() -> Rs2Inventory.hasItem("Phoenix necklace"), 2000);
            } else {
                Microbot.log("[WildernessNicky] ⚠️ No phoenix necklace found in bank!");
            }
        }

        // Withdraw ice plateau teleport (if enabled)
        if (config.useIcePlateauTp() && !Rs2Inventory.hasItem("Ice plateau teleport")) {
            if (Rs2Bank.hasItem("Ice plateau teleport")) {
                Microbot.log("[WildernessNicky] Withdrawing ice plateau teleport...");
                Rs2Bank.withdrawOne("Ice plateau teleport");
                sleepUntil(() -> Rs2Inventory.hasItem("Ice plateau teleport"), 2000);
            } else {
                Microbot.log("[WildernessNicky] ⚠️ No ice plateau teleport found in bank!");
            }
        }

        // Withdraw venom protection (if configured)
        if (config.withdrawVenomProtection() != WildernessNickyConfig.VenomProtectionOption.None) {
            int venomItemId = config.withdrawVenomProtection().getItemId();
            String venomItemName = config.withdrawVenomProtection().toString();

            if (!Rs2Inventory.hasItem(venomItemId)) {
                if (Rs2Bank.hasItem(venomItemId)) {
                    Microbot.log("[WildernessNicky] Withdrawing " + venomItemName + "...");
                    Rs2Bank.withdrawOne(venomItemId);
                    sleepUntil(() -> Rs2Inventory.hasItem(venomItemId), 2000);
                } else {
                    Microbot.log("[WildernessNicky] ⚠️ No " + venomItemName + " found in bank!");
                }
            }
        }

        // Withdraw Ring of Wealth if we need to buy or unnote looting bag
        boolean hasNotedBagInInventory = Rs2Inventory.hasItem("Looting bag (noted)");
        if ((needsToBuyNotedLootingBag || hasNotedBagInInventory) && !hasWithdrawnRingOfWealth) {
            // Look for any Ring of Wealth with charges (1-5)
            String[] rowVariants = {"Ring of wealth (5)", "Ring of wealth (4)", "Ring of wealth (3)", "Ring of wealth (2)", "Ring of wealth (1)"};
            boolean foundRoW = false;

            for (String rowName : rowVariants) {
                if (Rs2Bank.hasItem(rowName)) {
                    Microbot.log("[WildernessNicky] Withdrawing " + rowName + " for unnoting...");
                    Rs2Bank.withdrawOne(rowName);
                    sleepUntil(() -> Rs2Inventory.hasItem(rowName), 2000);
                    hasWithdrawnRingOfWealth = true;
                    foundRoW = true;
                    break;
                }
            }

            if (!foundRoW) {
                Microbot.log("[WildernessNicky] ⚠️ No Ring of Wealth found in bank! Cannot unnote looting bag.");
                Microbot.log("[WildernessNicky] Please add a Ring of Wealth to your bank and restart.");
                // Close bank and stop - can't proceed without RoW for unnoting
                Rs2Bank.closeBank();
                return;
            }
        }

        // Determine next state based on looting bag situation
        if (needsToBuyNotedLootingBag) {
            // Need to buy noted looting bag from GE first
            Microbot.log("[WildernessNicky] 🛒 Going to GE to buy noted looting bag...");
            Rs2Bank.closeBank();
            sleep(600);
            currentState = ObstacleState.GE_BUY_NOTED_LOOTING_BAG;
            return;
        } else if (hasNotedBagInInventory) {
            // Have noted bag in inventory - need to unnote it
            Microbot.log("[WildernessNicky] 📝 Unnoting looting bag...");
            // Don't close bank yet - unnote handler will handle it
            currentState = ObstacleState.UNNOTE_LOOTING_BAG;
            return;
        }

        // Close bank
        Rs2Bank.closeBank();
        sleep(600);

        // Now traverse to wilderness agility course
        Microbot.log("[WildernessNicky] ✅ Items withdrawn! Traveling to wilderness agility course...");

        // Use ice plateau teleport if we have it
        if (Rs2Inventory.hasItem("Ice plateau teleport")) {
            Microbot.log("[WildernessNicky] Using ice plateau teleport...");
            Rs2Inventory.interact("Ice plateau teleport", "Break");
            sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(new WorldPoint(2974, 3938, 0)) <= 20, 5000);
        } else {
            // Walk to course (fallback)
            Microbot.log("[WildernessNicky] Walking to course...");
            Rs2Walker.walkTo(DISPENSER_POINT, 10);
            sleepUntil(() -> Rs2Player.getWorldLocation().distanceTo(DISPENSER_POINT) <= 10, 10000);
        }

        // Transition to START state
        Microbot.log("[WildernessNicky] 🎯 Arrived at course - starting script!");
        currentState = ObstacleState.START;
    }

    private void handleStart() {
        // Check looting bag on startup if present
        // DISABLED: This corrupts inventory action data and causes Rs2Inventory.use() to crash
        // checkLootingBagOnStartup();
        
        TileObject dispenserObj = getDispenserObj();
        WorldPoint playerLoc = Rs2Player.getWorldLocation();
        boolean nearDispenser = dispenserObj != null && playerLoc != null && playerLoc.distanceTo(dispenserObj.getWorldLocation()) <= 4;

        if (!(forceStartAtCourse || config.startAtCourse())) {
            if (!nearDispenser) {
                WorldPoint walkTarget = dispenserObj != null ? dispenserObj.getWorldLocation() : DISPENSER_POINT;
                if (!isAt(walkTarget, 4)) {
                    Rs2Walker.walkTo(walkTarget, 2);
                    sleep(1000);
                    return;
                }
            }
            int coinCount = Rs2Inventory.itemQuantity(COINS_ID);
            if (coinCount < 150000) {
                Microbot.log("[WildernessNicky] Not enough coins to deposit into dispenser (" + coinCount + " < 150000) - going to bank");
                // Player Monitor not used - built-in anti-PK system is sufficient
                currentState = ObstacleState.BANKING;
                return;
            }
            if (dispenserObj != null) {
                Microbot.log("[WildernessNicky] Attempting to deposit " + coinCount + " coins into dispenser");
                Rs2Inventory.use(COINS_ID);
                sleep(400);
                Rs2GameObject.interact(dispenserObj, "Use");
                sleep(getActionDelay());
                sleepUntil(() -> Rs2Inventory.itemQuantity(COINS_ID) < coinCount, getXpTimeout());
            } else {
                Microbot.log("[WildernessNicky] Dispenser object not found!");
            }
            currentState = ObstacleState.PIPE;
            return;
        } else {
            if (!nearDispenser) {
                WorldPoint walkTarget = dispenserObj != null ? dispenserObj.getWorldLocation() : DISPENSER_POINT;
                if (!isAt(walkTarget, 4)) {
                    Rs2Walker.walkTo(walkTarget, 2);
                    return;
                }
            }
            sleep(300, 600);
            currentState = ObstacleState.PIPE;
        }
    }

    /**
     * Tracks player location and detects if stuck for 8 seconds
     */
    private void handleLocationTracking() {
        // Skip location tracking for states that don't need it (like banking)
        if (shouldSkipPositionTracking()) {
            return;
        }
        
        WorldPoint currentLocation = Rs2Player.getWorldLocation();
        long currentTime = System.currentTimeMillis();
        
        if (currentLocation == null) {
            return;
        }
        
        // Initialize tracking if needed
        if (lastPlayerLocation == null) {
            lastPlayerLocation = currentLocation;
            lastLocationChangeTime = currentTime;
            return;
        }
        
        // Check if location has changed
        if (!currentLocation.equals(lastPlayerLocation)) {
            lastPlayerLocation = currentLocation;
            lastLocationChangeTime = currentTime;
            lastStateBeforeStuck = null; // Reset stuck state
            return;
        }
        
        // Location hasn't changed - check if stuck for 8 seconds
        long timeSinceLastMove = currentTime - lastLocationChangeTime;
        if (timeSinceLastMove >= LOCATION_STUCK_TIMEOUT && lastStateBeforeStuck == null) {
            lastStateBeforeStuck = currentState;
            Microbot.log("[Location Tracking] Player stuck for " + (LOCATION_STUCK_TIMEOUT/1000) + " seconds in " + currentState + ", will retry previous state");
        }
        
        // If stuck for 8 seconds, retry the current state
        if (timeSinceLastMove >= LOCATION_STUCK_TIMEOUT && lastStateBeforeStuck != null) {
            Microbot.log("[Location Tracking] Retrying state " + currentState + " due to being stuck");
            retryCurrentState();
            lastLocationChangeTime = currentTime; // Reset timer
        }
    }

    /**
     * Handles position-based timeout and retry logic
     */
    private void handlePositionTimeoutLogic() {
        WorldPoint currentPosition = Rs2Player.getWorldLocation();
        long currentTime = System.currentTimeMillis();
        
        // Only check position every POSITION_CHECK_INTERVAL to avoid spam
        if (currentTime - lastPositionCheckTime < POSITION_CHECK_INTERVAL) {
            return;
        }
        lastPositionCheckTime = currentTime;
        
        // Skip position tracking for certain states that don't need it
        if (shouldSkipPositionTracking()) {
            resetPositionTracking();
            return;
        }
        
        // Initialize position tracking if needed
        if (lastTrackedPosition == null) {
            resetPositionTracking();
            lastTrackedPosition = currentPosition;
            positionLastChangedTime = currentTime;
            return;
        }
        
        // Check if position has changed
        if (currentPosition != null && !currentPosition.equals(lastTrackedPosition)) {
            lastTrackedPosition = currentPosition;
            positionLastChangedTime = currentTime;
            currentStateRetryAttempts = 0;
            isInRetryMode = false;
            return;
        }
        
        // Position hasn't changed - check for timeout
        long timeSinceLastMove = currentTime - positionLastChangedTime;
        if (timeSinceLastMove >= getPositionTimeout()) {
            handlePositionTimeout();
        }
    }
    
    /**
     * Handles what happens when position timeout is reached
     */
    private void handlePositionTimeout() {
        if (!isInRetryMode) {
            // First timeout - retry current state
            if (currentState == ObstacleState.PIT_RECOVERY) {
                Microbot.log("[Position Timeout] Player stuck in pit for " + getPositionTimeout() + "ms. Retrying ladder interaction...");
            } else {
                Microbot.log("[Position Timeout] Player stuck in " + currentState + " for " + getPositionTimeout() + "ms. Retrying...");
            }
            isInRetryMode = true;
            currentStateRetryAttempts++;
            retryCurrentState();
            resetPositionTracking();
        } else if (currentStateRetryAttempts >= MAX_RETRY_ATTEMPTS) {
            // Second timeout after retry - move to next state
            if (currentState == ObstacleState.PIT_RECOVERY) {
                Microbot.log("[Position Timeout] Still stuck in pit after retry. Forcing exit from pit recovery...");
            } else {
                Microbot.log("[Position Timeout] Retry failed for " + currentState + ". Moving to next state...");
            }
            forceProgressToNextState();
            resetPositionTracking();
        }
    }
    
    /**
     * Determines if position tracking should be skipped for the current state
     */
    private boolean shouldSkipPositionTracking() {
        return currentState == ObstacleState.BANKING || 
               currentState == ObstacleState.WORLD_HOP_1 || 
               currentState == ObstacleState.WORLD_HOP_2 ||
               currentState == ObstacleState.WALK_TO_LEVER ||
               currentState == ObstacleState.INTERACT_LEVER ||
               currentState == ObstacleState.WALK_TO_COURSE ||
               Rs2Player.isMoving() ||
               Rs2Player.isAnimating();
    }
    
    /**
     * Resets position tracking variables
     */
    private void resetPositionTracking() {
        lastTrackedPosition = Rs2Player.getWorldLocation();
        positionLastChangedTime = System.currentTimeMillis();
        currentStateRetryAttempts = 0;
        isInRetryMode = false;
    }
    
    /**
     * Retries the current obstacle state
     */
    private void retryCurrentState() {
        switch (currentState) {
            case PIPE:
                isWaitingForPipe = false;
                break;
            case ROPE:
                isWaitingForRope = false;
                break;
            case STONES:
                isWaitingForStones = false;
                break;
            case LOG:
                isWaitingForLog = false;
                break;
            case ROCKS:
                // No waiting state for rocks
                break;
            case DISPENSER:
                waitingForDispenserLoot = false;
                dispenserLootAttempts = 0;
                break;
            case PIT_RECOVERY:
                // Reset ladder interaction time to allow immediate retry
                lastLadderInteractTime = 0;
                ropeRecoveryWalked = false;
                break;
            default:
                break;
        }
    }
    
    /**
     * Forces progression to the next state in the obstacle sequence
     */
    private void forceProgressToNextState() {
        switch (currentState) {
            case PIPE:
                Microbot.log("[Force Progress] Moving from PIPE to ROPE");
                currentState = ObstacleState.ROPE;
                isWaitingForPipe = false;
                break;
            case ROPE:
                Microbot.log("[Force Progress] Moving from ROPE to STONES");
                currentState = ObstacleState.STONES;
                isWaitingForRope = false;
                break;
            case STONES:
                Microbot.log("[Force Progress] Moving from STONES to LOG");
                currentState = ObstacleState.LOG;
                isWaitingForStones = false;
                break;
            case LOG:
                Microbot.log("[Force Progress] Moving from LOG to ROCKS");
                currentState = ObstacleState.ROCKS;
                isWaitingForLog = false;
                break;
            case ROCKS:
                Microbot.log("[Force Progress] Moving from ROCKS to DISPENSER");
                currentState = ObstacleState.DISPENSER;
                break;
            case DISPENSER:
                Microbot.log("[Force Progress] Moving from DISPENSER to CONFIG_CHECKS");
                currentState = ObstacleState.CONFIG_CHECKS;
                waitingForDispenserLoot = false;
                dispenserLootAttempts = 0;
                break;
            case PIT_RECOVERY:
                Microbot.log("[Force Progress] Stuck in pit recovery, forcing exit and continuing...");
                // Force the player to move to the recovery target or default to ROPE
                if (pitRecoveryTarget != null) {
                    currentState = pitRecoveryTarget;
                    Microbot.log("[Force Progress] Returning to recovery target: " + pitRecoveryTarget);
                } else {
                    currentState = ObstacleState.ROPE;
                    Microbot.log("[Force Progress] No recovery target set, defaulting to ROPE");
                }
                // Reset pit recovery variables
                pitRecoveryTarget = null;
                ropeRecoveryWalked = false;
                lastLadderInteractTime = 0;
                break;
            case BANKING:
                Microbot.log("[Force Progress] Moving from BANKING to POST_BANK_CONFIG");
                currentState = ObstacleState.POST_BANK_CONFIG;
                break;
            case SWAP_BACK:
                Microbot.log("[Force Progress] Moving from SWAP_BACK to PIPE");
                currentState = ObstacleState.PIPE;
                break;
            case CONFIG_CHECKS:
                Microbot.log("[Force Progress] Moving from CONFIG_CHECKS to PIPE");
                currentState = ObstacleState.PIPE;
                break;
            case START:
                Microbot.log("[Force Progress] Moving from START to PIPE");
                currentState = ObstacleState.PIPE;
                break;
            case WORLD_HOP_2:
                Microbot.log("[Force Progress] Moving from WORLD_HOP_2 to WALK_TO_LEVER");
                currentState = ObstacleState.WALK_TO_LEVER;
                break;
            case INTERACT_LEVER:
                Microbot.log("[Force Progress] Moving from INTERACT_LEVER to BANKING (web walking handles lever)");
                currentState = ObstacleState.BANKING;
                break;
            case POST_BANK_CONFIG:
                Microbot.log("[Force Progress] Moving from POST_BANK_CONFIG to WALK_TO_COURSE");
                currentState = ObstacleState.WALK_TO_COURSE;
                break;
            case WALK_TO_COURSE:
                Microbot.log("[Force Progress] Moving from WALK_TO_COURSE to PIPE");
                currentState = ObstacleState.PIPE;
                break;
            case INIT:
                Microbot.log("[Force Progress] Moving from INIT to PIPE");
                currentState = ObstacleState.PIPE;
                break;
            case WALK_TO_LEVER:
                Microbot.log("[Force Progress] Moving from WALK_TO_LEVER to BANKING (web walking handles lever)");
                webWalkStartTime = 0; // Reset web walk timeout
                currentState = ObstacleState.BANKING;
                break;
            case WORLD_HOP_1:
                Microbot.log("[Force Progress] Moving from WORLD_HOP_1 to WORLD_HOP_2");
                currentState = ObstacleState.WORLD_HOP_2;
                break;
            default:
                Microbot.log("[Force Progress] No progression defined for state: " + currentState);
                break;
        }
    }

    private boolean waitForXpChange(int startXp, int timeoutMs) {
        return sleepUntil(() -> Microbot.getClient().getSkillExperience(AGILITY) > startXp, timeoutMs);
    }

    private boolean isAt(WorldPoint target, int dist) {
        WorldPoint loc = Rs2Player.getWorldLocation();
        return loc != null && target != null && loc.distanceTo(target) <= dist;
    }

    private int getActionDelay() { return ACTION_DELAY; }
    private int getXpTimeout() { return XP_TIMEOUT; }

    /**
     * Gets the position timeout value from config (same as animation fail timeout)
     * @return timeout in milliseconds from config
     */
    private int getPositionTimeout() {
        return config.failTimeoutMs();
    }

    /**
     * PROACTIVE PLAYER DETECTION - MASS-FRIENDLY
     * Scans for nearby players who could be threats (PKers)
     * ONLY flags players who are NOT in your FC (for mass agility runs)
     * Returns true if a REAL threat is detected
     */
    private boolean detectNearbyThreat() {
        // Rate limit scanning
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPlayerScanTime < PLAYER_SCAN_INTERVAL) {
            return false; // Don't scan too frequently
        }
        lastPlayerScanTime = currentTime;

        try {
            // Get local player info
            net.runelite.api.Player localPlayer = Microbot.getClient().getLocalPlayer();
            if (localPlayer == null) return false;

            WorldPoint playerLocation = localPlayer.getWorldLocation();
            int localCombatLevel = localPlayer.getCombatLevel();

            // Get FC members list (if in FC)
            net.runelite.api.clan.ClanChannel clanChannel = Microbot.getClient().getClanChannel();
            java.util.Set<String> fcMembers = new java.util.HashSet<>();
            if (clanChannel != null && config.joinFc()) {
                clanChannel.getMembers().forEach(member -> {
                    if (member != null && member.getName() != null) {
                        fcMembers.add(member.getName().toLowerCase().trim());
                    }
                });
            }

            // Scan for nearby UNSAFE players (NOT in FC) using ADVANCED Rs2Pvp APIs
            long unsafePlayers = Rs2Player.getPlayers(p -> {
                // Skip self
                if (p.getPlayer() == localPlayer) return false;

                // Check distance
                WorldPoint pLoc = p.getWorldLocation();
                if (pLoc == null || pLoc.distanceTo(playerLocation) > THREAT_SCAN_RADIUS) {
                    return false;
                }

                // MASS-FRIENDLY: Skip FC members (they're safe)
                String playerName = p.getName();
                if (playerName != null) {
                    String normalizedName = playerName.toLowerCase().trim();
                    if (fcMembers.contains(normalizedName)) {
                        return false; // FC member = safe, ignore
                    }
                }

                // Use Rs2Pvp.isAttackable() for accurate wilderness combat range checking
                boolean canAttack = Rs2Pvp.isAttackable(p);

                if (canAttack) {
                    // Check equipment for threat level
                    lastThreatHadHighTierGear = hasHighTierGear(p);
                    String gearWarning = lastThreatHadHighTierGear ? " [HIGH-TIER GEAR]" : "";

                    Microbot.log("[WildernessNicky] ⚠️ ATTACKABLE PLAYER DETECTED: '" + playerName +
                               "' (Lvl " + p.getCombatLevel() + ") NOT in FC at distance " +
                               pLoc.distanceTo(playerLocation) + " tiles" + gearWarning + " - POTENTIAL PKER!");
                }

                return canAttack;
            }).count();

            // If any UNSAFE threatening players detected, return true
            return unsafePlayers > 0;

        } catch (Exception e) {
            Microbot.log("[WildernessNicky] Error in player threat detection: " + e.getMessage());
            return false;
        }
    }

    /**
     * Quick check if any player threat is nearby (without full scan delay)
     * Used for smarter escape decisions
     */
    private boolean isPlayerThreatNearby() {
        try {
            net.runelite.api.Player localPlayer = Microbot.getClient().getLocalPlayer();
            if (localPlayer == null) return false;

            WorldPoint playerLocation = localPlayer.getWorldLocation();
            int localCombatLevel = localPlayer.getCombatLevel();

            // Get FC members (for mass safety)
            net.runelite.api.clan.ClanChannel clanChannel = Microbot.getClient().getClanChannel();
            java.util.Set<String> fcMembers = new java.util.HashSet<>();
            if (clanChannel != null && config.joinFc()) {
                clanChannel.getMembers().forEach(member -> {
                    if (member != null && member.getName() != null) {
                        fcMembers.add(member.getName().toLowerCase().trim());
                    }
                });
            }

            // Quick check for nearby non-FC players in attack range
            return Microbot.getClient().getTopLevelWorldView().players().stream()
                .filter(p -> p != null && p != localPlayer)
                .filter(p -> {
                    WorldPoint pLoc = p.getWorldLocation();
                    return pLoc != null && pLoc.distanceTo(playerLocation) <= THREAT_SCAN_RADIUS;
                })
                .anyMatch(p -> {
                    // Skip FC members
                    String playerName = p.getName();
                    if (playerName != null && fcMembers.contains(playerName.toLowerCase().trim())) {
                        return false;
                    }

                    // Check if in attack range
                    int theirCombatLevel = p.getCombatLevel();
                    int wildernessLevel = getWildernessLevel(playerLocation);
                    int levelDiff = Math.abs(theirCombatLevel - localCombatLevel);
                    return levelDiff <= wildernessLevel;
                });

        } catch (Exception e) {
            return false; // If error, assume no threat
        }
    }

    /**
     * SOLO MODE - Detect ANY attackable player nearby (skulled or not)
     * Triggers instant logout in solo mode
     */
    private boolean detectAttackablePlayer() {
        try {
            net.runelite.api.Player localPlayer = Microbot.getClient().getLocalPlayer();
            if (localPlayer == null) return false;

            WorldPoint playerLocation = localPlayer.getWorldLocation();
            int localCombatLevel = localPlayer.getCombatLevel();
            int wildernessLevel = getWildernessLevel(playerLocation);

            // Get FC members (for mass safety)
            net.runelite.api.clan.ClanChannel clanChannel = Microbot.getClient().getClanChannel();
            java.util.Set<String> fcMembers = new java.util.HashSet<>();
            if (clanChannel != null && config.joinFc()) {
                clanChannel.getMembers().forEach(member -> {
                    if (member != null && member.getName() != null) {
                        fcMembers.add(member.getName().toLowerCase().trim());
                    }
                });
            }

            // Scan for ANY attackable player within range (excluding FC members)
            return Microbot.getClient().getTopLevelWorldView().players().stream()
                .filter(p -> p != null && p != localPlayer)
                .filter(p -> {
                    // Skip FC/clan members
                    String playerName = p.getName();
                    if (playerName != null && fcMembers.contains(playerName.toLowerCase().trim())) {
                        return false;
                    }
                    return true;
                })
                .filter(p -> {
                    WorldPoint pLoc = p.getWorldLocation();
                    return pLoc != null && pLoc.distanceTo(playerLocation) <= THREAT_SCAN_RADIUS;
                })
                .anyMatch(p -> {
                    // Check if in attack range
                    int theirCombatLevel = p.getCombatLevel();
                    int levelDiff = Math.abs(theirCombatLevel - localCombatLevel);
                    boolean canAttack = levelDiff <= wildernessLevel;

                    if (canAttack) {
                        String skullStatus = p.getSkullIcon() != -1 ? "SKULLED" : "non-skulled";
                        Microbot.log("[WildernessNicky] ⚠️ Attackable player detected: " + p.getName() + " (Level " + theirCombatLevel + ", " + skullStatus + ")");
                    }
                    return canAttack;
                });
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * LEGACY - Detect skulled attackable players nearby
     * Kept for backwards compatibility but no longer used
     */
    private boolean detectSkulledThreat() {
        try {
            net.runelite.api.Player localPlayer = Microbot.getClient().getLocalPlayer();
            if (localPlayer == null) return false;

            WorldPoint playerLocation = localPlayer.getWorldLocation();
            int localCombatLevel = localPlayer.getCombatLevel();
            int wildernessLevel = getWildernessLevel(playerLocation);

            // Scan for SKULLED players within attack range
            return Microbot.getClient().getTopLevelWorldView().players().stream()
                .filter(p -> p != null && p != localPlayer)
                .filter(p -> p.getSkullIcon() != -1) // ONLY skulled players (skull icon exists)
                .filter(p -> {
                    WorldPoint pLoc = p.getWorldLocation();
                    return pLoc != null && pLoc.distanceTo(playerLocation) <= THREAT_SCAN_RADIUS;
                })
                .anyMatch(p -> {
                    // Check if in attack range
                    int theirCombatLevel = p.getCombatLevel();
                    int levelDiff = Math.abs(theirCombatLevel - localCombatLevel);
                    boolean canAttack = levelDiff <= wildernessLevel;

                    if (canAttack) {
                        Microbot.log("[WildernessNicky] 💀 Skulled player detected: " + p.getName() + " (Level " + theirCombatLevel + ")");
                    }
                    return canAttack;
                });
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Smart food eating - eats best available food
     */
    private void eatFood() {
        // Try foods in priority order
        if (Rs2Inventory.contains("Anglerfish")) {
            Rs2Inventory.interact("Anglerfish", "Eat");
        } else if (Rs2Inventory.contains("Manta ray")) {
            Rs2Inventory.interact("Manta ray", "Eat");
        } else if (Rs2Inventory.contains("Karambwan")) {
            Rs2Inventory.interact("Karambwan", "Eat");
        }
    }

    /**
     * Checks if player is in a safe zone (banks, non-wilderness areas, etc.)
     * Safe zones don't trigger solo mode logout
     */
    private boolean isInSafeZone() {
        try {
            WorldPoint playerLoc = Rs2Player.getWorldLocation();
            if (playerLoc == null) return false;

            // Check if NOT in wilderness (safest check)
            if (!Rs2Pvp.isInWilderness()) {
                return true; // Not in wilderness = safe zone
            }

            // Mage Bank safe zone (underground)
            // Mage Bank coordinates: 2534, 4712, 0 (plane 0 = underground instance)
            WorldArea mageBankArea = new WorldArea(2530, 4708, 10, 10, 0); // 10x10 area around bank
            if (mageBankArea.contains(playerLoc)) {
                return true; // Inside Mage Bank
            }

            // Banking state check (if we're banking, assume safe)
            if (currentState == ObstacleState.BANKING) {
                return true;
            }

            return false; // Not in safe zone
        } catch (Exception e) {
            Microbot.log("[WildernessNicky] Error checking safe zone: " + e.getMessage());
            return false; // Assume unsafe on error (better safe than sorry)
        }
    }

    /**
     * Finds the nearest safe spot (away from NPCs/players) for logging out
     * Returns null if no safe spot found within range
     */
    private WorldPoint findNearestSafeSpot() {
        try {
            WorldPoint currentLoc = Rs2Player.getWorldLocation();
            if (currentLoc == null) return null;

            // Search in expanding radius (3, 5, 7 tiles) for a safe walkable tile
            for (int radius = 3; radius <= 7; radius++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        WorldPoint candidate = new WorldPoint(
                            currentLoc.getX() + dx,
                            currentLoc.getY() + dy,
                            currentLoc.getPlane()
                        );

                        // Check if tile is walkable and safe from NPCs
                        if (isSafeTile(candidate)) {
                            return candidate;
                        }
                    }
                }
            }
            return null; // No safe spot found
        } catch (Exception e) {
            Microbot.log("[WildernessNicky] Error finding safe spot: " + e.getMessage());
            return null;
        }
    }

    /**
     * Checks if a tile is safe (walkable and no NPCs/players within 2 tiles)
     */
    private boolean isSafeTile(WorldPoint tile) {
        try {
            // Check if tile is walkable
            if (!Rs2Tile.isWalkable(tile)) {
                return false;
            }

            // Check for nearby NPCs (skeletons, etc.)
            long nearbyNpcs = Microbot.getClient().getTopLevelWorldView().npcs().stream()
                .filter(npc -> npc != null && npc.getWorldLocation() != null)
                .filter(npc -> npc.getWorldLocation().distanceTo(tile) <= 2)
                .count();

            if (nearbyNpcs > 0) {
                return false; // Not safe, NPCs too close
            }

            // Check for nearby players (PKers)
            long nearbyPlayers = Microbot.getClient().getTopLevelWorldView().players().stream()
                .filter(p -> p != null && p != Microbot.getClient().getLocalPlayer())
                .filter(p -> p.getWorldLocation() != null)
                .filter(p -> p.getWorldLocation().distanceTo(tile) <= 5) // Farther distance for players
                .count();

            if (nearbyPlayers > 0) {
                return false; // Not safe, players too close
            }

            return true; // Safe tile found
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Enables the best protection prayer based on current attacker
     * Used during emergency escape to maximize survival
     */
    private void enableBestProtectionPrayer() {
        try {
            if (!Rs2Player.hasPrayerPoints()) {
                return; // No prayer points
            }

            Actor interacting = Microbot.getClient().getLocalPlayer().getInteracting();

            // If being attacked by a player, use projectile-based prayer (handled by main loop)
            if (interacting instanceof net.runelite.api.Player) {
                // Projectile prayer switching is more accurate for players
                // Handled by main loop's handleProjectilePrayerSwitching()
                return;
            }

            // If being attacked by NPC (skeleton), default to protect melee
            if (interacting instanceof net.runelite.api.NPC) {
                if (activeCombatPrayer != Rs2PrayerEnum.PROTECT_MELEE) {
                    switchProtectionPrayer(Rs2PrayerEnum.PROTECT_MELEE);
                }
            }

            // If no attacker but in wilderness, enable Protect Item (via quick prayer)
            if (interacting == null && Rs2Pvp.isInWilderness()) {
                if (!Rs2Prayer.isQuickPrayerEnabled()) {
                    Rs2Prayer.toggleQuickPrayer(true);
                }
            }
        } catch (Exception e) {
            Microbot.log("[WildernessNicky] Error enabling protection prayer: " + e.getMessage());
        }
    }

    /**
     * ===== BANKED LOOT TRACKING =====
     * Tracks all items being banked from inventory
     * Updates bankedLoot HashMap with item names and quantities
     */
    private void trackBankedLoot() {
        try {
            wildyItems.setupWildernessItemsIfEmpty();

            // Track items from inventory (before deposit) - using stream
            Rs2Inventory.items().forEach(itemModel -> {
                if (itemModel == null) return;

                int itemId = itemModel.getId();
                String itemName = Microbot.getItemManager().getItemComposition(itemId).getName();

                // Skip non-loot items (coins, knife, teleports, looting bag, etc.)
                if (itemName.contains("Coins") || itemName.contains("knife") ||
                    itemName.contains("teleport") || itemName.contains("Looting bag") ||
                    itemName.contains("necklace") || itemName.contains("antidote") ||
                    itemName.contains("venom") || itemName.contains("sack")) {
                    return;
                }

                // Check if this is a wilderness agility loot item
                if (WildernessNickyItems.ITEM_IDS.contains(itemId)) {
                    int quantity = itemModel.getQuantity();

                    // Add to banked loot HashMap
                    bankedLoot.merge(itemName, quantity, Integer::sum);

                    // Calculate value
                    int itemValue = Microbot.getItemManager().getItemPrice(itemId) * quantity;
                    totalBankedValue += itemValue;

                    Microbot.log("[WildernessNicky] Tracked: " + quantity + "x " + itemName + " (+" + NumberFormat.getIntegerInstance().format(itemValue) + "gp)");
                }
            });

            Microbot.log("[WildernessNicky] Loot tracking complete. Total tracked items: " + bankedLoot.size() + " types");
        } catch (Exception e) {
            Microbot.log("[WildernessNicky] Error tracking banked loot: " + e.getMessage());
        }
    }

    /**
     * Gets a formatted string of top 5 banked items for overlay display
     * @return Formatted string with top items
     */
    public Map<String, Integer> getTopBankedLoot(int limit) {
        return bankedLoot.entrySet()
            .stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(limit)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    public int getTotalBankedValue() {
        return totalBankedValue;
    }

    public int getTotalBankingTrips() {
        return totalBankingTrips;
    }

    /**
     * ===== ENHANCED PVP DAMAGE DETECTION =====
     * Detects when player takes PvP damage (not agility fail damage)
     * This allows us to react ONLY when attacked by another player
     * Perfect for mass agility runs where you want to ignore nearby players until attacked
     */
    private void detectPvPDamage() {
        long currentTime = System.currentTimeMillis();

        // Rate limit health checks
        if (currentTime - lastHealthCheckTime < HEALTH_CHECK_INTERVAL) {
            return;
        }
        lastHealthCheckTime = currentTime;

        // Get current health (cast double to int)
        int currentHealth = (int) Rs2Player.getHealthPercentage();

        // Initialize previousHealth on first check
        if (previousHealth == 100 && currentHealth < 100) {
            previousHealth = currentHealth;
            return;
        }

        // Detect health drop
        if (currentHealth < previousHealth) {
            int healthDrop = previousHealth - currentHealth;

            // Check if we're in combat with another player
            Actor interactingWith = Microbot.getClient().getLocalPlayer().getInteracting();
            boolean inPlayerCombat = (interactingWith instanceof Player);

            // Heuristic: Large health drops (>10%) OR being in player combat = PvP damage
            // Agility fails typically do smaller, predictable damage
            if (healthDrop >= 10 || inPlayerCombat) {
                pvpHitCount++;
                recentlyTookPvPDamage = true;
                lastPvPDamageTime = currentTime;

                // NEW: Track if attacker is clan member or not
                if (inPlayerCombat && interactingWith != null) {
                    Player attacker = (Player) interactingWith;
                    String attackerName = attacker.getName();
                    lastAttackerName = attackerName != null ? attackerName : "";

                    // Check if attacker is in our FC/clan
                    boolean isAttackerClanMember = isPlayerInClan(attackerName);

                    if (isAttackerClanMember) {
                        clanMemberHitCount++;
                        Microbot.log("[WildernessNicky] ⚔️ Clan Member Hit #" + clanMemberHitCount + " from " + attackerName + " (Health -" + healthDrop + "%)");
                    } else {
                        nonClanHitCount++;
                        Microbot.log("[WildernessNicky] ⚔️ NON-CLAN Hit #" + nonClanHitCount + " from " + attackerName + " (Health -" + healthDrop + "%)");
                    }
                } else {
                    // Unknown attacker, assume non-clan for safety
                    nonClanHitCount++;
                    Microbot.log("[WildernessNicky] ⚔️ PvP Hit #" + pvpHitCount + " Detected! Health dropped " + healthDrop + "%");
                }
            }
        }

        // Clear PvP damage flag and reset hit counters after timeout
        if (recentlyTookPvPDamage && currentTime - lastPvPDamageTime > PVP_DAMAGE_TIMEOUT) {
            recentlyTookPvPDamage = false;
            pvpHitCount = 0;
            clanMemberHitCount = 0;
            nonClanHitCount = 0;
            lastAttackerName = "";
            Microbot.log("[WildernessNicky] PvP damage flag cleared (timeout) - all hit counters reset");
        }

        // Update previous health
        previousHealth = currentHealth;
    }

    /**
     * Calculate wilderness level based on Y coordinate
     * Wilderness level determines combat level range for PvP
     */
    private int getWildernessLevel(WorldPoint location) {
        if (location == null) return 0;

        // Wilderness starts at Y=3520 (level 1)
        // Each tile north increases wilderness level by 1
        int y = location.getY();
        if (y < 3520) return 0; // Not in wilderness

        return Math.min((y - 3520) / 1, 56); // Max wilderness level is 56
    }

    /**
     * Check if a player name is in our clan/FC
     */
    private boolean isPlayerInClan(String playerName) {
        if (playerName == null || playerName.isEmpty()) return false;
        if (!config.joinFc()) return false; // If not using FC, treat all as non-clan

        try {
            net.runelite.api.clan.ClanChannel clanChannel = Microbot.getClient().getClanChannel();
            if (clanChannel == null) return false;

            String normalizedName = playerName.toLowerCase().trim();
            return clanChannel.getMembers().stream()
                .anyMatch(member -> member != null &&
                         member.getName() != null &&
                         member.getName().toLowerCase().trim().equals(normalizedName));
        } catch (Exception e) {
            return false; // If error, assume non-clan for safety
        }
    }

    /**
     * Triggers Phoenix Escape - enables player monitor and uses robust escape logic
     * @param reason The reason for triggering the escape (for logging and debugging)
     */
    private void triggerPhoenixEscape(String reason) {
        if (emergencyEscapeTriggered) {
            return; // Already triggered
        }

        emergencyEscapeTriggered = true;
        emergencyEscapeStartTime = System.currentTimeMillis();

        // Track escape reason and time
        lastEscapeReason = reason;
        lastEscapeTime = System.currentTimeMillis();

        // Reset escape step tracking
        hasEquippedPhoenixNecklace = false;
        hasClimbedRocks = false;
        hasOpenedGate = false;
        escapeStep2StartTime = 0;

        // Reset escape step attempt counters
        escapeEquipNecklaceAttempts = 0;
        escapeClimbRocksAttempts = 0;
        escapeOpenGateAttempts = 0;
        escapeWalkToMageBankAttempts = 0;

        // Reset PvP hit counter when escaping
        pvpHitCount = 0;
        recentlyTookPvPDamage = false;

        Microbot.log("[WildernessNicky] ⚠️ EMERGENCY ESCAPE TRIGGERED ⚠️");
        Microbot.log("[WildernessNicky] 📋 ESCAPE REASON: " + reason);

        // NOTE: Player Monitor is NOT enabled during emergency escape as it interferes with logout attempts
        // The built-in prayer switching and logout system is sufficient

        // Set state to Emergency Escape (dedicated escape state)
        currentState = ObstacleState.EMERGENCY_ESCAPE;
    }

    /**
     * Handles emergency escape from PKers with advanced anti-PK features.
     *
     * <p><b>Key Features:</b></p>
     * <ul>
     *   <li><b>1-Tick Prayer Switching:</b> Automatically switches to correct protection prayer
     *       (Melee/Range/Magic) based on incoming projectiles</li>
     *   <li><b>Spam Logout:</b> Attempts logout every 100ms throughout entire escape</li>
     *   <li><b>Continuous Movement:</b> Never stands still - always moving toward Mage Bank</li>
     *   <li><b>Auto Food:</b> Eats food when HP drops below 60%</li>
     *   <li><b>Smart Pathing:</b> Equip necklace → Climb rocks → Open gate → Run to Mage Bank</li>
     * </ul>
     *
     * <p><b>Escape Flow:</b></p>
     * <pre>
     * Every 100ms loop:
     *   1. Switch prayer (1-tick before projectile impact)
     *   2. Eat food (if HP &lt; 60%)
     *   3. Spam logout attempt
     *   4. Check position & continue movement:
     *      - Equip Phoenix necklace (while moving)
     *      - Climb rocks (if in rock area)
     *      - Open gate (if near gate)
     *      - Walk to Mage Bank (continuous)
     *   5. Repeat (no blocking waits)
     * </pre>
     *
     * <p><b>Safety Features:</b></p>
     * <ul>
     *   <li>World hop after successful logout (Solo mode, up to 3 attempts)</li>
     *   <li>Failsafe timeouts for each step (prevents getting stuck)</li>
     *   <li>Force logout at Mage Bank safe zone</li>
     *   <li>Emergency timeout after 3 minutes</li>
     * </ul>
     *
     * @see #handleProjectilePrayerSwitching() for 1-tick prayer system
     * @see #eatFood() for food consumption logic
     * @see #resetEscapeMode() for cleanup after escape
     */
    private void handleEmergencyEscape() {
        if (!emergencyEscapeTriggered) {
            return;
        }

        long timeSinceStart = System.currentTimeMillis() - emergencyEscapeStartTime;

        // ===== STEP 1: IMMEDIATE PRAYER SWITCHING (1-tick accuracy) =====
        // Activates BEFORE logout check to ensure protection even during combat
        if (config.useProjectilePrayerSwitching() && Rs2Player.hasPrayerPoints()) {
            handleProjectilePrayerSwitching(); // Switches to correct prayer 1 tick before projectile hits
        }

        // ===== STEP 2: AUTO FOOD CONSUMPTION =====
        // Maintains health above 60% throughout entire escape
        if (Rs2Player.getHealthPercentage() < 60) {
            eatFood();
        }

        // ===== STEP 3: SMART SKELETON COMBAT ESCAPE + SPAM LOGOUT =====
        // NEW: Multi-zone escape strategy based on player location
        boolean inSkeletonCombat = isInSkeletonCombat();

        if (inSkeletonCombat) {
            WorldPoint currentLoc = Rs2Player.getWorldLocation();

            // PRIORITY 1: Check if already at a safe logout location
            // If at safe spot, just spam logout (combat will break when skeletons de-aggro)
            if (isSafeLogoutLocation()) {
                Microbot.log("[WildernessNicky] ✅ Already at safe logout spot - spamming logout to break skeleton combat");
                // Logout spam happens at line 3743, so just return and let it continue
                return;
            }

            // PRIORITY 2: If close to a safe logout spot (within 10 tiles), go there instead
            WorldPoint closestSafeSpot = getClosestSafeLogoutLocation();
            if (closestSafeSpot != null && currentLoc != null) {
                int distanceToSafeSpot = currentLoc.distanceTo(closestSafeSpot);
                if (distanceToSafeSpot <= 10) {
                    if (!Rs2Player.isMoving() || distanceToSafeSpot > 3) {
                        Microbot.log("[WildernessNicky] 🦴 Skeleton combat - running to safe logout spot at: " + closestSafeSpot + " (Distance: " + distanceToSafeSpot + " tiles)");
                        Rs2Walker.walkTo(closestSafeSpot, 2);
                    }
                    return; // Keep trying to reach safe logout spot
                }
            }

            // PRIORITY 3: Get best escape zone based on current position (ladder/gate/etc.)
            WorldPoint bestEscapeZone = getBestSkeletonEscapeZone();

            // If near ladder, try to go down (instant combat break)
            if (currentLoc != null && currentLoc.distanceTo(LADDER_AREA) < 10) {
                TileObject ladder = Rs2GameObject.getAll(o -> o.getId() == LADDER_OBJECT_ID, 104)
                    .stream().findFirst().orElse(null);
                if (ladder != null && !Rs2Player.isMoving()) {
                    Microbot.log("[WildernessNicky] 🪜 LADDER ESCAPE: Going underground to break skeleton combat");
                    Rs2GameObject.interact(ladder, "Climb-down");
                    sleep(1200); // Wait for climb animation
                    return; // Let next cycle check if we're underground
                }
            }

            // If near gate, try to open and exit (breaks skeleton aggro)
            if (currentLoc != null && currentLoc.distanceTo(GATE_AREA) < 10) {
                TileObject gate = Rs2GameObject.getAll(o -> o.getId() == GATE_OBJECT_ID, 104)
                    .stream().findFirst().orElse(null);
                if (gate != null && !Rs2Player.isMoving()) {
                    Microbot.log("[WildernessNicky] 🚪 GATE ESCAPE: Exiting course to break skeleton combat");
                    Rs2GameObject.interact(gate, "Open");
                    sleep(600);
                    // After opening, walk through it
                    Rs2Walker.walkTo(bestEscapeZone, 2);
                    return; // Let next cycle continue
                }
            }

            // Default: Run to best escape zone
            if (currentLoc != null && currentLoc.distanceTo(bestEscapeZone) > 3 && !Rs2Player.isMoving()) {
                Microbot.log("[WildernessNicky] 🦴 Skeleton combat - running to: " + bestEscapeZone);
                Rs2Walker.walkTo(bestEscapeZone, 2);
            }
        }

        // Always spam logout (works instantly if combat breaks)
        // Every 100ms - tries to logout using Rs2Player.logout() (direct menu entry invocation)
        // This is the fastest logout method available in the API
        // IMPORTANT: We keep trying to logout throughout the ENTIRE escape, not just for 3 seconds
        Rs2Player.logout();
        sleep(100); // Small delay to prevent excessive CPU usage

        // Check if logout succeeded
        if (!Microbot.isLoggedIn()) {
            Microbot.log("[WildernessNicky] ✅ Successfully logged out during emergency escape!");

            // If in solo mode, world hop to find safer world
            if (config.playMode() == WildernessNickyConfig.PlayMode.SOLO && soloModeWorldHopAttempts < MAX_SOLO_WORLD_HOP_ATTEMPTS) {
                soloModeWorldHopAttempts++;
                Microbot.log("[WildernessNicky] 🌍 Solo mode world hop attempt " + soloModeWorldHopAttempts + "/" + MAX_SOLO_WORLD_HOP_ATTEMPTS);
                sleep(2000); // Wait 2 seconds before world hop

                int currentWorld = Rs2Player.getWorld();
                int nextWorld = -1;

                // Try custom world list first
                if (!parsedSoloWorlds.isEmpty()) {
                    nextWorld = getNextSoloWorld();
                    Microbot.log("[WildernessNicky] Using custom solo hop world: " + nextWorld);

                    // If same world, get next one
                    if (nextWorld == currentWorld && parsedSoloWorlds.size() > 1) {
                        nextWorld = getNextSoloWorld();
                    }
                } else {
                    // Fallback to random world
                    nextWorld = net.runelite.client.plugins.microbot.util.security.Login.getRandomWorld(Rs2Player.isMember());

                    // Make sure we don't hop to the same world
                    int attempts = 0;
                    while (nextWorld == currentWorld && attempts < 10) {
                        nextWorld = net.runelite.client.plugins.microbot.util.security.Login.getRandomWorld(Rs2Player.isMember());
                        attempts++;
                    }
                }

                if (nextWorld > 0 && nextWorld != currentWorld) {
                    Microbot.log("[WildernessNicky] Attempting to hop from world " + currentWorld + " to world " + nextWorld);
                    Microbot.cantHopWorld = false;

                    boolean hopSuccess = Microbot.hopToWorld(nextWorld);
                    if (hopSuccess) {
                        final int targetWorld = nextWorld;
                        boolean hopConfirmed = sleepUntil(() -> Rs2Player.getWorld() == targetWorld && Microbot.isLoggedIn(), 15000);

                        if (hopConfirmed) {
                            Microbot.log("[WildernessNicky] ✅ Successfully hopped to world " + nextWorld);
                            // Reset escape state and continue
                            resetEscapeMode();
                            currentState = ObstacleState.START;
                            return;
                        } else {
                            Microbot.log("[WildernessNicky] ⚠️ World hop not confirmed");
                        }
                    }
                } else {
                    Microbot.log("[WildernessNicky] ⚠️ No valid world found for hopping");
                }

                // If max hops reached, stop plugin
                if (soloModeWorldHopAttempts >= MAX_SOLO_WORLD_HOP_ATTEMPTS) {
                    Microbot.log("[WildernessNicky] ❌ Max world hop attempts reached - stopping plugin");
                    sleep(2000);
                    Microbot.stopPlugin(plugin);
                }
            }

            return;
        }

        // ===== STEP 4: LOGOUT SUCCESS CHECK =====
        // If we're still here, logout failed - we're in combat
        // Prayer activation and food eating already handled at top of method

        // ===== STEP 5: PHYSICAL ESCAPE TO MAGE BANK =====
        // Continue physical escape while STILL trying to logout (no timeout)
        // We spam logout throughout the entire escape until we reach Mage Bank
        // Only log the transition once
        if (attemptingLogout) {
            long logoutAttemptTime = System.currentTimeMillis() - logoutAttemptStartTime;

            if (logoutAttemptTime >= MAX_LOGOUT_ATTEMPT_TIME) {
                // Log once that we're now doing physical escape while still trying logout
                Microbot.log("[WildernessNicky] ⏱️ Still in combat after 3s - moving to safety while spamming logout");
                attemptingLogout = false; // Just to stop this log from repeating
                // DON'T stop trying to logout - it happens at line 2809 every 100ms
            } else {
                // Still within initial 3 second window - don't do physical escape yet
                return; // Keep trying logout without moving
            }
        }

        // ===== STEP 5: WIKI SAFE ZONE ROUTING =====
        // If logout fails after 3 seconds, execute smart escape route to safe zones
        // These safe zones are NPC-dense areas where PKers have difficulty following
        // Routes include: Skeleton aggro, Ice Warriors, Pirates' Hideout, Deep Dungeon, Mage Bank

        // Select best escape route if not already chosen
        if (selectedEscapeRoute == null) {
            selectedEscapeRoute = selectBestEscapeRoute();
            Microbot.log("[WildernessNicky] 🗺️ Selected escape route: " + selectedEscapeRoute.name());
        }

        // Execute escape route (handles movement and logout attempts at safe zones)
        executeEscapeRoute();

        // SAFETY CHECK: Allow escape mode to be cancelled if conditions are met
        // This prevents getting stuck in escape mode forever
        if (canExitEscapeMode()) {
            Microbot.log("[WildernessNicky] ✅ Safe conditions detected - exiting escape mode and resuming normal operation");
            resetEscapeMode();
            currentState = ObstacleState.START; // Resume from start
            return;
        }

        // Check for timeout
        if (timeSinceStart > EMERGENCY_ESCAPE_TIMEOUT) {
            String timeoutReason = "Emergency escape timeout after " + (EMERGENCY_ESCAPE_TIMEOUT/1000) + " seconds (Original reason: " + lastEscapeReason + ")";
            Microbot.log("[WildernessNicky] ⏱️ " + timeoutReason);
            Microbot.log("[WildernessNicky] 🚪 Logging out for safety");
            lastEscapeReason = timeoutReason;
            Rs2Player.logout();
            return;
        }
        
        // ========================================
        // ESCAPE STEP 1: EQUIP PHOENIX NECKLACE
        // ========================================
        // Equips Phoenix necklace while moving (NO blocking waits)
        // Phoenix necklace teleports player to Edgeville if killed, saving items
        if (!hasEquippedPhoenixNecklace && Rs2Inventory.hasItem("Phoenix necklace") && !Rs2Equipment.isWearing("Phoenix necklace")) {
            escapeEquipNecklaceAttempts++;

            // Failsafe: Skip if too many attempts
            if (escapeEquipNecklaceAttempts > MAX_ESCAPE_STEP_ATTEMPTS) {
                Microbot.log("[WildernessNicky] ⚠️ Failed to equip Phoenix necklace after " + MAX_ESCAPE_STEP_ATTEMPTS + " attempts - skipping");
                hasEquippedPhoenixNecklace = true;
            } else {
                Rs2Inventory.wield("Phoenix necklace");
                Microbot.log("[WildernessNicky] 🔗 Equipping Phoenix necklace while escaping (Attempt " + escapeEquipNecklaceAttempts + ")");
            }
        }

        // Mark as equipped if wearing (check during every iteration)
        if (!hasEquippedPhoenixNecklace && Rs2Equipment.isWearing("Phoenix necklace")) {
            hasEquippedPhoenixNecklace = true;
            Microbot.log("[WildernessNicky] ✅ Phoenix necklace equipped");
        }
        
        // ========================================
        // ESCAPE STEP 2: NAVIGATE ROCK AREA
        // ========================================
        // Climbs rocks obstacle to progress toward gate (CONTINUOUS MOVEMENT - no standing still)
        // Rock area is WorldArea(2991,3936 to 3001,3945) - final wilderness agility obstacle
        if (!hasClimbedRocks) {
            WorldArea rockArea = new WorldArea(SOUTH_WEST_CORNER, ROCK_AREA_WIDTH, ROCK_AREA_HEIGHT);
            WorldPoint currentLoc = Rs2Player.getWorldLocation();
            boolean isInRockArea = currentLoc != null && rockArea.contains(currentLoc);

            // Initialize step timer
            if (escapeStep2StartTime == 0) {
                escapeStep2StartTime = System.currentTimeMillis();
            }

            // Failsafe: Skip if timeout (player might be past this already)
            long stepElapsedTime = System.currentTimeMillis() - escapeStep2StartTime;
            if (stepElapsedTime > ESCAPE_STEP_TIMEOUT * 2) { // 20 seconds timeout
                Microbot.log("[WildernessNicky] ⚠️ Rock area navigation timeout - skipping to gate");
                hasClimbedRocks = true;
                escapeStep2StartTime = 0;
            } else if (isInRockArea) {
                // We're IN the rock area - climb rocks
                if (!Rs2Player.isMoving() || escapeClimbRocksAttempts == 0) {
                    escapeClimbRocksAttempts++;
                    Microbot.log("[WildernessNicky] 🧗 Climbing rocks (Attempt " + escapeClimbRocksAttempts + ")");
                    Rs2GameObject.interact(ROCKS_OBJECT_ID, "Climb");
                }
                // Don't wait - keep looping to maintain logout spam and prayer switching
            } else if (currentLoc != null && currentLoc.distanceTo(GATE_AREA) <= 3) {
                // Already past the rock area - mark as complete
                hasClimbedRocks = true;
                escapeStep2StartTime = 0;
                Microbot.log("[WildernessNicky] ✅ Past rock area - continuing to gate");
            } else {
                // Not in rock area yet - keep walking toward gate
                if (!Rs2Player.isMoving() || currentLoc.distanceTo(GATE_AREA) > 10) {
                    Rs2Walker.walkTo(GATE_AREA, 4);
                }
            }
        }
        
        // ========================================
        // ESCAPE STEP 3: OPEN GATE
        // ========================================
        // Opens wilderness gate to access Mage Bank area (NO STANDING STILL - open and keep moving)
        // Gate location: WorldPoint(2998, 3931, 0)
        if (!hasOpenedGate && hasClimbedRocks) {
            WorldPoint currentLoc = Rs2Player.getWorldLocation();
            WorldPoint mageBankPoint = new WorldPoint(2534, 4712, 0);

            if (currentLoc != null) {
                // Check if we're already past the gate (closer to Mage Bank than gate)
                if (currentLoc.distanceTo(mageBankPoint) < currentLoc.distanceTo(GATE_AREA)) {
                    hasOpenedGate = true;
                    Microbot.log("[WildernessNicky] ✅ Past gate - heading to Mage Bank");
                } else {
                    // Not past gate yet - need to open it
                    int distanceToGate = currentLoc.distanceTo(GATE_AREA);

                    // Check if player fell off to the right (east of gate area)
                    boolean fellOffRight = currentLoc.getX() > GATE_AREA.getX() + 2;

                    if (fellOffRight && distanceToGate > 5) {
                        // Fell off to the right - need to walk back west to gate area
                        Microbot.log("[WildernessNicky] ⚠️ FELL OFF RIGHT - Walking back west to gate (Distance: " + distanceToGate + " tiles)");
                        Microbot.log("[WildernessNicky] Current position: " + currentLoc + ", Gate: " + GATE_AREA);

                        // Walk directly west toward the gate area
                        if (!Rs2Player.isMoving()) {
                            Rs2Walker.walkTo(GATE_AREA, 1); // Walk to exact gate location
                            sleep(1200, 1500); // Wait for walk to start
                        }
                    } else if (distanceToGate <= 5) {
                        // Close enough - try to open gate
                        // Find the CLOSEST gate to the gate area to avoid clicking wrong gates
                        List<TileObject> gates = Rs2GameObject.getAll(o -> o.getId() == GATE_OBJECT_ID, 104);
                        TileObject closestGate = null;
                        int closestDistance = Integer.MAX_VALUE;

                        for (TileObject gateObj : gates) {
                            if (gateObj != null && gateObj.getWorldLocation() != null) {
                                int distToGateArea = gateObj.getWorldLocation().distanceTo(GATE_AREA);
                                if (distToGateArea < closestDistance) {
                                    closestDistance = distToGateArea;
                                    closestGate = gateObj;
                                }
                            }
                        }

                        if (closestGate != null && escapeOpenGateAttempts < MAX_ESCAPE_STEP_ATTEMPTS) {
                            escapeOpenGateAttempts++;
                            Microbot.log("[WildernessNicky] 🚪 Opening gate at " + closestGate.getWorldLocation() + " (Attempt " + escapeOpenGateAttempts + ")");
                            Rs2GameObject.interact(closestGate, "Open");
                            sleep(600, 1200); // Wait for gate interaction
                        } else if (escapeOpenGateAttempts >= MAX_ESCAPE_STEP_ATTEMPTS) {
                            // Skip gate if too many attempts
                            hasOpenedGate = true;
                            Microbot.log("[WildernessNicky] ⚠️ Gate opening skipped - continuing");
                        }
                    } else {
                        // Too far from gate - walk towards it (general recovery)
                        if (!Rs2Player.isMoving() || distanceToGate > 10) {
                            Microbot.log("[WildernessNicky] 🚶 Walking back to gate (Distance: " + distanceToGate + " tiles)");

                            // Walk directly to the GATE_AREA point
                            Rs2Walker.walkTo(GATE_AREA, 1);
                            sleep(600); // Wait for walk to start
                        }
                    }
                }
            }
        }

        // ========================================
        // ESCAPE STEP 4: RUN TO MAGE BANK
        // ========================================
        // Final escape destination - Mage Bank safe zone (NEVER STOP - spam walk + logout)
        // Mage Bank location: WorldPoint(2534, 4712, 0) - underground safe zone
        // Safe zone radius: 10 tiles from bank
        WorldPoint mageBankPoint = new WorldPoint(2534, 4712, 0);
        WorldPoint currentLoc = Rs2Player.getWorldLocation();

        if (currentLoc != null) {
            int distanceToBank = currentLoc.distanceTo(mageBankPoint);

            // ===== PRIORITY 1: CHECK IF AT SAFE LOGOUT LOCATION (WILDERNESS AGILITY COURSE) =====
            // If player is at a safe logout spot within the course, force logout immediately
            // This prevents the long run to Mage Bank and allows quick logout at nearby safe spots
            if (isSafeLogoutLocation()) {
                Microbot.log("[WildernessNicky] ✅ At safe logout location in Wilderness Agility Course!");
                Microbot.log("[WildernessNicky] 📋 Escape reason: " + lastEscapeReason);
                Microbot.log("[WildernessNicky] 🚪 Forcing logout at safe spot...");

                // Force logout until successful
                int logoutAttempts = 0;
                while (Microbot.isLoggedIn() && logoutAttempts < 50) {
                    Rs2Player.logout();
                    sleep(100);
                    logoutAttempts++;
                }

                // Reset escape state
                resetEscapeMode();
                return;
            }

            // ===== PRIORITY 2: CHECK IF CLOSE TO A SAFE LOGOUT LOCATION =====
            // If within 10 tiles of a safe logout spot, navigate there instead of Mage Bank
            WorldPoint closestSafeSpot = getClosestSafeLogoutLocation();
            if (closestSafeSpot != null) {
                int distanceToSafeSpot = currentLoc.distanceTo(closestSafeSpot);

                // If safe spot is closer than Mage Bank and within 15 tiles, go there instead
                if (distanceToSafeSpot <= 15 && distanceToSafeSpot < distanceToBank) {
                    if (!Rs2Player.isMoving() || distanceToSafeSpot > 3) {
                        if (escapeWalkToMageBankAttempts % 5 == 1) { // Log every 5 attempts to reduce spam
                            Microbot.log("[WildernessNicky] 🏃 Running to nearby safe logout spot (Distance: " + distanceToSafeSpot + " tiles)");
                        }
                        Rs2Walker.walkTo(closestSafeSpot, 2);
                        escapeWalkToMageBankAttempts++;
                        return; // Skip Mage Bank logic and try again next iteration
                    }
                }
            }

            // ===== PRIORITY 3: CHECK IF REACHED MAGE BANK SAFE ZONE =====
            // Only navigate to Mage Bank if no safe logout spots are nearby
            if (distanceToBank <= 10) {
                Microbot.log("[WildernessNicky] ✅ Reached Mage Bank safe zone!");
                Microbot.log("[WildernessNicky] 📋 Escape reason: " + lastEscapeReason);
                Microbot.log("[WildernessNicky] 🚪 Forcing logout...");

                // Force logout until successful
                int logoutAttempts = 0;
                while (Microbot.isLoggedIn() && logoutAttempts < 50) {
                    Rs2Player.logout();
                    sleep(100);
                    logoutAttempts++;
                }

                // Reset escape state
                resetEscapeMode();
                return;
            }

            // KEEP MOVING - if we're not moving or far from bank, walk there
            if (!Rs2Player.isMoving() || distanceToBank > 5) {
                escapeWalkToMageBankAttempts++;
                if (escapeWalkToMageBankAttempts % 5 == 1) { // Log every 5 attempts to reduce spam
                    Microbot.log("[WildernessNicky] 🏃 Running to Mage Bank (Distance: " + distanceToBank + " tiles)");
                }
                Rs2Walker.walkTo(mageBankPoint, 20);
            }
        }

        // Failsafe: If we've been trying too long, force logout wherever we are
        if (escapeWalkToMageBankAttempts > MAX_ESCAPE_STEP_ATTEMPTS * 10) { // 50 attempts
            Microbot.log("[WildernessNicky] ⚠️ Escape timeout - forcing logout at current location");
            Rs2Player.logout();
            resetEscapeMode();
        }
    }

    /**
     * Checks if it's safe to exit escape mode
     * Conditions for exiting:
     * 1. No immediate threats nearby
     * 2. Health is above critical threshold (or health check is disabled)
     * 3. Phoenix necklace check is disabled OR player has phoenix necklace
     * 4. Been in escape mode for at least 30 seconds (to ensure initial threat passed)
     */
    private boolean canExitEscapeMode() {
        long timeSinceEscapeStart = System.currentTimeMillis() - emergencyEscapeStartTime;

        // Must be in escape mode for at least 30 seconds before allowing exit
        if (timeSinceEscapeStart < 30000) {
            return false;
        }

        // Check 1: No threats nearby
        if (isPlayerThreatNearby()) {
            return false;
        }

        // Check 2: Health is safe (if health check is enabled)
        if (config.leaveAtHealthPercent() > 0) {
            // Require health to be at least 20% above the escape threshold to exit
            int safeHealthThreshold = Math.min(100, config.leaveAtHealthPercent() + 20);
            if (Rs2Player.getHealthPercentage() < safeHealthThreshold) {
                return false;
            }
        }

        // Check 3: Phoenix necklace requirement (if enabled)
        if (config.phoenixEscape()) {
            if (!hasPhoenixNecklace()) {
                return false;
            }
        }

        // All conditions met - safe to exit escape mode
        return true;
    }

    /**
     * SKELETON COMBAT DETECTION
     * Checks if player is currently in combat with a skeleton/NPC
     * @return true if being attacked by NPC (not player)
     */
    private boolean isInSkeletonCombat() {
        Actor interacting = Microbot.getClient().getLocalPlayer().getInteracting();
        if (interacting == null) return false;

        // Check if interacting with NPC (not player)
        return !(interacting instanceof net.runelite.api.Player);
    }

    /**
     * BEST SKELETON ESCAPE ZONE
     * Determines the best escape point based on current location
     * Priority: Ladder > Gate > Rock Exit > START_POINT
     * @return WorldPoint to escape to
     */
    private WorldPoint getBestSkeletonEscapeZone() {
        WorldPoint currentPos = Rs2Player.getWorldLocation();
        if (currentPos == null) return START_POINT;

        // Priority 1: If near ladder (within 10 tiles), go down to underground pit
        if (currentPos.distanceTo(LADDER_AREA) < 10) {
            // Check if ladder is actually visible
            TileObject ladder = Rs2GameObject.getAll(o -> o.getId() == LADDER_OBJECT_ID, 104)
                .stream().findFirst().orElse(null);
            if (ladder != null) {
                Microbot.log("[WildernessNicky] 🪜 Ladder escape available - going underground");
                return ladder.getWorldLocation();
            }
        }

        // Priority 2: If near gate (within 10 tiles), exit through gate
        if (currentPos.distanceTo(GATE_AREA) < 10) {
            Microbot.log("[WildernessNicky] 🚪 Gate escape available - exiting course");
            return GATE_AREA;
        }

        // Priority 3: If in rock area, run to edge
        WorldArea rockArea = new WorldArea(SOUTH_WEST_CORNER, ROCK_AREA_WIDTH, ROCK_AREA_HEIGHT);
        if (rockArea.contains(currentPos)) {
            Microbot.log("[WildernessNicky] 🏃 Rock area - running to edge");
            return ROCK_EXIT_POINT;
        }

        // Priority 4: Default to START_POINT
        Microbot.log("[WildernessNicky] ⬅️ Default escape - running to start");
        return START_POINT;
    }

    /**
     * SAFE LOGOUT LOCATION CHECK
     * Checks if the player is currently at a safe logout location within the Wilderness Agility Course.
     * Safe locations are spots where skeletons won't attack, allowing for safe logouts.
     *
     * @return true if player is within SAFE_LOGOUT_RADIUS (2 tiles) of any safe logout location
     */
    private boolean isSafeLogoutLocation() {
        WorldPoint currentPos = Rs2Player.getWorldLocation();
        if (currentPos == null) {
            return false;
        }

        // Check if player is within radius of any safe logout location
        for (WorldPoint safeSpot : SAFE_LOGOUT_LOCATIONS) {
            if (currentPos.distanceTo(safeSpot) <= SAFE_LOGOUT_RADIUS) {
                return true;
            }
        }

        return false;
    }

    /**
     * CLOSEST SAFE LOGOUT LOCATION
     * Finds the nearest safe logout location to the player's current position.
     * Used when navigating to a safe spot before logging out.
     *
     * @return WorldPoint of the closest safe logout location, or null if no safe spots available
     */
    private WorldPoint getClosestSafeLogoutLocation() {
        WorldPoint currentPos = Rs2Player.getWorldLocation();
        if (currentPos == null) {
            return null;
        }

        WorldPoint closestSafeSpot = null;
        int closestDistance = Integer.MAX_VALUE;

        for (WorldPoint safeSpot : SAFE_LOGOUT_LOCATIONS) {
            int distance = currentPos.distanceTo(safeSpot);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestSafeSpot = safeSpot;
            }
        }

        return closestSafeSpot;
    }

    /**
     * Resets all escape mode variables to default state
     */
    private void resetEscapeMode() {
        emergencyEscapeTriggered = false;
        emergencyEscapeStartTime = 0;
        hasEquippedPhoenixNecklace = false;
        hasClimbedRocks = false;
        hasOpenedGate = false;
        escapeStep2StartTime = 0;
        phoenixEscapeTriggered = false;
        phoenixEscapeStartTime = 0;

        // Reset wiki safe zone escape route tracking
        selectedEscapeRoute = null;
        hasReachedSafeZone = false;
        safeZoneReachedTime = 0;
    }

    /**
     * Attempts to hop to a world with retry logic and proper error handling.
     * Enhanced with world validation and faster hop detection.
     *
     * @param targetWorld The world number to hop to
     * @param context Context string for logging (e.g., "banking", "returning")
     * @return true if hop was successful, false if failed after retries
     */
    private boolean attemptWorldHop(int targetWorld, String context) {
        // Check if we're already on the target world
        if (Rs2Player.getWorld() == targetWorld) {
            return true;
        }

        // Validate target world availability before attempting hop
        if (!isWorldAvailable(targetWorld)) {
            Microbot.log("[WildernessNicky] ⚠️ World " + targetWorld + " not available - skipping hop in " + context);
            worldHopRetryCount = 0;
            return false;
        }

        // Initialize retry tracking if this is a new attempt
        if (worldHopRetryCount == 0) {
            worldHopRetryStartTime = System.currentTimeMillis();
        }

        // Check if we've exceeded max retries or timeout
        long timeSinceStart = System.currentTimeMillis() - worldHopRetryStartTime;
        if (worldHopRetryCount >= MAX_WORLD_HOP_RETRIES || timeSinceStart > WORLD_HOP_RETRY_TIMEOUT) {
            Microbot.log("[WildernessNicky] ⚠️ World hop to " + targetWorld + " failed after " + worldHopRetryCount + " attempts in " + context);
            worldHopRetryCount = 0; // Reset for next attempt
            return false;
        }

        worldHopRetryCount++;

        // Reset cantHopWorld flag before attempting hop
        Microbot.cantHopWorld = false;

        Microbot.log("[WildernessNicky] 🌍 Hopping to world " + targetWorld + " (attempt " + worldHopRetryCount + "/" + MAX_WORLD_HOP_RETRIES + ") - " + context);

        // Attempt to initiate world hop
        boolean hopInitiated = Microbot.hopToWorld(targetWorld);
        if (!hopInitiated) {
            Microbot.log("[WildernessNicky] ❌ Failed to initiate hop to " + targetWorld);
            return false; // Will retry on next call
        }

        // Wait for hop confirmation with faster 3s timeout (reduced from 8s)
        boolean hopConfirmed = sleepUntil(() -> Rs2Player.getWorld() == targetWorld, 3000);
        if (!hopConfirmed) {
            Microbot.log("[WildernessNicky] ⏱️ Hop timeout - current world: " + Rs2Player.getWorld());
            return false; // Will retry on next call
        }

        // Success! Reset retry counter
        Microbot.log("[WildernessNicky] ✅ Successfully hopped to world " + targetWorld);
        worldHopRetryCount = 0;
        return true;
    }

    /**
     * Helper method to check if a world is available and accessible.
     * Validates that the world exists and is not a restricted world type.
     *
     * @param worldNumber The world number to check
     * @return true if world is available, false otherwise
     */
    private boolean isWorldAvailable(int worldNumber) {
        try {
            // Validate world number first
            if (worldNumber <= 0 || worldNumber > 9999) {
                return false; // Invalid world number
            }

            // Check if world exists and is accessible using getWorldList()
            net.runelite.api.World[] worlds = Microbot.getClient().getWorldList();
            if (worlds == null || worlds.length == 0) {
                return false;
            }

            for (net.runelite.api.World world : worlds) {
                if (world.getId() == worldNumber) {
                    // Check if world is not Deadman or Tournament
                    String activity = world.getActivity();
                    if (activity != null && (activity.contains("DEADMAN") || activity.contains("TOURNAMENT"))) {
                        return false;
                    }
                    return true; // World exists and is accessible
                }
            }
            return false; // World not found
        } catch (Exception e) {
            Microbot.log("[WildernessNicky] ⚠️ Error checking world availability: " + e.getMessage());
            return false; // Assume not available if error occurs
        }
    }

    public String getPreviousLapTime() {
        if (previousLapTime == 0) return "-";
        return String.format("%.2f s", previousLapTime / 1000.0);
    }

    public String getFastestLapTime() {
        if (fastestLapTime == Long.MAX_VALUE) return "-";
        return String.format("%.2f s", fastestLapTime / 1000.0);
    }

    private void setupWorldHop() {
        originalWorld = Rs2Player.getWorld();
        bankWorld1 = getConfigWorld(config.bankWorld1());
        bankWorld2 = getConfigWorld(config.bankWorld2());
    }

    /**
     * Parse mass world from config (String input)
     * @return world number, or -1 if not set/invalid
     */
    private int getMassWorldFromConfig() {
        String massWorldStr = config.massWorld();
        if (massWorldStr == null || massWorldStr.trim().isEmpty()) {
            return -1;
        }

        try {
            int world = Integer.parseInt(massWorldStr.trim());
            if (world <= 0) {
                return -1; // 0 or negative means random/not set
            }
            return world;
        } catch (NumberFormatException e) {
            Microbot.log("[WildernessNicky] ⚠️ Invalid mass world number: " + massWorldStr);
            return -1;
        }
    }

    private int getConfigWorld(WildernessNickyConfig.BankWorldOption option) {
        if (option == WildernessNickyConfig.BankWorldOption.Random) {
            // Pick a random world from the enum list, skipping the current world
            List<WildernessNickyConfig.BankWorldOption> all = Arrays.asList(WildernessNickyConfig.BankWorldOption.values());
            List<Integer> worldNums = new ArrayList<>();
            for (WildernessNickyConfig.BankWorldOption o : all) {
                if (o != WildernessNickyConfig.BankWorldOption.Random) {
                    int num = Integer.parseInt(o.name().substring(1));
                    if (num != Rs2Player.getWorld()) worldNums.add(num);
                }
            }
            if (worldNums.isEmpty()) return Rs2Player.getWorld();
            return worldNums.get(new Random().nextInt(worldNums.size()));
        } else {
            return Integer.parseInt(option.name().substring(1));
        }
    }

    private void handleWorldHop1() {
        if (!attemptWorldHop(bankWorld1, "banking hop 1")) {
            return; // Stay in this state to retry
        }
        sleep(4000); // Wait 4 seconds after hop
        if (config.leaveFcOnWorldHop()) {
            leaveFriendChat();
        }
        currentState = ObstacleState.WORLD_HOP_2;
    }

    private void handleWorldHop2() {
        if (!attemptWorldHop(bankWorld2, "banking hop 2")) {
            return; // Stay in this state to retry
        }
        currentState = ObstacleState.WALK_TO_LEVER;
    }

    private void handleSwapBack() {
        if (Rs2Player.getWorld() == originalWorld) {
            // Only join FC if not already in it
            if (config.joinFc() && !isInFriendChat()) {
                joinFriendChat();
            }
            currentState = ObstacleState.PIPE;
            return;
        }

        if (!attemptWorldHop(originalWorld, "returning to original world")) {
            return; // Stay in this state to retry
        }

        // Only join FC if not already in it
        if (config.joinFc() && !isInFriendChat()) {
            joinFriendChat();
        }
        currentState = ObstacleState.PIPE;
    }

    private void leaveFriendChat() {
        try {
            // Check if already in a clan channel
            net.runelite.api.clan.ClanChannel clanChannel = Microbot.getClient().getClanChannel();
            if (clanChannel == null) {
                Microbot.log("[WildernessNicky] Not in any clan channel");
                return;
            }

            String channelName = clanChannel.getName();
            Microbot.log("[WildernessNicky] 🚪 Leaving clan channel: " + channelName);

            // PRIMARY: Widget-based leave
            Rs2Tab.switchTo(InterfaceTab.CHAT);
            sleep(300, 600);

            // Note: Leave button uses same widget as Join button
            boolean foundLeaveButton = Rs2Widget.sleepUntilHasWidgetText("Leave",
                WidgetIndices.ChatChannel.GROUP_INDEX,
                WidgetIndices.ChatChannel.JOIN_LABEL, false, 2000);

            if (!foundLeaveButton) {
                Microbot.log("[WildernessNicky] ⚠️ Leave button not found - using direct keyboard entry");
                // FALLBACK: Type "//" to leave channel
                Rs2Keyboard.typeString("//");
                sleep(300);
                Rs2Keyboard.enter();
                sleep(1000);
            } else {
                // Click Leave button (uses same container as Join)
                Rs2Widget.clickWidget(WidgetIndices.ChatChannel.GROUP_INDEX,
                        WidgetIndices.ChatChannel.JOIN_DYNAMIC_CONTAINER);
                sleep(1000, 1500);
            }

            // Verify left
            clanChannel = Microbot.getClient().getClanChannel();
            if (clanChannel == null) {
                Microbot.log("[WildernessNicky] ✅ Successfully left clan channel");
            } else {
                Microbot.log("[WildernessNicky] ⚠️ May still be in clan channel");
            }
        } catch (Exception e) {
            Microbot.log("[WildernessNicky] ❌ Error leaving clan channel: " + e.getMessage());
        }
    }

    private void joinFriendChat() {
        // Check cooldown to prevent spam
        long timeSinceLastAttempt = System.currentTimeMillis() - lastFcJoinAttemptTime;
        if (timeSinceLastAttempt < FC_JOIN_COOLDOWN) {
            // Still on cooldown - don't spam join attempts
            return;
        }

        // Update last attempt time
        lastFcJoinAttemptTime = System.currentTimeMillis();

        Microbot.log("[WildernessNicky] 🔗 Attempting to join FC: " + config.fcChannel());
        joinChatChannel(config.fcChannel());
    }

    /**
     * Helper method to check if player is in the configured friends chat.
     * @return true if FC is disabled OR player is in the configured channel
     */
    private boolean isInFriendChat() {
        if (!config.joinFc()) {
            return true; // Not using FC, consider "in chat"
        }

        String targetChannel = config.fcChannel();
        if (targetChannel == null || targetChannel.trim().isEmpty()) {
            return true; // No channel configured
        }

        try {
            net.runelite.api.clan.ClanChannel clanChannel = Microbot.getClient().getClanChannel();
            if (clanChannel == null) {
                return false; // Not in any channel
            }

            String currentChannelName = clanChannel.getName();
            if (currentChannelName == null) {
                return false; // Channel name unavailable
            }

            // Case-insensitive comparison with trimmed names
            boolean inCorrectChannel = currentChannelName.trim().equalsIgnoreCase(targetChannel.trim());

            // Debug log (remove after testing)
            if (inCorrectChannel) {
                Microbot.log("[WildernessNicky] ✅ Already in FC: " + currentChannelName);
            }

            return inCorrectChannel;
        } catch (Exception e) {
            Microbot.log("[WildernessNicky] ⚠️ Error checking FC status: " + e.getMessage());
            return false; // Assume not in FC on error (safer to attempt join)
        }
    }

    private void joinChatChannel(String channelName) {
        try {
            if (channelName == null || channelName.trim().isEmpty()) {
                Microbot.log("[WildernessNicky] No clan channel name configured - skipping join");
                return;
            }

            // Check if already in the target channel
            net.runelite.api.clan.ClanChannel clanChannel = Microbot.getClient().getClanChannel();
            if (clanChannel != null && clanChannel.getName().equalsIgnoreCase(channelName.trim())) {
                Microbot.log("[WildernessNicky] Already in clan channel: " + channelName);
                return;
            }

            Microbot.log("[WildernessNicky] Joining clan channel: " + channelName);

            // Join via chat-channel using widget method
            Rs2Tab.switchTo(InterfaceTab.CHAT);
            sleep(300, 600);

            // Wait for the Join button to appear
            boolean foundJoinButton = Rs2Widget.sleepUntilHasWidgetText("Join",
                WidgetIndices.ChatChannel.GROUP_INDEX,
                WidgetIndices.ChatChannel.JOIN_LABEL, false, 2000);

            if (!foundJoinButton) {
                Microbot.log("[WildernessNicky] ⚠️ Join button not found - using direct keyboard entry");
                // FALLBACK: Type channel name directly (NOT "/join")
                Rs2Keyboard.typeString(channelName);
                sleep(300);
                Rs2Keyboard.enter();
                sleep(1000, 1500);
            } else {
                // PRIMARY: Click Join button via widget
                Rs2Widget.clickWidget(WidgetIndices.ChatChannel.GROUP_INDEX,
                        WidgetIndices.ChatChannel.JOIN_DYNAMIC_CONTAINER);
                sleep(600, 1000);

                // Type the channel name
                Rs2Keyboard.typeString(channelName);
                sleep(300, 500);
                Rs2Keyboard.enter();
                sleep(1000, 1500);
            }

            // Verify we joined
            clanChannel = Microbot.getClient().getClanChannel();
            if (clanChannel != null && clanChannel.getName().equalsIgnoreCase(channelName.trim())) {
                Microbot.log("[WildernessNicky] ✅ Successfully joined clan channel: " + channelName);
            } else {
                Microbot.log("[WildernessNicky] ⚠️ May not have joined clan channel");
            }
        } catch (Exception e) {
            Microbot.log("[WildernessNicky] Error joining clan channel: " + e.getMessage());
        }
    }

    private void handleWalkToLever() {
        // Player Monitor not used - built-in anti-PK system is sufficient

        // Initialize web walk timeout tracking
        if (webWalkStartTime == 0) {
            webWalkStartTime = System.currentTimeMillis();
            Microbot.log("[WildernessNicky] Starting web walk to Mage Bank");
        }
        
        // Check for timeout
        long timeSinceStart = System.currentTimeMillis() - webWalkStartTime;
        if (timeSinceStart > WEB_WALK_TIMEOUT) {
            Microbot.log("[WildernessNicky] Web walk to Mage Bank timed out after " + (WEB_WALK_TIMEOUT/1000) + " seconds, forcing to banking state");
            webWalkStartTime = 0; // Reset for next attempt
            currentState = ObstacleState.BANKING;
            return;
        }
        
        // Use web walking directly to Mage Bank instead of manual lever interaction
        WorldPoint mageBankTile = new WorldPoint(2534, 4712, 0);
        
        // Use web walking which will handle the lever interaction automatically
        boolean walkSuccess = Rs2Walker.walkTo(mageBankTile, 5); // Allow 5 tile distance for arrival
        if (walkSuccess) {
            Microbot.log("[WildernessNicky] Successfully reached Mage Bank area");
            webWalkStartTime = 0; // Reset for next attempt
            currentState = ObstacleState.BANKING;
        } else {
            // Check if we're close enough to Mage Bank to consider it successful
            WorldPoint currentLoc = Rs2Player.getWorldLocation();
            if (currentLoc != null && currentLoc.distanceTo(mageBankTile) <= 10) {
                Microbot.log("[WildernessNicky] Close enough to Mage Bank, proceeding to banking");
                webWalkStartTime = 0; // Reset for next attempt
                currentState = ObstacleState.BANKING;
            } else {
                // Stay in this state to retry, but log progress
                if (timeSinceStart % 10000 < 100) { // Log every 10 seconds
                    Microbot.log("[WildernessNicky] Still walking to Mage Bank... (" + (timeSinceStart/1000) + "s elapsed)");
                }
            }
        }
    }

    private void handleInteractLever() {
        // This method is no longer used since we're using web walking directly to Mage Bank
        // The web walker handles the lever interaction automatically
        Microbot.log("[WildernessNicky] handleInteractLever called but using web walking instead");
        currentState = ObstacleState.BANKING;
    }

    /**
     * Handles buying a new looting bag from the Grand Exchange after death.
     * Flow: Walk to GE → Buy looting bag note → Use note on banker → Go to bank
     */
    private void handleGEBuyLootingBag() {
        Microbot.log("[WildernessNicky] 🛒 Starting GE looting bag purchase");

        // Check if we already have a looting bag (shouldn't happen, but check)
        if (Rs2Inventory.hasItem(LOOTING_BAG_CLOSED_ID) || Rs2Inventory.hasItem(LOOTING_BAG_OPEN_ID)) {
            Microbot.log("[WildernessNicky] ✅ Already have looting bag - skipping GE purchase");
            needsToBuyLootingBag = false;
            currentState = ObstacleState.BANKING;
            return;
        }

        // Step 1: Walk to GE if not there
        if (!Rs2GrandExchange.isOpen()) {
            if (!isNearGrandExchange()) {
                Microbot.log("[WildernessNicky] 🚶 Walking to Grand Exchange");
                Rs2GrandExchange.walkToGrandExchange();
                if (!sleepUntil(this::isNearGrandExchange, 30000)) {
                    Microbot.log("[WildernessNicky] ⚠️ Failed to reach Grand Exchange - will retry");
                    return; // Stay in this state to retry
                }
            }

            // Step 2: Open GE
            Microbot.log("[WildernessNicky] 🏦 Opening Grand Exchange");
            if (!Rs2GrandExchange.openExchange()) {
                Microbot.log("[WildernessNicky] ⚠️ Failed to open Grand Exchange - will retry");
                return; // Stay in this state to retry
            }
            if (!sleepUntil(Rs2GrandExchange::isOpen, 5000)) {
                Microbot.log("[WildernessNicky] ⚠️ Grand Exchange did not open - will retry");
                return; // Stay in this state to retry
            }
        }

        // Step 3: Calculate buy price with 3x price increase
        int lootingBagId = LOOTING_BAG_CLOSED_ID;
        int basePrice = Rs2GrandExchange.getPrice(lootingBagId);

        if (basePrice <= 0) {
            basePrice = 10000; // Fallback price if API fails
            Microbot.log("[WildernessNicky] ⚠️ Could not fetch price, using fallback: " + basePrice + " gp");
        }

        // Increase price 3x (hit up arrow 3 times = +15% total)
        int buyPrice = (int)(basePrice * 1.15);

        Microbot.log("[WildernessNicky] 💰 Buying looting bag for " + buyPrice + " gp (base: " + basePrice + " gp)");

        // Step 4: Buy 1x looting bag
        boolean buySuccess = Rs2GrandExchange.buyItem("Looting bag", buyPrice, 1);
        if (!buySuccess) {
            Microbot.log("[WildernessNicky] ⚠️ Failed to place buy offer - will retry");
            Rs2GrandExchange.closeExchange();
            return; // Stay in this state to retry
        }

        // Step 5: Wait for purchase to complete (up to 10 seconds)
        Microbot.log("[WildernessNicky] ⏳ Waiting for looting bag purchase to complete");
        boolean bought = sleepUntil(Rs2GrandExchange::hasBoughtOffer, 10000);

        if (!bought) {
            Microbot.log("[WildernessNicky] ⚠️ Purchase timeout - collecting anyway and will retry if needed");
        }

        // Step 6: Collect to inventory
        Microbot.log("[WildernessNicky] 📦 Collecting looting bag to inventory");
        Rs2GrandExchange.collectAllToInventory();
        sleepUntil(() -> Rs2Inventory.hasItem(LOOTING_BAG_CLOSED_ID), 3000);

        // Step 7: Close GE
        Rs2GrandExchange.closeExchange();
        sleepUntil(() -> !Rs2GrandExchange.isOpen(), 2000);

        // Step 8: Verify we have the looting bag
        if (Rs2Inventory.hasItem(LOOTING_BAG_CLOSED_ID)) {
            Microbot.log("[WildernessNicky] ✅ Successfully purchased looting bag!");
            needsToBuyLootingBag = false;
            currentState = ObstacleState.BANKING;
        } else {
            Microbot.log("[WildernessNicky] ⚠️ Looting bag not in inventory - will retry purchase");
            // Stay in this state to retry
        }
    }

    /**
     * STARTUP: Buy NOTED looting bag from GE (when no bag in bank)
     * This is for startup flow only - buys noted version which will be unnoted at bank with RoW
     */
    private void handleGEBuyNotedLootingBag() {
        Microbot.log("[WildernessNicky] 🛒 Starting GE noted looting bag purchase (startup)");

        // Check if we already have a noted looting bag
        if (Rs2Inventory.hasItem("Looting bag (noted)")) {
            Microbot.log("[WildernessNicky] ✅ Already have noted looting bag - proceeding to unnote");
            needsToBuyNotedLootingBag = false;
            currentState = ObstacleState.UNNOTE_LOOTING_BAG;
            return;
        }

        // Step 1: Walk to GE if not there
        if (!Rs2GrandExchange.isOpen()) {
            if (!isNearGrandExchange()) {
                Microbot.log("[WildernessNicky] 🚶 Walking to Grand Exchange");
                Rs2GrandExchange.walkToGrandExchange();
                if (!sleepUntil(this::isNearGrandExchange, 30000)) {
                    Microbot.log("[WildernessNicky] ⚠️ Failed to reach Grand Exchange - will retry");
                    return;
                }
            }

            // Step 2: Open GE
            Microbot.log("[WildernessNicky] 🏦 Opening Grand Exchange");
            if (!Rs2GrandExchange.openExchange()) {
                Microbot.log("[WildernessNicky] ⚠️ Failed to open Grand Exchange - will retry");
                return;
            }
            if (!sleepUntil(Rs2GrandExchange::isOpen, 5000)) {
                Microbot.log("[WildernessNicky] ⚠️ Grand Exchange did not open - will retry");
                return;
            }
        }

        // Step 3: Calculate buy price (1.15x for fast buy)
        int lootingBagId = LOOTING_BAG_CLOSED_ID;
        int basePrice = Rs2GrandExchange.getPrice(lootingBagId);

        if (basePrice <= 0) {
            basePrice = 10000; // Fallback price
            Microbot.log("[WildernessNicky] ⚠️ Could not fetch price, using fallback: " + basePrice + " gp");
        }

        int buyPrice = (int)(basePrice * 1.15);
        Microbot.log("[WildernessNicky] 💰 Buying NOTED looting bag for " + buyPrice + " gp (base: " + basePrice + " gp)");

        // Step 4: Buy 1x noted looting bag
        boolean buySuccess = Rs2GrandExchange.buyItem("Looting bag", buyPrice, 1);
        if (!buySuccess) {
            Microbot.log("[WildernessNicky] ⚠️ Failed to place buy offer - will retry");
            Rs2GrandExchange.closeExchange();
            return;
        }

        // Step 5: Wait for purchase to complete
        Microbot.log("[WildernessNicky] ⏳ Waiting for noted looting bag purchase");
        boolean bought = sleepUntil(Rs2GrandExchange::hasBoughtOffer, 10000);

        if (!bought) {
            Microbot.log("[WildernessNicky] ⚠️ Purchase timeout - collecting anyway");
        }

        // Step 6: Collect to inventory (should be noted)
        Microbot.log("[WildernessNicky] 📦 Collecting noted looting bag");
        Rs2GrandExchange.collectAllToInventory();
        sleepUntil(() -> Rs2Inventory.hasItem("Looting bag (noted)"), 3000);

        // Step 7: Close GE
        Rs2GrandExchange.closeExchange();
        sleepUntil(() -> !Rs2GrandExchange.isOpen(), 2000);

        // Step 8: Verify we have noted bag
        if (Rs2Inventory.hasItem("Looting bag (noted)")) {
            Microbot.log("[WildernessNicky] ✅ Successfully purchased noted looting bag!");
            needsToBuyNotedLootingBag = false;
            currentState = ObstacleState.UNNOTE_LOOTING_BAG;
        } else {
            Microbot.log("[WildernessNicky] ⚠️ Noted looting bag not in inventory - will retry");
            // Stay in this state to retry
        }
    }

    /**
     * STARTUP: Unnote the looting bag at bank using Ring of Wealth
     * Requires Ring of Wealth in inventory (any charges 1-5)
     */
    private void handleUnnoteLootingBag() {
        Microbot.log("[WildernessNicky] 📝 Unnoting looting bag at bank");

        // Check if we already have unnoted looting bag
        if (Rs2Inventory.hasItem(LOOTING_BAG_CLOSED_ID) || Rs2Inventory.hasItem(LOOTING_BAG_OPEN_ID)) {
            Microbot.log("[WildernessNicky] ✅ Already have unnoted looting bag - continuing startup");
            currentState = ObstacleState.STARTUP_BANKING; // Go back to banking to finish withdrawals
            return;
        }

        // Verify we have noted looting bag
        if (!Rs2Inventory.hasItem("Looting bag (noted)")) {
            Microbot.log("[WildernessNicky] ⚠️ No noted looting bag in inventory - going back to banking");
            currentState = ObstacleState.STARTUP_BANKING;
            return;
        }

        // Step 1: Walk to nearest bank if not there
        if (!Rs2Bank.isNearBank(10)) {
            Microbot.log("[WildernessNicky] 🚶 Walking to nearest bank for unnoting");
            Rs2Bank.walkToBank();
            sleepUntil(() -> Rs2Bank.isNearBank(10), 30000);
            return;
        }

        // Step 2: Find and interact with banker NPC (NOT bank booth - must be NPC for unnoting)
        // Common banker names: "Banker", "Bank assistant", "Banker tutor"
        if (!Rs2Widget.isWidgetVisible(14352385)) { // Unnote dialog not open yet
            // Use noted looting bag on banker
            if (!Rs2Inventory.isItemSelected()) {
                Microbot.log("[WildernessNicky] Selecting noted looting bag...");
                Rs2Inventory.use("Looting bag (noted)");
                sleep(600);
                return;
            } else {
                // Item selected, now click banker
                Microbot.log("[WildernessNicky] Using noted bag on banker...");
                // Try common banker NPCs
                if (Rs2Npc.interact("Banker", "Use")) {
                    sleep(1200); // Wait for dialog
                } else if (Rs2Npc.interact("Bank assistant", "Use")) {
                    sleep(1200);
                } else {
                    Microbot.log("[WildernessNicky] ⚠️ No banker found - will retry");
                }
                return;
            }
        }

        // Step 3: Dialog should be open - select "Exchange All"
        if (Rs2Widget.isWidgetVisible(14352385)) {
            Microbot.log("[WildernessNicky] Selecting 'Exchange All' (pressing 1)");
            Rs2Keyboard.keyPress('1'); // Option 1 = "Exchange All"
            sleep(1200); // Wait for unnoting to complete

            // Verify unnoting worked
            if (Rs2Inventory.hasItem(LOOTING_BAG_CLOSED_ID)) {
                Microbot.log("[WildernessNicky] ✅ Successfully unnoted looting bag!");

                // Step 4: Auto-open the looting bag
                Microbot.log("[WildernessNicky] Opening looting bag...");
                Rs2Inventory.interact(LOOTING_BAG_CLOSED_ID, "Open");
                sleepUntil(() -> Rs2Inventory.hasItem(LOOTING_BAG_OPEN_ID), 2000);

                if (Rs2Inventory.hasItem(LOOTING_BAG_OPEN_ID)) {
                    Microbot.log("[WildernessNicky] ✅ Looting bag is now open!");
                } else {
                    Microbot.log("[WildernessNicky] ⚠️ Failed to open looting bag - user will need to open manually");
                }

                // Continue with normal startup banking flow
                currentState = ObstacleState.STARTUP_BANKING;
            } else {
                Microbot.log("[WildernessNicky] ⚠️ Unnoting failed - will retry");
                // Stay in this state to retry
            }
        }
    }

    /**
     * Helper method to check if player is near Grand Exchange
     */
    private boolean isNearGrandExchange() {
        WorldPoint geLocation = new WorldPoint(3164, 3487, 0); // GE center
        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation == null) return false;
        return playerLocation.distanceTo(geLocation) < 20;
    }

    private void handlePostBankConfig() {
        forceBankNextLoot = false;

        // MASS MODE: Hop to configured mass world if set
        if (config.playMode() == WildernessNickyConfig.PlayMode.MASS) {
            int targetMassWorld = -1;

            // Try custom world list first
            if (!parsedMassWorlds.isEmpty()) {
                targetMassWorld = parsedMassWorlds.get(0); // Always use first world for mass mode consistency
            } else {
                // Use text box world number
                targetMassWorld = getMassWorldFromConfig();
            }

            if (targetMassWorld > 0) {
                int currentWorld = Rs2Player.getWorld();

                if (currentWorld != targetMassWorld) {
                    Microbot.log("[WildernessNicky] 🌍 MASS MODE: Hopping to configured mass world " + targetMassWorld);
                    if (!attemptWorldHop(targetMassWorld, "mass mode world")) {
                        return; // Stay in this state to retry
                    }
                    Microbot.log("[WildernessNicky] ✅ Successfully hopped to mass world " + targetMassWorld);
                } else {
                    Microbot.log("[WildernessNicky] 📌 Already on mass world " + targetMassWorld);
                }
            }
        }
        // NORMAL MODE / SOLO MODE: Swap back to original world if enabled
        else if (config.swapBack() && config.enableWorldHop() && originalWorld > 0 && Rs2Player.getWorld() != originalWorld) {
            if (!attemptWorldHop(originalWorld, "post-bank config")) {
                return; // Stay in this state to retry
            }
        } else if (config.swapBack() && !config.enableWorldHop()) {
            // Swap back enabled but world hop disabled - log warning once
            Microbot.log("[WildernessNicky] ⚠️ Swap back enabled but world hopping disabled - skipping swap back");
        }

        // Only join FC if not already in it
        if (config.joinFc() && !isInFriendChat()) {
            joinFriendChat();
        }
        // Force disable startAtCourse after banking
        forceStartAtCourse = false;
        currentState = ObstacleState.WALK_TO_COURSE;
    }

    private void handleWalkToCourse() {
        // Check looting bag on startup if present (for when returning from bank)
        // DISABLED: This corrupts inventory action data and causes Rs2Inventory.use() to crash
        // checkLootingBagOnStartup();

        if (!isAt(START_POINT, 2)) {
            Rs2Walker.walkTo(START_POINT, 2);
            sleepUntil(() -> isAt(START_POINT, 2), 20000);
            return;
        }

        // ===== ENTRANCE FEE PAYMENT WITH ALREADY-PAID CHECK =====
        TileObject dispenserObj = getDispenserObj();
        if (dispenserObj != null && !entranceFeePaid) {
            int coinCount = Rs2Inventory.itemQuantity(COINS_ID);
            if (coinCount >= 150000) {
                Microbot.log("[WildernessNicky] [WALK_TO_COURSE] Attempting to deposit 150k entrance fee into dispenser");

                // Click coins first
                Rs2Inventory.interact(COINS_ID, "Use");
                sleep(600, 800);

                // Then click dispenser with "Use" action
                boolean interacted = Rs2GameObject.interact(dispenserObj, "Use");
                if (interacted) {
                    Microbot.log("[WildernessNicky] Successfully clicked dispenser with coins");
                    sleep(1200); // Wait for animation/interaction

                    // Wait for payment to complete (coins decrease)
                    boolean paymentSuccess = sleepUntil(() -> Rs2Inventory.itemQuantity(COINS_ID) < coinCount, 5000);

                    if (paymentSuccess) {
                        int coinsAfter = Rs2Inventory.itemQuantity(COINS_ID);
                        int coinsSpent = coinCount - coinsAfter;
                        entranceFeePaid = true;
                        Microbot.log("[WildernessNicky] ✅ Entrance fee paid successfully! (Spent: " + coinsSpent + " gp)");
                    } else {
                        // Payment didn't go through - stay in this state to retry
                        Microbot.log("[WildernessNicky] ⚠️ Entrance fee payment failed (coins didn't decrease) - retrying...");
                        return; // Stay in WALK_TO_COURSE state to retry
                    }
                } else {
                    Microbot.log("[WildernessNicky] ⚠️ Failed to interact with dispenser - retrying...");
                    return; // Stay in WALK_TO_COURSE state to retry
                }
            } else {
                // Not enough coins - need to go bank/regear
                Microbot.log("[WildernessNicky] ⚠️ Not enough coins for entrance fee (" + coinCount + " < 150000)");
                Microbot.log("[WildernessNicky] Going to Mage Bank to regear with starting setup...");
                currentState = ObstacleState.BANKING;
                return;
            }
        } else if (entranceFeePaid) {
            Microbot.log("[WildernessNicky] [WALK_TO_COURSE] Entrance fee already paid - skipping payment");
        } else {
            Microbot.log("[WildernessNicky] [WALK_TO_COURSE] Dispenser object not found - retrying...");
            return; // Stay in this state to retry finding dispenser
        }
        currentState = ObstacleState.PIPE;
    }

    private void handleBanking() {
        Microbot.log("[WildernessNicky] 🏦 handleBanking() - Attempting to open bank...");

        // Single-step banking logic
        if (!Rs2Bank.isOpen()) {
            WorldPoint playerLoc = Rs2Player.getWorldLocation();

            // Check if we're in Mage Bank (underground plane 0)
            boolean inMageBank = playerLoc != null && playerLoc.getPlane() == 0 &&
                                playerLoc.getX() >= 2530 && playerLoc.getX() <= 2540 &&
                                playerLoc.getY() >= 4708 && playerLoc.getY() <= 4718;

            if (inMageBank) {
                // Mage Bank uses a bank chest, not a booth
                Microbot.log("[WildernessNicky] In Mage Bank - attempting to open bank...");

                // Rs2Bank.openBank() should find any nearby bank object (chest, booth, etc.)
                Rs2Bank.openBank();
                sleepUntil(Rs2Bank::isOpen, 10000);

                if (!Rs2Bank.isOpen()) {
                    Microbot.log("[WildernessNicky] ⚠️ Failed to open Mage Bank chest, retrying...");
                    return;
                }

                Microbot.log("[WildernessNicky] ✅ Mage Bank opened successfully!");
            } else {
                // Regular bank (use normal banking)
                if (!Rs2Bank.walkToBank()) {
                    Microbot.log("[WildernessNicky] Walking to bank...");
                    sleep(1000);
                    return;
                }

                Microbot.log("[WildernessNicky] Near bank, attempting to open...");
                Rs2Bank.openBank();
                sleepUntil(Rs2Bank::isOpen, 20000);

                if (!Rs2Bank.isOpen()) {
                    Microbot.log("[WildernessNicky] ⚠️ Failed to open bank, will retry...");
                    return;
                }

                Microbot.log("[WildernessNicky] ✅ Bank opened successfully!");
            }
        }

        // Reset entrance fee flag when banking (starting new session)
        if (entranceFeePaid) {
            Microbot.log("[WildernessNicky] Resetting entrance fee flag (banking indicates new session)");
            entranceFeePaid = false;
        }

        // Player Monitor not used - no need to disable


        // ===== BANKED LOOT TRACKING =====
        // Track loot from inventory and looting bag before banking
        trackBankedLoot();

        // Check if we have an open looting bag and deposit its contents first
        if (Rs2Inventory.hasItem(LOOTING_BAG_OPEN_ID)) {
            Microbot.log("[WildernessNicky] Depositing looting bag contents (value: " + NumberFormat.getIntegerInstance().format(lootingBagValue) + "gp)");
            Rs2Bank.depositLootingBag();
            sleep(getActionDelay());

            // Reset looting bag value and contents after deposit
            lootingBagValue = 0;
            lootingBagContents.clear();
            Microbot.log("[WildernessNicky] Looting bag value and contents reset after deposit");
        }

        // Deposit all
        Rs2Bank.depositAll();
        sleep(getActionDelay());

        // Increment banking trip counter
        totalBankingTrips++;
        Microbot.log("[WildernessNicky] Banking trip #" + totalBankingTrips + " completed. Total banked value: " + NumberFormat.getIntegerInstance().format(totalBankedValue) + "gp");

        // Withdraw Looting Bag (if enabled) - ORDER 1
        if (config.withdrawLootingBag() && !Rs2Inventory.hasItem(LOOTING_BAG_CLOSED_ID) && !Rs2Inventory.hasItem(LOOTING_BAG_OPEN_ID)) {
            // Try to withdraw closed bag first
            Rs2Bank.withdrawOne(LOOTING_BAG_CLOSED_ID);
            boolean closedSuccess = sleepUntil(() -> Rs2Inventory.hasItem(LOOTING_BAG_CLOSED_ID), 3000);
            
            if (closedSuccess) {
                needsLootingBagActivation = true; // Mark that we need to activate it after bank closes
                Microbot.log("[WildernessNicky] Successfully withdrew closed looting bag");
            } else {
                // If closed bag withdrawal failed, try open bag
                Microbot.log("[WildernessNicky] Closed looting bag not available, trying open version");
                Rs2Bank.withdrawOne(LOOTING_BAG_OPEN_ID);
                boolean openSuccess = sleepUntil(() -> Rs2Inventory.hasItem(LOOTING_BAG_OPEN_ID), 3000);

                if (openSuccess) {
                    Microbot.log("[WildernessNicky] Successfully withdrew open looting bag");
                    needsLootingBagActivation = false; // No need to activate, already open
                } else {
                    // Check if looting bag exists in bank at all
                    boolean lootingBagInBank = Rs2Bank.hasBankItem("Looting bag") || Rs2Bank.hasBankItem("Open looting bag");

                    if (!lootingBagInBank) {
                        Microbot.log("[WildernessNicky] ⚠️ No looting bag in bank! Starting GE buying phase...");

                        // Withdraw Ring of Wealth for GE teleport
                        if (!Rs2Inventory.hasItem("Ring of wealth")) {
                            Microbot.log("[WildernessNicky] Withdrawing Ring of Wealth for GE teleport");
                            Rs2Bank.withdrawOne("Ring of wealth");
                            sleepUntil(() -> Rs2Inventory.hasItem("Ring of wealth"), 3000);
                        }

                        Rs2Bank.closeBank();
                        sleep(600);

                        // Transition to looting bag buying state
                        needsToBuyNotedLootingBag = true;
                        currentState = ObstacleState.GE_BUY_NOTED_LOOTING_BAG;
                        Microbot.log("[WildernessNicky] State changed to GE_BUY_NOTED_LOOTING_BAG");
                        return;
                    } else {
                        Microbot.log("[WildernessNicky] Failed to withdraw looting bag, but it exists in bank - retrying");
                    }
                }
            }
        }
        
        // Withdraw Knife (if enabled) - ORDER 2
        if (config.withdrawKnife() && !Rs2Inventory.hasItem(KNIFE_ID)) {
            Rs2Bank.withdrawOne(KNIFE_ID);
            sleepUntil(() -> Rs2Inventory.hasItem(KNIFE_ID), 3000);
        }
        
        // Withdraw Venom Protection (if enabled) - ORDER 3
        if (config.withdrawVenomProtection() != WildernessNickyConfig.VenomProtectionOption.None) {
            int venomItemId = config.withdrawVenomProtection().getItemId();
            if (venomItemId != -1 && !Rs2Inventory.hasItem(venomItemId)) {
                Rs2Bank.withdrawOne(venomItemId);
                sleepUntil(() -> Rs2Inventory.hasItem(venomItemId), 3000);
            }
        }
        
        // Withdraw Coins (if enabled) - ORDER 4
        if (config.withdrawCoins() && (!Rs2Inventory.hasItem(COINS_ID) || Rs2Inventory.itemQuantity(COINS_ID) < 150000)) {
            Rs2Bank.withdrawX(COINS_ID, 150000);
            sleepUntil(() -> Rs2Inventory.hasItem(COINS_ID) && Rs2Inventory.itemQuantity(COINS_ID) >= 150000, 3000);
        }
        
        // Withdraw Phoenix Necklace (if enabled) - ORDER 5
        if (config.phoenixEscape() && !Rs2Inventory.hasItem(PHOENIX_NECKLACE_ID) && !Rs2Equipment.isWearing("Phoenix necklace")) {
            Microbot.log("[WildernessNicky] Withdrawing Phoenix necklace...");
            Rs2Bank.withdrawOne(PHOENIX_NECKLACE_ID);
            sleepUntil(() -> Rs2Inventory.hasItem(PHOENIX_NECKLACE_ID), 3000);
        }

        // Withdraw Ice Plateau TP (if enabled) - ORDER 6
        if (config.useIcePlateauTp() && !Rs2Inventory.hasItem(TELEPORT_ID)) {
            Rs2Bank.withdrawOne(TELEPORT_ID);
            sleepUntil(() -> Rs2Inventory.hasItem(TELEPORT_ID), 3000);
        }

        // Confirm all items are present
        boolean venomPresent = config.withdrawVenomProtection() == WildernessNickyConfig.VenomProtectionOption.None ||
            (config.withdrawVenomProtection().getItemId() != -1 && Rs2Inventory.hasItem(config.withdrawVenomProtection().getItemId()));

        boolean phoenixPresent = !config.phoenixEscape() || Rs2Inventory.hasItem(PHOENIX_NECKLACE_ID) || Rs2Equipment.isWearing("Phoenix necklace");

        boolean allPresent = (!config.withdrawKnife() || Rs2Inventory.hasItem(KNIFE_ID))
            && (!config.withdrawCoins() || Rs2Inventory.hasItem(COINS_ID))
            && (!config.useIcePlateauTp() || Rs2Inventory.hasItem(TELEPORT_ID))
            && (!config.withdrawLootingBag() || Rs2Inventory.hasItem(LOOTING_BAG_CLOSED_ID) || Rs2Inventory.hasItem(LOOTING_BAG_OPEN_ID))
            && venomPresent
            && phoenixPresent;

        if (!allPresent) {
            Microbot.log("[WildernessNicky] ⚠️ Not all required items present:");
            if (config.withdrawKnife() && !Rs2Inventory.hasItem(KNIFE_ID)) {
                Microbot.log("  ❌ Missing: Knife");
            }
            if (config.withdrawCoins() && !Rs2Inventory.hasItem(COINS_ID)) {
                Microbot.log("  ❌ Missing: Coins (need 150k)");
            }
            if (config.useIcePlateauTp() && !Rs2Inventory.hasItem(TELEPORT_ID)) {
                Microbot.log("  ❌ Missing: Ice Plateau Teleport");
            }
            if (config.withdrawLootingBag() && !Rs2Inventory.hasItem(LOOTING_BAG_CLOSED_ID) && !Rs2Inventory.hasItem(LOOTING_BAG_OPEN_ID)) {
                Microbot.log("  ❌ Missing: Looting Bag");
            }
            if (!venomPresent) {
                Microbot.log("  ❌ Missing: Venom Protection");
            }
            if (!phoenixPresent) {
                Microbot.log("  ❌ Missing: Phoenix Necklace");
            }
            Microbot.log("[WildernessNicky] Retrying item withdrawal...");
            return;
        }

        Rs2Bank.closeBank();
        sleep(getActionDelay());

        // Activate looting bag if needed (closed -> open)
        if (needsLootingBagActivation && Rs2Inventory.hasItem(LOOTING_BAG_CLOSED_ID)) {
            Microbot.log("[WildernessNicky] Activating looting bag");
            Rs2Inventory.interact(LOOTING_BAG_CLOSED_ID, "Open");
            sleep(getActionDelay());
            needsLootingBagActivation = false;
        }

        // Continue to next state
        currentState = ObstacleState.POST_BANK_CONFIG;
    }

    private void attemptLogoutUntilLoggedOut() {
        int maxAttempts = 30; // Try for up to 30 seconds
        int attempts = 0;
        while (!"LOGIN_SCREEN".equals(Microbot.getClient().getGameState().toString()) && attempts < maxAttempts) {
            Rs2Player.logout();
            sleep(1000); // Wait 1 second before trying again
            attempts++;
        }
    }

    private boolean hasTimedOutSince(long startTime, int threshold) {
        return System.currentTimeMillis() - startTime > threshold;
    }

    /**
     * Determines if we should drop items after dispenser based on config
     */
    private boolean shouldDropAfterDispenserNow() {
        WildernessNickyConfig.DropLocationOption dropLocation = config.dropLocation();
        
        switch (dropLocation) {
            case AfterDispenser:
                return true;
            case BeforeLog:
                return false;
            case Random:
                // Use the pre-determined random choice for this lap
                return shouldDropAfterDispenser;
            default:
                return false; // Default to before log (current behavior)
        }
    }
    
    /**
     * Generates a new random choice for drop location (called at start of each lap)
     */
    private void generateRandomDropLocation() {
        if (config.dropLocation() == WildernessNickyConfig.DropLocationOption.Random) {
            shouldDropAfterDispenser = new Random().nextBoolean();
        }
    }

    private void clearInventoryIfNeeded() {
        int attempts = 0;
        int maxAttempts = 10; // Prevent infinite loops
        
        while (Rs2Inventory.items().count() >= config.maxInventorySize() && isRunning() && attempts < maxAttempts) {
            attempts++;
            boolean itemHandled = false;
            
            // Get current counts of each food type
            int anglerfishCount = Rs2Inventory.itemQuantity(FOOD_PRIMARY);
            int karambwanCount = Rs2Inventory.itemQuantity(FOOD_SECONDARY);
            int mantaRayCount = Rs2Inventory.itemQuantity(FOOD_TERTIARY);
            int restorePotCount = Rs2Inventory.itemQuantity(FOOD_DROP);
            
            // Check if we can eat/drop items while respecting maximum configurations
            // Priority order: Prayer potion (above max) → Food primary (above max) → Food secondary (above max) → Food tertiary (above max)
            if (restorePotCount > config.minimumRestorePot()) {
                Rs2Inventory.interact(FOOD_DROP, "Drop");
                waitForInventoryChanges(800);
                itemHandled = true;
            } else if (anglerfishCount > config.minimumAnglerfish()) {
                Rs2Inventory.interact(FOOD_PRIMARY, "Eat");
                waitForInventoryChanges(getActionDelay());
                itemHandled = true;
            } else if (karambwanCount > config.minimumKarambwan()) {
                Rs2Inventory.interact(FOOD_SECONDARY, "Eat");
                waitForInventoryChanges(getActionDelay());
                itemHandled = true;
            } else if (mantaRayCount > config.minimumMantaRay()) {
                Rs2Inventory.interact(FOOD_TERTIARY, "Eat");
                waitForInventoryChanges(getActionDelay());
                itemHandled = true;
            }
            
            if (!itemHandled) {
                // If no known food items can be eaten/dropped, try to drop any non-essential items
                if (Rs2Inventory.count() >= config.maxInventorySize()) {
                    // Drop any item that's not essential (not knife, teleport, coins, or tickets)
                    Rs2Inventory.items().filter(item -> 
                        item.getId() != KNIFE_ID && 
                        item.getId() != TELEPORT_ID && 
                        item.getId() != COINS_ID &&
                        item.getId() != TICKET_ITEM_ID
                    ).findFirst().ifPresent(item -> {
                        Rs2Inventory.interact(item, "Drop");
                        waitForInventoryChanges(800);
                    });
                }
                break;
            }
        }
        
        if (attempts >= maxAttempts) {
            Microbot.log("clearInventoryIfNeeded() reached max attempts, breaking to prevent infinite loop");
        }
    }
    public int getDispenserLoots() {
        return dispenserLoots;
    }

    public void setLastFcJoinMessageTime(long time) {
        this.lastFcJoinMessageTime = time;
    }

    /**
     * Triggers death handling from external sources (like chat message detection)
     */
    public void triggerDeathHandling() {
        deathDetected = true;
    }
    
    /**
     * Handles dispenser chat messages to track looting bag value and detect incomplete laps
     * Called from WildernessNickyPlugin.onChatMessage()
     */
    public void handleDispenserChatMessage(String message) {
        // Check for incomplete lap message first
        if (message.contains("You need to complete one full lap to receive a reward")) {
            incompleteLapDetected = true;
            Microbot.log("[WildernessNicky] ⚠️ INCOMPLETE LAP DETECTED - Need to complete full lap before dispenser works");
            Microbot.log("[WildernessNicky] Starting fresh lap from PIPE obstacle...");
            return;
        }

        Matcher matcher = WILDY_DISPENSER_REGEX.matcher(message);
        Matcher extraMatcher = WILDY_DISPENSER_EXTRA_REGEX.matcher(message);

        if (extraMatcher.matches()) {
            // Handle message with bonus item first (3 items total)
            wildyItems.setupWildernessItemsIfEmpty();
            int quantity1 = Integer.parseInt(extraMatcher.group(1));
            String itemName1 = extraMatcher.group(2);
            int quantity2 = Integer.parseInt(extraMatcher.group(3));
            String itemName2 = extraMatcher.group(4);
            String bonusItemName = extraMatcher.group(5);

            addLootingBagValue(quantity1, itemName1, quantity2, itemName2, 1, bonusItemName);
        } else if (matcher.matches()) {
            // Handle standard message (2 items)
            wildyItems.setupWildernessItemsIfEmpty();
            int quantity1 = Integer.parseInt(matcher.group(1));
            String itemName1 = matcher.group(2);
            int quantity2 = Integer.parseInt(matcher.group(3));
            String itemName2 = matcher.group(4);

            addLootingBagValue(quantity1, itemName1, quantity2, itemName2);
        }
    }
    
    /**
     * Adds items to looting bag tracking system (item-by-item tracking + value calculation).
     * Tracks both individual items and their total value for banking/escape decisions.
     *
     * @param qty1 Quantity of first item
     * @param item1 Name of first item (from dispenser message)
     * @param qty2 Quantity of second item
     * @param item2 Name of second item (from dispenser message)
     */
    private void addLootingBagValue(int qty1, String item1, int qty2, String item2) {
        // Track item 1
        addItemToLootingBag(item1, qty1);

        // Track item 2
        addItemToLootingBag(item2, qty2);

        // Calculate values for entrance fee validation
        int itemId1 = wildyItems.nameToItemId(item1);
        int itemId2 = wildyItems.nameToItemId(item2);
        int value1 = Microbot.getItemManager().getItemPrice(itemId1) * qty1;
        int value2 = Microbot.getItemManager().getItemPrice(itemId2) * qty2;
        int totalValue = value1 + value2;

        // CRITICAL: Detect zero-value loot (entrance fee not paid!)
        if (totalValue == 0 || (value1 == 0 && value2 == 0)) {
            Microbot.log("[WildernessNicky] ⚠️ ZERO VALUE DISPENSER LOOT DETECTED!");
            Microbot.log("[WildernessNicky] ⚠️ This means ENTRANCE FEE WAS NOT PAID!");
            Microbot.log("[WildernessNicky] Resetting entrance fee flag and going to bank to pay...");

            // Reset entrance fee flag
            entranceFeePaid = false;

            // Force banking to pay entrance fee
            currentState = ObstacleState.BANKING;
            return;
        }

        // Update total value
        lootingBagValue += totalValue;

        Microbot.log(String.format("[WildernessNicky] Looting bag: +%d %s, +%d %s (Total: %s gp)",
            qty1, item1, qty2, item2, NumberFormat.getIntegerInstance().format(lootingBagValue)));
    }
    
    /**
     * Adds items to looting bag tracking system (3 items - includes bonus item).
     *
     * @param qty1 Quantity of first item
     * @param item1 Name of first item
     * @param qty2 Quantity of second item
     * @param item2 Name of second item
     * @param qty3 Quantity of bonus item
     * @param item3 Name of bonus item
     */
    private void addLootingBagValue(int qty1, String item1, int qty2, String item2, int qty3, String item3) {
        // Track all items individually
        addItemToLootingBag(item1, qty1);
        addItemToLootingBag(item2, qty2);
        addItemToLootingBag(item3, qty3);

        // Calculate values for entrance fee validation
        int itemId1 = wildyItems.nameToItemId(item1);
        int itemId2 = wildyItems.nameToItemId(item2);
        int itemId3 = wildyItems.nameToItemId(item3);
        int value1 = Microbot.getItemManager().getItemPrice(itemId1) * qty1;
        int value2 = Microbot.getItemManager().getItemPrice(itemId2) * qty2;
        int value3 = Microbot.getItemManager().getItemPrice(itemId3) * qty3;
        int totalValue = value1 + value2 + value3;

        // CRITICAL: Detect zero-value loot (entrance fee not paid!)
        if (totalValue == 0 || (value1 == 0 && value2 == 0 && value3 == 0)) {
            Microbot.log("[WildernessNicky] ⚠️ ZERO VALUE DISPENSER LOOT DETECTED!");
            Microbot.log("[WildernessNicky] ⚠️ This means ENTRANCE FEE WAS NOT PAID!");
            Microbot.log("[WildernessNicky] Resetting entrance fee flag and going to bank to pay...");

            // Reset entrance fee flag
            entranceFeePaid = false;

            // Force banking to pay entrance fee
            currentState = ObstacleState.BANKING;
            return;
        }

        // Update total value
        lootingBagValue += totalValue;

        Microbot.log(String.format("[WildernessNicky] Looting bag: +%d %s, +%d %s, +%d %s [BONUS] (Total: %s gp)",
            qty1, item1, qty2, item2, qty3, item3, NumberFormat.getIntegerInstance().format(lootingBagValue)));
    }

    /**
     * Helper method to add an item to the looting bag contents tracker.
     * Maintains a running tally of all items collected.
     *
     * @param itemName Name of the item
     * @param quantity Quantity to add
     */
    private void addItemToLootingBag(String itemName, int quantity) {
        if (itemName == null || itemName.trim().isEmpty()) {
            return;
        }

        // Add to running tally
        lootingBagContents.merge(itemName, quantity, Integer::sum);

        Microbot.log(String.format("[WildernessNicky] 📦 Looting bag now contains: %d x %s",
            lootingBagContents.get(itemName), itemName));
    }

    /**
     * Checks if player has Phoenix necklace in inventory or equipped
     */
    private boolean hasPhoenixNecklace() {
        // Check if wearing Phoenix necklace
        if (Rs2Equipment.isWearing("Phoenix necklace")) {
            return true;
        }
        
        // Check if Phoenix necklace is in inventory
        return Rs2Inventory.hasItem(PHOENIX_NECKLACE_ID);
    }

    /**
     * Public method to trigger Phoenix Escape from external sources
     */
    public void triggerPhoenixEscapeExternal() {
        if (config.phoenixEscape()) {
            triggerPhoenixEscape("External trigger (manual/other plugin)");
        }
    }
    
    /**
     * Handles ItemContainerChanged events to sync looting bag value
     * Called from WildernessNickyPlugin
     */
    public void handleItemContainerChanged(net.runelite.api.events.ItemContainerChanged event) {
        // ALWAYS track looting bag container changes in real-time
        if (event.getContainerId() == LOOTING_BAG_CONTAINER_ID) {
            syncLootingBagFromContainer(event.getItemContainer());
        }
    }

    /**
     * Tracks incoming projectiles for projectile-based prayer switching
     * Called by WildernessNickyPlugin when a projectile is detected
     * @param hitCycle The game cycle when the projectile will hit
     * @param projectile The projectile object
     */
    public void trackIncomingProjectile(int hitCycle, Projectile projectile) {
        try {
            if (projectile == null) return;

            // Store projectile by hit cycle
            incomingProjectiles.put(hitCycle, projectile);

            // Update statistics
            lastProjectileId = projectile.getId();
            lastProjectileDetectionTime = System.currentTimeMillis();
            projectilesDetectedCount++;

            // Log projectile detection (only if config enabled for projectile switching)
            if (config != null && config.useProjectilePrayerSwitching()) {
                String attackStyle = WildernessProjectileType.getAttackStyleName(projectile.getId());
                Microbot.log(String.format("[WildernessNicky] 🎯 Projectile detected: ID=%d, Style=%s, Cycles until impact=%d",
                    projectile.getId(),
                    attackStyle,
                    (hitCycle - Microbot.getClient().getGameCycle()) / 30
                ));
            }
        } catch (Exception e) {
            Microbot.log("[WildernessNicky] Error tracking projectile: " + e.getMessage());
        }
    }

    // Looting bag sync tracking
    private int lastLootingBagSyncLap = 0;
    private int nextLootingBagSyncInterval = 0;

    /**
     * Determines if we should sync looting bag this lap (randomized 3-7 laps)
     */
    private boolean shouldSyncLootingBag() {
        // Initialize random interval on first check
        if (nextLootingBagSyncInterval == 0) {
            nextLootingBagSyncInterval = 3 + new java.util.Random().nextInt(5); // 3-7 laps
        }

        // Check if enough laps have passed
        if (dispenserLoots - lastLootingBagSyncLap >= nextLootingBagSyncInterval) {
            lastLootingBagSyncLap = dispenserLoots;
            // Generate new random interval for next sync
            nextLootingBagSyncInterval = 3 + new java.util.Random().nextInt(5); // 3-7 laps
            return true;
        }
        return false;
    }

    /**
     * Checks looting bag to trigger container sync for GUI display
     * Uses random intervals (3-7 laps) to avoid predictable patterns
     */
    private void openLootingBagForSync() {
        try {
            // Check if we have a looting bag (closed or open)
            if (!Rs2Inventory.hasItem(LOOTING_BAG_CLOSED_ID) && !Rs2Inventory.hasItem(LOOTING_BAG_OPEN_ID)) {
                Microbot.log("[WildernessNicky] No looting bag found for sync");
                return;
            }

            // Use "Check" option on the looting bag (triggers container sync without opening interface)
            // Item ID 22586 = Open looting bag
            if (Rs2Inventory.hasItem(LOOTING_BAG_OPEN_ID)) {
                Rs2Inventory.interact(LOOTING_BAG_OPEN_ID, "Check");
                sleep(600); // Wait 1 game tick for container data
            } else if (Rs2Inventory.hasItem(LOOTING_BAG_CLOSED_ID)) {
                // If closed, just interact with it (will auto-check)
                Rs2Inventory.interact(LOOTING_BAG_CLOSED_ID, "Check");
                sleep(600);
            }

            Microbot.log("[WildernessNicky] ✅ Looting bag synced for GUI (next sync in " + nextLootingBagSyncInterval + " laps)");
        } catch (Exception e) {
            Microbot.log("[WildernessNicky] Error syncing looting bag: " + e.getMessage());
        }
    }

    /**
     * Syncs looting bag value from the ItemContainer (real-time tracking)
     * This is called whenever the looting bag container changes
     */
    private void syncLootingBagFromContainer(net.runelite.api.ItemContainer container) {
        if (container == null) {
            Microbot.log("[WildernessNicky] Looting bag is empty (container null)");
            lootingBagValue = 0;
            lootingBagContents.clear();
            waitingForLootingBagSync = false;
            return;
        }

        wildyItems.setupWildernessItemsIfEmpty();

        // Clear previous contents
        lootingBagContents.clear();

        // Calculate value from container items and track contents
        int totalValue = 0;
        for (net.runelite.api.Item item : container.getItems()) {
            if (item.getId() > 0) { // Valid item
                int itemValue = Microbot.getItemManager().getItemPrice(item.getId()) * item.getQuantity();
                totalValue += itemValue;

                // Track item for GUI display
                String itemName = Microbot.getItemManager().getItemComposition(item.getId()).getName();
                lootingBagContents.put(itemName, item.getQuantity());
            }
        }

        lootingBagValue = totalValue;
        waitingForLootingBagSync = false;

        Microbot.log("[WildernessNicky] 📦 Looting bag updated: " +
            java.text.NumberFormat.getIntegerInstance().format(lootingBagValue) + "gp, " +
            lootingBagContents.size() + " item types");
    }
    
    /**
     * Checks the looting bag to sync initial value on startup
     * Should be called from INIT, START, or WALK_TO_COURSE states
     */
    public void checkLootingBagOnStartup() {
        // Only check once per script run
        if (hasCheckedLootingBagOnStartup) {
            return;
        }
        
        if (!Rs2Inventory.hasItem(LOOTING_BAG_OPEN_ID)) {
            hasCheckedLootingBagOnStartup = true; // Mark as checked even if no bag
            return; // No open looting bag to check
        }
        
        Microbot.log("[WildernessNicky] Checking looting bag for initial sync...");
        waitingForLootingBagSync = true;
        hasCheckedLootingBagOnStartup = true;
        
        try {
            // Right-click looting bag and select "Check"
            Rs2Inventory.interact(LOOTING_BAG_OPEN_ID, "Check");
            
            // Wait for container to load
            sleepUntil(() -> !waitingForLootingBagSync, 3000);
            
            // Close the looting bag interface
            Rs2Keyboard.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
            sleep(300); // Wait for interface to close
            
            // Ensure we're on the inventory tab
            Rs2Tab.switchTo(InterfaceTab.INVENTORY);
            sleep(200);
            
            Microbot.log("[WildernessNicky] Looting bag interface closed, inventory refreshed");
        } catch (NullPointerException e) {
            // Known issue: Items with null action data in inventory can cause Rs2Inventory operations to crash
            Microbot.log("[WildernessNicky] Inventory error during looting bag check - will retry next cycle");
            waitingForLootingBagSync = false;
            hasCheckedLootingBagOnStartup = false; // Allow retry

            // Try to close any open interface just in case
            try {
                Rs2Keyboard.keyPress(java.awt.event.KeyEvent.VK_ESCAPE);
                sleep(300);
            } catch (Exception ignored) {}
        }
    }

    // ===========================================================================================
    // ADVANCED WILDERNESS COMBAT SYSTEMS
    // ===========================================================================================

    /**
     * Updates the current wilderness level
     */
    private void updateWildernessLevel() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastWildernessLevelCheck < WILDERNESS_LEVEL_CHECK_INTERVAL) {
            return;
        }
        lastWildernessLevelCheck = currentTime;

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        if (playerLocation != null) {
            currentWildernessLevel = Rs2Pvp.getWildernessLevelFrom(playerLocation);
        }
    }

    /**
     * Updates teleblock status
     */
    private void updateTeleBlockStatus() {
        isTeleBlocked = Rs2Player.isTeleBlocked();
        if (isTeleBlocked && teleBlockDetectedTime == 0) {
            teleBlockDetectedTime = System.currentTimeMillis();
            Microbot.log("[WildernessNicky] ⚠️ TELEBLOCK DETECTED - Escape options limited!");
        } else if (!isTeleBlocked && teleBlockDetectedTime != 0) {
            Microbot.log("[WildernessNicky] ✅ Teleblock worn off");
            teleBlockDetectedTime = 0;
        }
    }

    /**
     * PROJECTILE-BASED PRAYER SWITCHING SYSTEM
     * Automatically switches to the correct protection prayer based on incoming projectiles
     * This is MORE ACCURATE than weapon-based switching as it detects the actual attack
     */
    private void handleProjectilePrayerSwitching() {
        try {
            // Don't process if not enabled
            if (config == null || !config.useProjectilePrayerSwitching()) {
                return;
            }

            // Get current game cycle
            int currentCycle = Microbot.getClient().getGameCycle();

            // Remove expired projectiles (already hit)
            incomingProjectiles.entrySet().removeIf(entry -> entry.getKey() < currentCycle);

            // Find the next projectile that will hit
            Map.Entry<Integer, Projectile> nextProjectile = incomingProjectiles.entrySet().stream()
                .min(Comparator.comparingInt(Map.Entry::getKey))
                .orElse(null);

            if (nextProjectile == null) {
                // No incoming projectiles - check if we should disable prayers after timeout
                long timeSinceLastProjectile = System.currentTimeMillis() - lastProjectileDetectionTime;
                if (timeSinceLastProjectile > COMBAT_TIMEOUT && activeCombatPrayer != null) {
                    Microbot.log("[WildernessNicky] 🛡️ No projectiles for " + (COMBAT_TIMEOUT/1000) + "s - disabling protection prayers");
                    disableAllProtectionPrayers();
                    activeCombatPrayer = null;
                }
                return;
            }

            // Calculate ticks until impact
            int hitCycle = nextProjectile.getKey();
            int ticksUntilImpact = (hitCycle - currentCycle) / 30;

            // Switch prayer when projectile is 1 tick away (for 1-tick accuracy)
            if (ticksUntilImpact <= 1) {
                Projectile projectile = nextProjectile.getValue();
                int projectileId = projectile.getId();

                // Get the required prayer for this projectile
                Rs2PrayerEnum requiredPrayer = WildernessProjectileType.getPrayerForProjectile(projectileId);

                // ALWAYS ensure protection prayer is active for incoming attack
                if (requiredPrayer != null) {
                    // Check if we need to switch OR re-enable prayer
                    boolean needsSwitch = (requiredPrayer != activeCombatPrayer);
                    boolean prayerNotActive = !Rs2Prayer.isPrayerActive(requiredPrayer);

                    if (needsSwitch || prayerNotActive) {
                        String attackStyle = WildernessProjectileType.getAttackStyleName(projectileId);
                        Microbot.log(String.format("[WildernessNicky] ⚡ %s PRAYER: %s attack incoming! (Projectile ID: %d)",
                            needsSwitch ? "SWITCHING" : "ACTIVATING", attackStyle, projectileId));

                        switchProtectionPrayer(requiredPrayer);
                        lastCombatActionTime = System.currentTimeMillis();
                    }
                }

                // Remove this projectile from tracking (it's about to hit)
                incomingProjectiles.remove(hitCycle);
            }

        } catch (Exception e) {
            Microbot.log("[WildernessNicky] Error in projectile prayer switching: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * MELEE ATTACK DETECTION (Fallback for attacks without projectiles)
     * Detects when player is being attacked in melee range and activates Protect from Melee
     * Only runs when no projectiles are being tracked
     */
    private void handleMeleeDetection() {
        try {
            Player localPlayer = Microbot.getClient().getLocalPlayer();
            if (localPlayer == null) return;

            Actor interacting = localPlayer.getInteracting();

            // Check if being attacked by a player or NPC
            if (interacting != null) {
                // Get distance to attacker
                WorldPoint attackerPos = interacting.getWorldLocation();
                WorldPoint playerPos = localPlayer.getWorldLocation();

                if (attackerPos != null && playerPos != null) {
                    int distance = attackerPos.distanceTo(playerPos);

                    // If attacker is in melee range (1-2 tiles) and no projectiles detected
                    // Assume melee attack and protect
                    if (distance <= 2) {
                        // Only switch if not already protecting from melee
                        if (activeCombatPrayer != Rs2PrayerEnum.PROTECT_MELEE) {
                            Microbot.log("[WildernessNicky] 🗡️ Melee range attack detected - activating Protect from Melee");
                            switchProtectionPrayer(Rs2PrayerEnum.PROTECT_MELEE);
                            lastCombatActionTime = System.currentTimeMillis();
                        }
                    }
                }
            } else {
                // No attacker - disable prayers after timeout
                long timeSinceLastCombat = System.currentTimeMillis() - lastCombatActionTime;
                if (timeSinceLastCombat > COMBAT_TIMEOUT && activeCombatPrayer != null) {
                    Microbot.log("[WildernessNicky] 🛡️ No combat for " + (COMBAT_TIMEOUT/1000) + "s - disabling protection prayers");
                    disableAllProtectionPrayers();
                    activeCombatPrayer = null;
                }
            }
        } catch (Exception e) {
            Microbot.log("[WildernessNicky] Error in melee detection: " + e.getMessage());
        }
    }

    /**
     * 1-TICK PRAYER SWITCHING SYSTEM (WEAPON-BASED - LEGACY)
     * Automatically switches to the correct protection prayer based on attacker's weapon
     * NOTE: Projectile-based switching is more accurate and should be preferred
     */
    private void handle1TickPrayerSwitching() {
        try {
            // Check if we're in combat
            Player localPlayer = Microbot.getClient().getLocalPlayer();
            if (localPlayer == null) return;

            Actor interacting = localPlayer.getInteracting();

            // If we have an attacker
            if (interacting instanceof Player) {
                Player attacker = (Player) interacting;
                currentAttacker = new Rs2PlayerModel(attacker);

                // Determine required prayer based on attacker's weapon
                Rs2PrayerEnum requiredPrayer = determineProtectionPrayer(currentAttacker);

                // Switch prayer if needed (with cooldown to prevent spam)
                long currentTime = System.currentTimeMillis();
                if (requiredPrayer != null && requiredPrayer != activeCombatPrayer) {
                    if (currentTime - lastPrayerSwitchTime >= PRAYER_SWITCH_COOLDOWN) {
                        switchProtectionPrayer(requiredPrayer);
                        lastPrayerSwitchTime = currentTime;
                        lastCombatActionTime = currentTime;
                    }
                }
            } else {
                // No longer in combat - disable protection prayers after timeout
                long timeSinceLastCombat = System.currentTimeMillis() - lastCombatActionTime;
                if (timeSinceLastCombat > COMBAT_TIMEOUT && activeCombatPrayer != null) {
                    disableAllProtectionPrayers();
                    activeCombatPrayer = null;
                    currentAttacker = null;
                }
            }
        } catch (Exception e) {
            Microbot.log("[WildernessNicky] Error in prayer switching: " + e.getMessage());
        }
    }

    /**
     * Determines which protection prayer to use based on attacker's equipped weapon
     */
    private Rs2PrayerEnum determineProtectionPrayer(Rs2PlayerModel attacker) {
        if (attacker == null) return null;

        try {
            // Get attacker's equipment
            Map<KitType, Integer> equipment = Rs2Player.getPlayerEquipmentIds(attacker);
            if (equipment == null || equipment.isEmpty()) return Rs2PrayerEnum.PROTECT_MELEE;

            int weaponId = equipment.getOrDefault(KitType.WEAPON, -1);

            // RANGED WEAPONS
            if (isRangedWeapon(weaponId)) {
                Microbot.log("[WildernessNicky] 🛡️ Detected ranged attack - switching to Protect from Missiles");
                return Rs2PrayerEnum.PROTECT_RANGE;
            }

            // MAGIC WEAPONS/STAFF
            if (isMagicWeapon(weaponId)) {
                Microbot.log("[WildernessNicky] 🛡️ Detected magic attack - switching to Protect from Magic");
                return Rs2PrayerEnum.PROTECT_MAGIC;
            }

            // DEFAULT TO MELEE (most common in wilderness)
            Microbot.log("[WildernessNicky] 🛡️ Detected melee attack - switching to Protect from Melee");
            return Rs2PrayerEnum.PROTECT_MELEE;

        } catch (Exception e) {
            Microbot.log("[WildernessNicky] Error determining prayer: " + e.getMessage());
            return Rs2PrayerEnum.PROTECT_MELEE; // Safe default
        }
    }

    /**
     * Checks if a weapon ID is a ranged weapon
     */
    private boolean isRangedWeapon(int weaponId) {
        // Bows
        if (weaponId >= 9185 && weaponId <= 9191) return true; // Dark bow
        if (weaponId == 11235) return true; // Dragon hunter crossbow
        if (weaponId == 25862 || weaponId == 25865) return true; // Webweaver bow
        if (weaponId == 25869) return true; // Venator bow
        if (weaponId == 11785) return true; // Armadyl crossbow
        if (weaponId == 21012) return true; // Twisted bow
        if (weaponId >= 4212 && weaponId <= 4223) return true; // Crystal bow

        // Crossbows
        if (weaponId >= 9174 && weaponId <= 9185) return true; // Various crossbows
        if (weaponId == 9185) return true; // Rune crossbow
        if (weaponId == 20997 || weaponId == 21002) return true; // Zaryte crossbow

        // Blowpipe and darts
        if (weaponId == 12926) return true; // Blowpipe

        // Throwing weapons
        if (weaponId >= 806 && weaponId <= 810) return true; // Knives
        if (weaponId >= 806 && weaponId <= 829) return true; // Throwing axes

        return false;
    }

    /**
     * Checks if a weapon ID is a magic weapon
     */
    private boolean isMagicWeapon(int weaponId) {
        // Staves
        if (weaponId >= 1381 && weaponId <= 1409) return true; // Basic staves
        if (weaponId >= 3053 && weaponId <= 3054) return true; // Zamorak/Guthix staff
        if (weaponId == 6914 || weaponId == 6916 ||  weaponId == 6918) return true; // God staves
        if (weaponId == 11791 || weaponId == 11787) return true; // Staff of the dead / Toxic staff
        if (weaponId == 21006) return true; // Kodai wand
        if (weaponId == 22323) return true; // Tumeken's shadow
        if (weaponId == 25731) return true; // Warped sceptre
        if (weaponId >= 4170 && weaponId <= 4178) return true; // Ancient staff variants

        // Powered staves
        if (weaponId == 11907) return true; // Trident of the seas
        if (weaponId == 12899) return true; // Trident of the swamp
        if (weaponId == 28585 || weaponId == 28583) return true; // Accursed/Corrupted sceptre

        // Wands
        if (weaponId == 28597) return true; // Iban's staff

        return false;
    }

    /**
     * Switches to the specified protection prayer.
     * Ensures the prayer is active and tracking variable is updated.
     */
    private void switchProtectionPrayer(Rs2PrayerEnum prayer) {
        try {
            // Disable current protection prayer if different
            if (activeCombatPrayer != null && activeCombatPrayer != prayer) {
                Rs2Prayer.toggle(activeCombatPrayer, false);
            }

            // Enable new protection prayer (if not already active)
            if (!Rs2Prayer.isPrayerActive(prayer)) {
                Rs2Prayer.toggle(prayer, true);
            }

            // ALWAYS update tracking variable to ensure consistency
            activeCombatPrayer = prayer;
        } catch (Exception e) {
            Microbot.log("[WildernessNicky] Error switching prayer: " + e.getMessage());
        }
    }

    /**
     * Disables all protection prayers
     */
    private void disableAllProtectionPrayers() {
        try {
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, false);
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, false);
            Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, false);
        } catch (Exception e) {
            Microbot.log("[WildernessNicky] Error disabling prayers: " + e.getMessage());
        }
    }

    /**
     * EQUIPMENT-BASED THREAT ASSESSMENT
     * Checks if a player has high-tier PvP gear equipped
     * High-tier gear indicates a serious PKer
     */
    private boolean hasHighTierGear(Rs2PlayerModel player) {
        if (player == null) return false;

        try {
            Map<KitType, Integer> equipment = Rs2Player.getPlayerEquipmentIds(player);
            if (equipment == null || equipment.isEmpty()) return false;

            // High-tier weapons
            int weaponId = equipment.getOrDefault(KitType.WEAPON, -1);

            // T-Bow, Zaryte, Shadow, etc.
            if (weaponId == 21012) return true; // Twisted bow
            if (weaponId == 20997 || weaponId == 21002) return true; // Zaryte crossbow
            if (weaponId == 22323) return true; // Tumeken's shadow
            if (weaponId == 25869) return true; // Venator bow
            if (weaponId == 11785) return true; // Armadyl crossbow
            if (weaponId == 13652) return true; // Dragon claws
            if (weaponId == 21003 || weaponId == 21006) return true; // Ancestral/Kodai

            // High-tier armor
            int bodyId = equipment.getOrDefault(KitType.TORSO, -1);
            int legsId = equipment.getOrDefault(KitType.LEGS, -1);

            // Ancestral
            if (bodyId == 21021 || legsId == 21024) return true;

            // Masori
            if (bodyId == 27235 || legsId == 27238) return true;

            // Torva/Pernix/Virtus
            if (bodyId == 26382 || bodyId == 26374 || bodyId == 26378) return true;
            if (legsId == 26384 || legsId == 26376 || legsId == 26380) return true;

            // Inquisitor
            if (bodyId == 24419 || legsId == 24422) return true;

            return false;
        } catch (Exception e) {
            return false; // If we can't check, assume not high-tier
        }
    }
}
