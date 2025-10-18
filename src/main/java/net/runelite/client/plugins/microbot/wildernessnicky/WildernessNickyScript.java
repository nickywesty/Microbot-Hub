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
import net.runelite.client.plugins.microbot.*;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.inventory.*;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.keyboard.Rs2Keyboard;
import net.runelite.client.plugins.microbot.util.tabs.Rs2Tab;
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
    public static final String VERSION = "1.6.0";

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
    private static final WorldPoint START_POINT = new WorldPoint(3004, 3936, 0);
    private static final WorldPoint DISPENSER_POINT = new WorldPoint(3004, 3936, 0);

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
        EMERGENCY_ESCAPE
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
    private int pvpHitCount = 0; // Track number of PvP hits
    private static final int MIN_PVP_HITS_TO_ESCAPE = 2; // Escape after 2 hits

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
            currentState = ObstacleState.START;
        }
        startTime = System.currentTimeMillis();
        Microbot.log("[WildernessNickyScript] startup called - grace period active for " + (STARTUP_GRACE_PERIOD/1000) + " seconds");
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!Microbot.isLoggedIn()) return;
                
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
                        triggerPhoenixEscape("Phoenix necklace missing from inventory/equipment");
                    }
                }

                // ===== WAIT FOR HIT MODE (MASS-FRIENDLY) =====
                // Only escape after being hit by a player multiple times (not agility fails)
                if (config.waitForHitBeforeEscape() && !emergencyEscapeTriggered && !gracePeriodActive) {
                    if (pvpHitCount >= MIN_PVP_HITS_TO_ESCAPE) {
                        triggerPhoenixEscape("Took " + pvpHitCount + " PvP hits - player attacking (Wait for Hit mode)");
                    }
                }

                // PROACTIVE PLAYER DETECTION - Scan for PKers before they attack (if wait for hit is disabled)
                if (config.enableProactivePlayerDetection() && !config.waitForHitBeforeEscape() && !emergencyEscapeTriggered) {
                    if (detectNearbyThreat()) {
                        triggerPhoenixEscape("Threatening player detected within 15 tiles (Proactive Detection)");
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

                // Handle prayer switching (if in wilderness and has prayer)
                if (Rs2Pvp.isInWilderness() && Rs2Player.hasPrayerPoints()) {
                    // Check if we should use projectile-based or weapon-based switching
                    long currentTime = System.currentTimeMillis();

                    // Projectile-based switching (check every 50ms for maximum accuracy)
                    if (config.useProjectilePrayerSwitching()) {
                        if (currentTime - lastProjectileCheckTime >= PROJECTILE_CHECK_INTERVAL) {
                            handleProjectilePrayerSwitching();
                            lastProjectileCheckTime = currentTime;
                        }
                    } else {
                        // Fallback to weapon-based switching (legacy mode)
                        handle1TickPrayerSwitching();
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
        hasCheckedLootingBagOnStartup = false;

        // Reset incomplete lap detection
        incompleteLapDetected = false;
        dispenserInteractAttempts = 0;

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

        // Walk to bank to re-gear
        Microbot.log("[WildernessNicky] Walking to bank to re-gear for wilderness");
        currentState = ObstacleState.BANKING;

        // Reset emergency flags since we died
        emergencyEscapeTriggered = false;
        phoenixEscapeTriggered = false;

        // Force re-gear by going through banking state
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
    private boolean isInUndergroundPit() {
        // Check for the underground object that only exists in the pit
        return Rs2GameObject.getAll(o -> o.getId() == UNDERGROUND_OBJECT_ID, 104).stream().findFirst().orElse(null) != null;
    }
    private void recoverFromPit() {
        // First check if we're still in the pit using game object detection
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
            Microbot.log("[WildernessNicky] Starting fresh lap - likely incomplete lap issue");
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
            // Enable Player Monitor when starting banking process if configured
            if (config.enablePlayerMonitor()) {
                try {
                    Plugin playerMonitor = Microbot.getPluginManager().getPlugins().stream()
                            .filter(x -> x.getClass().getName().equals("net.runelite.client.plugins.microbot.playermonitor.PlayerMonitorPlugin"))
                            .findFirst()
                            .orElse(null);
                    if (playerMonitor != null) {
                        Microbot.startPlugin(playerMonitor);
                        Microbot.log("[WildernessNicky] Player Monitor enabled for banking safety");
                    } else {
                        Microbot.log("[WildernessNicky] Player Monitor plugin not found - continuing without it");
                    }
                } catch (Exception e) {
                    Microbot.log("[WildernessNicky] Failed to start Player Monitor: " + e.getMessage());
                }
            }
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
            // Enable Player Monitor when starting banking process if configured
            if (config.enablePlayerMonitor()) {
                try {
                    Plugin playerMonitor = Microbot.getPluginManager().getPlugins().stream()
                            .filter(x -> x.getClass().getName().equals("net.runelite.client.plugins.microbot.playermonitor.PlayerMonitorPlugin"))
                            .findFirst()
                            .orElse(null);
                    if (playerMonitor != null) {
                        Microbot.startPlugin(playerMonitor);
                        Microbot.log("[WildernessNicky] Player Monitor enabled for banking safety");
                    } else {
                        Microbot.log("[WildernessNicky] Player Monitor plugin not found - continuing without it");
                    }
                } catch (Exception e) {
                    Microbot.log("[WildernessNicky] Failed to start Player Monitor: " + e.getMessage());
                }
            }
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
            // Enable Player Monitor when starting banking process if configured
            if (config.enablePlayerMonitor()) {
                try {
                    Plugin playerMonitor = Microbot.getPluginManager().getPlugins().stream()
                            .filter(x -> x.getClass().getName().equals("net.runelite.client.plugins.microbot.playermonitor.PlayerMonitorPlugin"))
                            .findFirst()
                            .orElse(null);
                    if (playerMonitor != null) {
                        Microbot.startPlugin(playerMonitor);
                        Microbot.log("[WildernessNicky] Player Monitor enabled for banking safety");
                    } else {
                        Microbot.log("[WildernessNicky] Player Monitor plugin not found - continuing without it");
                    }
                } catch (Exception e) {
                    Microbot.log("[WildernessNicky] Failed to start Player Monitor: " + e.getMessage());
                }
            }
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
                
                // Enable Player Monitor when starting banking process if configured
                if (config.enablePlayerMonitor()) {
                    try {
                        Plugin playerMonitor = Microbot.getPluginManager().getPlugins().stream()
                                .filter(x -> x.getClass().getName().equals("net.runelite.client.plugins.microbot.playermonitor.PlayerMonitorPlugin"))
                                .findFirst()
                                .orElse(null);
                        if (playerMonitor != null) {
                            Microbot.startPlugin(playerMonitor);
                            Microbot.log("[WildernessNicky] Player Monitor enabled for banking safety");
                        } else {
                            Microbot.log("[WildernessNicky] Player Monitor plugin not found - continuing without it");
                        }
                    } catch (Exception e) {
                        Microbot.log("[WildernessNicky] Failed to start Player Monitor: " + e.getMessage());
                    }
                }
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
                Microbot.log("[WildernessNicky] ⚔️ PvP Hit #" + pvpHitCount + " Detected! Health dropped " + healthDrop + "% (Combat: " + inPlayerCombat + ")");
            }
        }

        // Clear PvP damage flag and reset hit counter after timeout
        if (recentlyTookPvPDamage && currentTime - lastPvPDamageTime > PVP_DAMAGE_TIMEOUT) {
            recentlyTookPvPDamage = false;
            pvpHitCount = 0;
            Microbot.log("[WildernessNicky] PvP damage flag cleared (timeout) - hit counter reset");
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
        
        // Enable Player Monitor for safety
        if (config.enablePlayerMonitor()) {
            try {
                Plugin playerMonitor = Microbot.getPluginManager().getPlugins().stream()
                        .filter(x -> x.getClass().getName().equals("net.runelite.client.plugins.microbot.playermonitor.PlayerMonitorPlugin"))
                        .findFirst()
                        .orElse(null);
                if (playerMonitor != null) {
                    Microbot.startPlugin(playerMonitor);
                    Microbot.log("[WildernessNicky] Player Monitor enabled for Emergency Escape");
                } else {
                    Microbot.log("[WildernessNicky] Player Monitor plugin not found - continuing escape without it");
                }
            } catch (Exception e) {
                Microbot.log("[WildernessNicky] Failed to start Player Monitor for Emergency Escape: " + e.getMessage());
            }
        }
        
        // Set state to Emergency Escape (dedicated escape state)
        currentState = ObstacleState.EMERGENCY_ESCAPE;
    }

    /**
     * Handles Emergency Escape logic - following Netoxic's approach exactly
     */
    private void handleEmergencyEscape() {
        if (!emergencyEscapeTriggered) {
            return;
        }

        long timeSinceStart = System.currentTimeMillis() - emergencyEscapeStartTime;

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
        
        // Step 1: Equip necklace if found in inventory (with attempt counter)
        if (!hasEquippedPhoenixNecklace && Rs2Inventory.hasItem("Phoenix necklace")) {
            escapeEquipNecklaceAttempts++;
            Microbot.log("[WildernessNicky] Emergency Escape Step 1: Equipping Phoenix necklace (Attempt " + escapeEquipNecklaceAttempts + "/" + MAX_ESCAPE_STEP_ATTEMPTS + ")");

            // Failsafe: Skip if too many attempts
            if (escapeEquipNecklaceAttempts > MAX_ESCAPE_STEP_ATTEMPTS) {
                Microbot.log("[WildernessNicky] ⚠️ Failed to equip Phoenix necklace after " + MAX_ESCAPE_STEP_ATTEMPTS + " attempts - skipping to next step");
                hasEquippedPhoenixNecklace = true;
                return;
            }

            Rs2Inventory.wield("Phoenix necklace");
            boolean equipped = sleepUntil(() -> Rs2Equipment.isWearing("Phoenix necklace"), 2000);

            if (equipped) {
                hasEquippedPhoenixNecklace = true;
                Microbot.log("[WildernessNicky] ✅ Phoenix necklace equipped successfully");
            }
            return; // Wait for next loop iteration
        }
        
        // Step 2: Check if player is in rock area and climb if needed (with attempt counter)
        if (!hasClimbedRocks) {
            escapeClimbRocksAttempts++;
            WorldArea rockArea = new WorldArea(SOUTH_WEST_CORNER, ROCK_AREA_WIDTH, ROCK_AREA_HEIGHT);
            boolean isInArea = rockArea.contains(Rs2Player.getWorldLocation());
            WorldPoint currentLoc = Rs2Player.getWorldLocation();

            // Initialize step timer
            if (escapeStep2StartTime == 0) {
                escapeStep2StartTime = System.currentTimeMillis();
            }

            // Failsafe: Skip if too many attempts OR timeout
            long stepElapsedTime = System.currentTimeMillis() - escapeStep2StartTime;
            if (escapeClimbRocksAttempts > MAX_ESCAPE_STEP_ATTEMPTS || stepElapsedTime > ESCAPE_STEP_TIMEOUT) {
                Microbot.log("[WildernessNicky] ⚠️ Climbing rocks failed (Attempts: " + escapeClimbRocksAttempts + ", Time: " + (stepElapsedTime/1000) + "s) - skipping to next step");
                hasClimbedRocks = true;
                escapeStep2StartTime = 0;
                return;
            }

            if (isInArea) {
                Microbot.log("[WildernessNicky] Emergency Escape Step 2: Climbing rocks (Attempt " + escapeClimbRocksAttempts + "/" + MAX_ESCAPE_STEP_ATTEMPTS + ")");
                Rs2GameObject.interact(ROCKS_OBJECT_ID, "Climb");
                sleep(1200);
                boolean climbedSuccessfully = sleepUntil(() -> !Rs2Player.isMoving(), 5000);

                if (climbedSuccessfully) {
                    hasClimbedRocks = true;
                    escapeStep2StartTime = 0;
                    Microbot.log("[WildernessNicky] ✅ Rocks climbed successfully");
                }
                return;
            } else {
                // Check if we're within 3 tiles of the gate area - consider it arrived
                if (currentLoc != null && currentLoc.distanceTo(GATE_AREA) <= 3) {
                    Microbot.log("[WildernessNicky] Emergency Escape Step 2: Arrived at gate area (within 3 tiles)");
                    hasClimbedRocks = true;
                    escapeStep2StartTime = 0;
                    return;
                }

                Microbot.log("[WildernessNicky] Emergency Escape Step 2: Walking to gate area (Attempt " + escapeClimbRocksAttempts + "/" + MAX_ESCAPE_STEP_ATTEMPTS + ")");
                Rs2Walker.walkTo(GATE_AREA, 4);
                return;
            }
        }
        
        // Step 3: Open gate (with attempt counter)
        if (!hasOpenedGate) {
            escapeOpenGateAttempts++;
            Microbot.log("[WildernessNicky] Emergency Escape Step 3: Opening gate (Attempt " + escapeOpenGateAttempts + "/" + MAX_ESCAPE_STEP_ATTEMPTS + ")");

            // Failsafe: Skip if too many attempts
            if (escapeOpenGateAttempts > MAX_ESCAPE_STEP_ATTEMPTS) {
                Microbot.log("[WildernessNicky] ⚠️ Failed to open gate after " + MAX_ESCAPE_STEP_ATTEMPTS + " attempts - skipping to next step");
                hasOpenedGate = true;
                return;
            }

            sleepUntilOnClientThread(() -> Rs2GameObject.getGameObject(GATE_OBJECT_ID) != null, 3000);
            Rs2GameObject.interact(GATE_OBJECT_ID, "Open");
            sleep(1000);
            hasOpenedGate = true;
            Microbot.log("[WildernessNicky] ✅ Gate opened successfully");
            return;
        }

        // Step 4: Walk to Mage Bank (with attempt counter)
        escapeWalkToMageBankAttempts++;
        Microbot.log("[WildernessNicky] Emergency Escape Step 4: Walking to Mage Bank (Attempt " + escapeWalkToMageBankAttempts + "/" + MAX_ESCAPE_STEP_ATTEMPTS + ")");

        // Failsafe: Logout if too many walk attempts
        if (escapeWalkToMageBankAttempts > MAX_ESCAPE_STEP_ATTEMPTS) {
            Microbot.log("[WildernessNicky] ⚠️ Failed to reach Mage Bank after " + MAX_ESCAPE_STEP_ATTEMPTS + " attempts - logging out for safety");
            Rs2Player.logout();
            return;
        }

        Rs2Walker.walkTo(new WorldPoint(2534, 4712, 0), 20); // Mage Bank coordinates
        
        // Check if we've reached Mage Bank area
        WorldPoint currentLoc = Rs2Player.getWorldLocation();
        if (currentLoc != null && currentLoc.distanceTo(new WorldPoint(2534, 4712, 0)) <= 10) {
            Microbot.log("[WildernessNicky] ✅ Successfully reached Mage Bank safely!");
            Microbot.log("[WildernessNicky] 📋 Escape was triggered by: " + lastEscapeReason);
            Microbot.log("[WildernessNicky] 🚪 Logging out for safety");
            // Logout until successful (Netoxic's approach)
            while (Microbot.isLoggedIn()) {
                Rs2Player.logout();
                sleepUntil(() -> !Microbot.isLoggedIn(), 300);
            }
            // Reset escape state
            emergencyEscapeTriggered = false;
            emergencyEscapeStartTime = 0;
            hasEquippedPhoenixNecklace = false;
            hasClimbedRocks = false;
            hasOpenedGate = false;
            escapeStep2StartTime = 0;
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
    }

    /**
     * Attempts to hop to a world with retry logic and proper error handling
     * @param targetWorld The world number to hop to
     * @param context Context string for logging (e.g., "banking", "returning")
     * @return true if hop was successful, false if failed after retries
     */
    private boolean attemptWorldHop(int targetWorld, String context) {
        // Check if we're already on the target world
        if (Rs2Player.getWorld() == targetWorld) {
            return true;
        }
        
        // Initialize retry tracking if this is a new attempt
        if (worldHopRetryCount == 0) {
            worldHopRetryStartTime = System.currentTimeMillis();
        }
        
        // Check if we've exceeded max retries or timeout
        long timeSinceStart = System.currentTimeMillis() - worldHopRetryStartTime;
        if (worldHopRetryCount >= MAX_WORLD_HOP_RETRIES || timeSinceStart > WORLD_HOP_RETRY_TIMEOUT) {
            Microbot.log("World hop to " + targetWorld + " failed after " + worldHopRetryCount + " attempts in " + context);
            worldHopRetryCount = 0; // Reset for next attempt
            return false;
        }
        
        worldHopRetryCount++;
        
        // Reset cantHopWorld flag before attempting hop
        Microbot.cantHopWorld = false;
        
        Microbot.log("Attempting world hop to " + targetWorld + " (attempt " + worldHopRetryCount + "/" + MAX_WORLD_HOP_RETRIES + ") in " + context);
        
        boolean hopSuccess = Microbot.hopToWorld(targetWorld);
        if (!hopSuccess) {
            Microbot.log("Failed to initiate world hop to " + targetWorld + " in " + context);
            return false; // Will retry on next call
        }
        
        boolean hopConfirmed = sleepUntil(() -> Rs2Player.getWorld() == targetWorld, 8000);
        if (!hopConfirmed) {
            Microbot.log("World hop to " + targetWorld + " not confirmed in " + context + ", current world: " + Rs2Player.getWorld());
            return false; // Will retry on next call
        }
        
        // Success! Reset retry counter
        Microbot.log("Successfully hopped to world " + targetWorld + " in " + context);
        worldHopRetryCount = 0;
        return true;
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
            if (config.joinFc()) {
                joinFriendChat();
            }
            currentState = ObstacleState.PIPE;
            return;
        }
        
        if (!attemptWorldHop(originalWorld, "returning to original world")) {
            return; // Stay in this state to retry
        }
        
        if (config.joinFc()) {
            joinFriendChat();
        }
        currentState = ObstacleState.PIPE;
    }

    private void leaveFriendChat() {
        try {
            // Check if already in a clan channel
            net.runelite.api.clan.ClanChannel clanChannel = Microbot.getClient().getClanChannel();
            if (clanChannel == null) {
                Microbot.log("[WildernessNicky] Not in a clan channel - skipping leave");
                return;
            }

            Microbot.log("[WildernessNicky] Leaving clan channel: " + clanChannel.getName());

            // Leave via chat-channel using widget method
            Rs2Tab.switchTo(InterfaceTab.CHAT);
            sleep(300, 600);

            // Wait for the Leave button to appear
            boolean foundLeaveButton = Rs2Widget.sleepUntilHasWidgetText("Leave",
                WidgetIndices.ChatChannel.GROUP_INDEX,
                WidgetIndices.ChatChannel.JOIN_LABEL, false, 2000);

            if (!foundLeaveButton) {
                Microbot.log("[WildernessNicky] Leave button not found - may already be outside channel");
                return;
            }

            // Click the Leave button
            Rs2Widget.clickWidget(WidgetIndices.ChatChannel.GROUP_INDEX,
                    WidgetIndices.ChatChannel.JOIN_DYNAMIC_CONTAINER);

            sleep(600, 1000);

            // Verify we left
            clanChannel = Microbot.getClient().getClanChannel();
            if (clanChannel == null) {
                Microbot.log("[WildernessNicky] ✅ Successfully left clan channel");
            } else {
                Microbot.log("[WildernessNicky] ⚠️ May still be in clan channel");
            }
        } catch (Exception e) {
            Microbot.log("[WildernessNicky] Error leaving clan channel: " + e.getMessage());
        }
    }

    private void joinFriendChat() {
        joinChatChannel(config.fcChannel());
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
                Microbot.log("[WildernessNicky] Join button not found - trying alternate method");
                // Try typing /join command as fallback
                Rs2Keyboard.typeString("/join " + channelName);
                Rs2Keyboard.enter();
                sleep(1000, 1500);
                return;
            }

            // Click the Join button
            Rs2Widget.clickWidget(WidgetIndices.ChatChannel.GROUP_INDEX,
                    WidgetIndices.ChatChannel.JOIN_DYNAMIC_CONTAINER);

            sleep(600, 1000);

            // Type the channel name
            Rs2Keyboard.typeString(channelName);
            sleep(300, 500);
            Rs2Keyboard.enter();

            sleep(1000, 1500);

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
        // Enable Player Monitor when starting banking process if configured
        if (config.enablePlayerMonitor()) {
            try {
                Plugin playerMonitor = Microbot.getPluginManager().getPlugins().stream()
                        .filter(x -> x.getClass().getName().equals("net.runelite.client.plugins.microbot.playermonitor.PlayerMonitorPlugin"))
                        .findFirst()
                        .orElse(null);
                if (playerMonitor != null) {
                    Microbot.startPlugin(playerMonitor);
                    Microbot.log("[WildernessNicky] Player Monitor enabled for banking safety");
                } else {
                    Microbot.log("[WildernessNicky] Player Monitor plugin not found - continuing without it");
                }
            } catch (Exception e) {
                Microbot.log("[WildernessNicky] Failed to start Player Monitor: " + e.getMessage());
            }
        }
        
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


    private void handlePostBankConfig() {
        forceBankNextLoot = false;
        if (config.swapBack() && Rs2Player.getWorld() != originalWorld) {
            if (!attemptWorldHop(originalWorld, "post-bank config")) {
                return; // Stay in this state to retry
            }
        }
        if (config.joinFc()) {
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
        TileObject dispenserObj = getDispenserObj();
        if (dispenserObj != null) {
            int coinCount = Rs2Inventory.itemQuantity(COINS_ID);
            if (coinCount >= 150000) {
                Microbot.log("[WildernessNicky] [WALK_TO_COURSE] Attempting to deposit " + coinCount + " coins into dispenser");
                Rs2Inventory.use(COINS_ID);
                sleep(400);
                Rs2GameObject.interact(dispenserObj, "Use");
                sleep(getActionDelay());
                sleepUntil(() -> Rs2Inventory.itemQuantity(COINS_ID) < coinCount, getXpTimeout());
            } else {
                Microbot.log("[WildernessNicky] [WALK_TO_COURSE] Not enough coins (" + coinCount + " < 150000)");
            }
        } else {
            Microbot.log("[WildernessNicky] [WALK_TO_COURSE] Dispenser object not found!");
        }
        currentState = ObstacleState.PIPE;
    }

    private void handleBanking() {
        // Single-step banking logic
        if (!Rs2Bank.isOpen()) {
            Rs2Bank.openBank();
            sleepUntil(Rs2Bank::isOpen, 20000);
            if (!Rs2Bank.isOpen()) return;
        }
        
        // Disable Player Monitor once we successfully reach the bank
        if (config.enablePlayerMonitor()) {
            try {
                Plugin playerMonitor = Microbot.getPluginManager().getPlugins().stream()
                        .filter(x -> x.getClass().getName().equals("net.runelite.client.plugins.microbot.playermonitor.PlayerMonitorPlugin"))
                        .findFirst()
                        .orElse(null);
                if (playerMonitor != null) {
                    Microbot.stopPlugin(playerMonitor);
                    Microbot.log("[WildernessNicky] Player Monitor disabled - safely reached bank");
                } else {
                    Microbot.log("[WildernessNicky] Player Monitor plugin not found - nothing to disable");
                }
            } catch (Exception e) {
                Microbot.log("[WildernessNicky] Failed to stop Player Monitor: " + e.getMessage());
            }
        }
        
        // ===== BANKED LOOT TRACKING =====
        // Track loot from inventory and looting bag before banking
        trackBankedLoot();

        // Check if we have an open looting bag and deposit its contents first
        if (Rs2Inventory.hasItem(LOOTING_BAG_OPEN_ID)) {
            Microbot.log("[WildernessNicky] Depositing looting bag contents (value: " + NumberFormat.getIntegerInstance().format(lootingBagValue) + "gp)");
            Rs2Bank.depositLootingBag();
            sleep(getActionDelay());

            // Reset looting bag value after deposit
            lootingBagValue = 0;
            Microbot.log("[WildernessNicky] Looting bag value reset after deposit");
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
                    Microbot.log("[WildernessNicky] Failed to withdraw looting bag in any state");
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
        
        // Withdraw Ice Plateau TP (if enabled) - ORDER 5
        if (config.useIcePlateauTp() && !Rs2Inventory.hasItem(TELEPORT_ID)) {
            Rs2Bank.withdrawOne(TELEPORT_ID);
            sleepUntil(() -> Rs2Inventory.hasItem(TELEPORT_ID), 3000);
        }
        
        // Confirm all items are present
        boolean venomPresent = config.withdrawVenomProtection() == WildernessNickyConfig.VenomProtectionOption.None || 
            (config.withdrawVenomProtection().getItemId() != -1 && Rs2Inventory.hasItem(config.withdrawVenomProtection().getItemId()));
        
        boolean allPresent = (!config.withdrawKnife() || Rs2Inventory.hasItem(KNIFE_ID))
            && (!config.withdrawCoins() || Rs2Inventory.hasItem(COINS_ID))
            && (!config.useIcePlateauTp() || Rs2Inventory.hasItem(TELEPORT_ID))
            && (!config.withdrawLootingBag() || Rs2Inventory.hasItem(LOOTING_BAG_CLOSED_ID) || Rs2Inventory.hasItem(LOOTING_BAG_OPEN_ID))
            && venomPresent;
        if (!allPresent) return;

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
     * Adds items to looting bag value tracker (2 items)
     */
    private void addLootingBagValue(int qty1, String item1, int qty2, String item2) {
        int itemId1 = wildyItems.nameToItemId(item1);
        int itemId2 = wildyItems.nameToItemId(item2);
        
        int value1 = Microbot.getItemManager().getItemPrice(itemId1) * qty1;
        int value2 = Microbot.getItemManager().getItemPrice(itemId2) * qty2;
        
        lootingBagValue += value1 + value2;
    }
    
    /**
     * Adds items to looting bag value tracker (3 items - with bonus)
     */
    private void addLootingBagValue(int qty1, String item1, int qty2, String item2, int qty3, String item3) {
        int itemId1 = wildyItems.nameToItemId(item1);
        int itemId2 = wildyItems.nameToItemId(item2);
        int itemId3 = wildyItems.nameToItemId(item3);
        
        int value1 = Microbot.getItemManager().getItemPrice(itemId1) * qty1;
        int value2 = Microbot.getItemManager().getItemPrice(itemId2) * qty2;
        int value3 = Microbot.getItemManager().getItemPrice(itemId3) * qty3;
        
        lootingBagValue += value1 + value2 + value3;
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
        // Check if this is the looting bag container
        if (event.getContainerId() == LOOTING_BAG_CONTAINER_ID && waitingForLootingBagSync) {
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

    /**
     * Syncs looting bag value from the ItemContainer when "Check" is used
     */
    private void syncLootingBagFromContainer(net.runelite.api.ItemContainer container) {
        if (container == null) {
            Microbot.log("[WildernessNicky] Looting bag is empty (container null)");
            lootingBagValue = 0;
            waitingForLootingBagSync = false;
            return;
        }
        
        wildyItems.setupWildernessItemsIfEmpty();
        
        // Calculate value from container items
        int totalValue = 0;
        for (net.runelite.api.Item item : container.getItems()) {
            if (item.getId() > 0) { // Valid item
                int itemValue = Microbot.getItemManager().getItemPrice(item.getId()) * item.getQuantity();
                totalValue += itemValue;
            }
        }
        
        lootingBagValue = totalValue;
        waitingForLootingBagSync = false;
        
        Microbot.log("[WildernessNicky] Synced looting bag from container: " + 
            java.text.NumberFormat.getIntegerInstance().format(lootingBagValue) + "gp");
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

                // Switch prayer if different from current
                if (requiredPrayer != null && requiredPrayer != activeCombatPrayer) {
                    String attackStyle = WildernessProjectileType.getAttackStyleName(projectileId);
                    Microbot.log(String.format("[WildernessNicky] ⚡ SWITCHING PRAYER: %s attack incoming! (Projectile ID: %d)",
                        attackStyle, projectileId));

                    switchProtectionPrayer(requiredPrayer);
                    lastCombatActionTime = System.currentTimeMillis();
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
     * Switches to the specified protection prayer
     */
    private void switchProtectionPrayer(Rs2PrayerEnum prayer) {
        try {
            // Disable current protection prayer if different
            if (activeCombatPrayer != null && activeCombatPrayer != prayer) {
                Rs2Prayer.toggle(activeCombatPrayer, false);
            }

            // Enable new protection prayer
            if (!Rs2Prayer.isPrayerActive(prayer)) {
                Rs2Prayer.toggle(prayer, true);
                activeCombatPrayer = prayer;
            }
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
