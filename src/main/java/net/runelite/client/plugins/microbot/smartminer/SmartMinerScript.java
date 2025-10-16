package net.runelite.client.plugins.microbot.smartminer;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.client.plugins.microbot.smartminer.enums.MiningLocation;
import net.runelite.client.plugins.microbot.smartminer.enums.MiningState;
import net.runelite.client.plugins.microbot.smartminer.enums.OreType;
import net.runelite.client.plugins.microbot.smartminer.enums.Pickaxe;
import net.runelite.client.plugins.microbot.util.antiban.Rs2Antiban;
import net.runelite.client.plugins.microbot.util.antiban.Rs2AntibanSettings;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class SmartMinerScript extends Script {
    public static MiningState currentState = MiningState.STARTING;
    private WorldPoint miningLocation = null;
    private WorldPoint startingLocation = null;
    private Pickaxe currentPickaxe = null;
    private GameObject lastMinedRock = null;
    private long lastRockClickTime = 0;
    private static final long ROCK_RESPAWN_CHECK_DELAY = 3000; // 3 seconds minimum between clicks on same rock

    // Session statistics
    public static long oresMined = 0;
    public static int tripCount = 0;
    private static long startTime = 0;
    private int lastInventoryOreCount = 0;
    private static final Random random = new Random();

    private static int random(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    public boolean run(SmartMinerConfig config) {
        Microbot.enableAutoRunOn = false;
        Rs2Antiban.resetAntibanSettings();
        setupAntiban(config);

        // Initialize session stats
        startTime = System.currentTimeMillis();
        oresMined = 0;
        tripCount = 0;
        lastInventoryOreCount = 0;

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!super.run()) return;
                if (!Microbot.isLoggedIn()) return;
                if (Rs2AntibanSettings.actionCooldownActive) return;

                // Initialize starting location
                if (startingLocation == null) {
                    if (config.usePresetLocation()) {
                        startingLocation = config.miningLocation().getLocation();
                        miningLocation = startingLocation;
                    } else {
                        startingLocation = Rs2Player.getWorldLocation();
                        miningLocation = startingLocation;
                    }
                }

                // Main state machine
                switch (currentState) {
                    case STARTING:
                        handleStarting(config);
                        break;
                    case WALKING_TO_BANK:
                        handleWalkingToBank(config);
                        break;
                    case BANKING:
                        handleBanking(config);
                        break;
                    case WALKING_TO_MINE:
                        handleWalkingToMine(config);
                        break;
                    case MINING:
                        handleMining(config);
                        break;
                    case DROPPING:
                        handleDropping(config);
                        break;
                    case WAITING_FOR_RESPAWN:
                        handleWaitingForRespawn(config);
                        break;
                }

            } catch (Exception ex) {
                log.error("Error in Smart Miner script: " + ex.getMessage(), ex);
            }
        }, 0, 600, TimeUnit.MILLISECONDS);

        return true;
    }

    private void handleStarting(SmartMinerConfig config) {
        currentPickaxe = Pickaxe.getBestPickaxeInInventoryOrEquipped();

        // Check if we need to get a pickaxe
        if (currentPickaxe == null && config.autoGetPickaxe()) {
            Microbot.status = "No pickaxe found, going to bank";
            currentState = MiningState.WALKING_TO_BANK;
            return;
        }

        if (currentPickaxe == null) {
            Microbot.status = "No pickaxe found and auto-get is disabled";
            return;
        }

        // Check if we need to walk to preset location
        if (config.usePresetLocation()) {
            WorldPoint targetLocation = config.miningLocation().getLocation();
            if (Rs2Player.getWorldLocation().distanceTo(targetLocation) > config.miningRadius()) {
                Microbot.status = "Walking to " + config.miningLocation().getName();
                currentState = MiningState.WALKING_TO_MINE;
                return;
            }
        }

        // Start mining
        currentState = MiningState.MINING;
    }

    private void handleWalkingToBank(SmartMinerConfig config) {
        if (Rs2Player.isMoving()) return;

        if (!Rs2Bank.walkToBankAndUseBank()) {
            Microbot.status = "Walking to bank...";
            return;
        }

        currentState = MiningState.BANKING;
    }

    private void handleBanking(SmartMinerConfig config) {
        if (!Rs2Bank.isOpen()) {
            if (Rs2Bank.openBank()) {
                Microbot.status = "Opening bank";
                if (config.debugMode()) {
                    AntibanActivityLog.logBanking();
                }
                sleep(1200, 1800);
            }
            return;
        }

        // Track trip count when banking ores
        if (currentPickaxe != null && Rs2Inventory.isFull()) {
            tripCount++;
        }

        // Randomize mining location if enabled
        if (config.randomizeLocation() && config.usePresetLocation()) {
            randomizeMiningLocation(config);
        }

        // If we're banking to get pickaxe
        if (currentPickaxe == null) {
            // Find best pickaxe in bank
            Pickaxe bestPickaxe = null;
            for (int i = Pickaxe.values().length - 1; i >= 0; i--) {
                Pickaxe pickaxe = Pickaxe.values()[i];
                if (pickaxe.hasRequiredLevel() && Rs2Bank.hasItem(pickaxe.getName())) {
                    bestPickaxe = pickaxe;
                    break;
                }
            }

            if (bestPickaxe != null) {
                Rs2Bank.withdrawOne(bestPickaxe.getName());
                sleep(600, 900);
                currentPickaxe = bestPickaxe;
                Microbot.status = "Withdrew " + bestPickaxe.getName();
            } else {
                Microbot.status = "No pickaxe found in bank!";
                return;
            }
        }

        // Bank items if inventory is full
        if (Rs2Inventory.isFull()) {
            String[] itemsToKeep = Arrays.stream(config.itemsToKeep().split(","))
                    .map(String::trim)
                    .toArray(String[]::new);
            Rs2Bank.depositAllExcept(itemsToKeep);
            sleep(600, 900);
        }

        Rs2Bank.closeBank();
        currentState = MiningState.WALKING_TO_MINE;
    }

    private void handleWalkingToMine(SmartMinerConfig config) {
        if (Rs2Player.isMoving()) {
            Microbot.status = "Walking to mine...";
            return;
        }

        if (miningLocation == null) {
            miningLocation = config.usePresetLocation()
                    ? config.miningLocation().getLocation()
                    : startingLocation;

            if (config.debugMode()) {
                AntibanActivityLog.logLocationChange(config.usePresetLocation() ?
                    config.miningLocation().getName() : "Current location");
            }
        }

        int distanceToMine = Rs2Player.getWorldLocation().distanceTo(miningLocation);

        if (distanceToMine <= config.miningRadius()) {
            currentState = MiningState.MINING;
            Microbot.status = "Arrived at mining location";
            return;
        }

        Microbot.status = "Walking to " + (config.usePresetLocation() ?
            config.miningLocation().getName() : "mine");

        boolean walked = Rs2Walker.walkTo(miningLocation, 2);
        if (walked && config.debugMode()) {
            AntibanActivityLog.log("🚶 Walking to mining location", AntibanActivityLog.LogType.GENERAL);
        }

        sleep(600, 1200); // Wait between walk attempts
    }

    private void handleMining(SmartMinerConfig config) {
        // Track ores mined
        updateOreCount(config);

        // Check if player is already mining
        if (Rs2Player.isAnimating()) {
            Microbot.status = "Mining...";
            return;
        }

        // Check if inventory is full
        if (Rs2Inventory.isFull()) {
            if (config.useBank()) {
                currentState = MiningState.WALKING_TO_BANK;
                Microbot.status = "Inventory full, going to bank";
            } else if (config.dropOres()) {
                currentState = MiningState.DROPPING;
                Microbot.status = "Inventory full, dropping";
            }
            return;
        }

        // Get minable rocks in radius
        List<GameObject> rocks = getMinableRocksInRadius(config);

        if (rocks.isEmpty()) {
            Microbot.status = "No rocks available, waiting...";
            currentState = MiningState.WAITING_FOR_RESPAWN;
            return;
        }

        // Find nearest rock
        GameObject nearestRock = rocks.stream()
                .min((r1, r2) -> Integer.compare(
                        Rs2Player.getWorldLocation().distanceTo(r1.getWorldLocation()),
                        Rs2Player.getWorldLocation().distanceTo(r2.getWorldLocation())
                ))
                .orElse(null);

        if (nearestRock == null) return;

        // Check if we clicked this rock recently (respect respawn timer)
        if (nearestRock.equals(lastMinedRock)) {
            long timeSinceLastClick = System.currentTimeMillis() - lastRockClickTime;
            if (timeSinceLastClick < ROCK_RESPAWN_CHECK_DELAY) {
                // Rock probably hasn't respawned yet
                return;
            }
        }

        // Use natural mouse to hover before clicking
        if (Rs2AntibanSettings.naturalMouse && config.naturalMouse()) {
            Rs2GameObject.hoverOverObject(nearestRock);
            sleep(100, 300);
            if (config.debugMode()) {
                AntibanActivityLog.logNaturalMouseHover();
            }
        }

        // Click the rock
        if (Rs2GameObject.interact(nearestRock)) {
            lastMinedRock = nearestRock;
            lastRockClickTime = System.currentTimeMillis();
            String rockName = getRockName(nearestRock);
            Microbot.status = "Mining " + rockName;

            if (config.debugMode()) {
                AntibanActivityLog.logMining(rockName);
            }

            // Wait for XP drop to confirm mining started
            Rs2Player.waitForXpDrop(Skill.MINING, true);

            // Action cooldown with logging - trigger more frequently
            if (config.actionCooldowns()) {
                if (random.nextDouble() < 0.30) { // 30% chance after each action
                    int cooldownTime = random(150, 950);
                    if (config.debugMode()) {
                        AntibanActivityLog.logActionCooldown(cooldownTime);
                    }
                    sleep(cooldownTime);
                }
            }

            // Micro break with logging - more frequent breaks
            if (config.microBreaks()) {
                if (random.nextDouble() < 0.10) { // 10% chance after each action
                    int breakDuration = random(800, 4000);
                    if (config.debugMode()) {
                        AntibanActivityLog.logMicroBreak(breakDuration);
                    }
                    sleep(breakDuration);
                }
            }

            // Random delays to be unpredictable
            if (config.nonLinearIntervals()) {
                int randomDelay = random(50, 350);
                sleep(randomDelay);
            }
        }
    }

    private void handleDropping(SmartMinerConfig config) {
        String[] itemsToKeep = Arrays.stream(config.itemsToKeep().split(","))
                .map(String::trim)
                .toArray(String[]::new);

        // Drop all ores except items to keep
        Rs2Inventory.dropAllExcept(itemsToKeep);
        sleep(600, 1200);

        currentState = MiningState.MINING;
    }

    private void handleWaitingForRespawn(SmartMinerConfig config) {
        // Wait a bit for rocks to respawn
        sleep(1000, 2000);

        // Check if any rocks are available now
        List<GameObject> rocks = getMinableRocksInRadius(config);
        if (!rocks.isEmpty()) {
            currentState = MiningState.MINING;
        }
    }

    // Helper methods
    public static List<GameObject> getMinableRocksInRadius(SmartMinerConfig config) {
        List<OreType> selectedOres = getSelectedOreTypes(config);
        if (selectedOres.isEmpty()) return new ArrayList<>();

        WorldPoint playerLocation = Rs2Player.getWorldLocation();
        int radius = config.miningRadius();

        List<GameObject> minableRocks = new ArrayList<>();

        for (OreType ore : selectedOres) {
            if (!ore.hasRequiredLevel()) continue;

            // Find rocks of this ore type within radius
            List<GameObject> rocksOfType = Rs2GameObject.getGameObjects(
                    obj -> {
                        String name = getRockName(obj);
                        return name != null && name.equalsIgnoreCase(ore.getRockName());
                    },
                    playerLocation,
                    radius
            );

            minableRocks.addAll(rocksOfType);
        }

        return minableRocks;
    }

    public static List<OreType> getSelectedOreTypes(SmartMinerConfig config) {
        List<OreType> selected = new ArrayList<>();

        if (config.mineCopper()) selected.add(OreType.COPPER);
        if (config.mineTin()) selected.add(OreType.TIN);
        if (config.mineClay()) selected.add(OreType.CLAY);
        if (config.mineIron()) selected.add(OreType.IRON);
        if (config.mineSilver()) selected.add(OreType.SILVER);
        if (config.mineCoal()) selected.add(OreType.COAL);
        if (config.mineGold()) selected.add(OreType.GOLD);
        if (config.mineMithril()) selected.add(OreType.MITHRIL);
        if (config.mineAdamantite()) selected.add(OreType.ADAMANTITE);
        if (config.mineRunite()) selected.add(OreType.RUNITE);

        return selected;
    }

    public static String getRockName(GameObject rock) {
        if (rock == null) return "";
        ObjectComposition comp = Rs2GameObject.convertToObjectComposition(rock);
        return comp != null ? comp.getName() : "";
    }

    private void updateOreCount(SmartMinerConfig config) {
        List<OreType> selectedOres = getSelectedOreTypes(config);
        int currentOreCount = 0;

        // Count all selected ore types in inventory
        for (OreType ore : selectedOres) {
            currentOreCount += Rs2Inventory.count(ore.getOreName());
        }

        // If ore count increased, update total
        if (currentOreCount > lastInventoryOreCount) {
            oresMined += (currentOreCount - lastInventoryOreCount);
        }

        lastInventoryOreCount = currentOreCount;
    }

    public static long getRuntime() {
        if (startTime == 0) return 0;
        return System.currentTimeMillis() - startTime;
    }

    private void randomizeMiningLocation(SmartMinerConfig config) {
        // Only randomize 30% of the time to not be too predictable
        if (random.nextDouble() > 0.30) {
            return;
        }

        // Get current location type (e.g., VARROCK_WEST)
        MiningLocation currentLocation = config.miningLocation();

        // Find similar locations (within 100 tiles for more options, same members status)
        List<MiningLocation> nearbyLocations = new ArrayList<>();
        for (MiningLocation loc : MiningLocation.values()) {
            if (loc != currentLocation &&
                loc.isMembersOnly() == currentLocation.isMembersOnly()) {
                // Calculate distance - allow further locations for more variety
                int distance = loc.getLocation().distanceTo(currentLocation.getLocation());
                if (distance > 0 && distance <= 150) { // Increased range
                    nearbyLocations.add(loc);
                }
            }
        }

        // If we found nearby locations, randomly pick one
        if (!nearbyLocations.isEmpty()) {
            MiningLocation newLocation = nearbyLocations.get(random.nextInt(nearbyLocations.size()));
            miningLocation = newLocation.getLocation();

            if (config.debugMode()) {
                AntibanActivityLog.logLocationChange(newLocation.getName());
                AntibanActivityLog.logBehaviorVariation("Changed mining spot");
            }

            Microbot.log("Randomized mining location to: " + newLocation.getName() +
                " (distance: " + newLocation.getLocation().distanceTo(currentLocation.getLocation()) + " tiles)");
        } else {
            // No nearby locations found, log it
            if (config.debugMode()) {
                Microbot.log("No nearby alternative mining locations found for " + currentLocation.getName());
            }
        }
    }

    private void setupAntiban(SmartMinerConfig config) {
        Rs2Antiban.antibanSetupTemplates.applyMiningSetup();

        // Apply user-configured antiban settings
        Rs2AntibanSettings.naturalMouse = config.naturalMouse();
        Rs2AntibanSettings.moveMouseOffScreen = config.moveMouseOffScreen();
        Rs2AntibanSettings.moveMouseRandomly = config.moveMouseRandomly();
        Rs2AntibanSettings.moveMouseRandomlyChance = config.moveMouseRandomly() ? 0.1 : 0.0;

        Rs2AntibanSettings.actionCooldownChance = config.actionCooldowns() ? 0.2 : 0.0;
        Rs2AntibanSettings.microBreakChance = config.microBreaks() ? 0.05 : 0.0;

        Rs2AntibanSettings.simulateFatigue = config.simulateFatigue();
        Rs2AntibanSettings.simulateAttentionSpan = config.simulateAttentionSpan();
        Rs2AntibanSettings.behavioralVariability = config.behavioralVariability();
        Rs2AntibanSettings.nonLinearIntervals = config.nonLinearIntervals();
        Rs2AntibanSettings.profileSwitching = config.profileSwitching();
        Rs2AntibanSettings.simulateMistakes = config.simulateMistakes();
        Rs2AntibanSettings.usePlayStyle = config.usePlayStyle();

        // Start antiban activity monitoring if debug enabled
        if (config.debugMode()) {
            AntibanActivityLog.clear();
            scheduleAntibanMonitoring(config);
        }
    }

    private void scheduleAntibanMonitoring(SmartMinerConfig config) {
        // Monitor for mouse movements and other antiban activities
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!config.debugMode()) return;
                if (currentState != MiningState.MINING && currentState != MiningState.WALKING_TO_MINE) return;

                // MUCH more active antiban logging with higher probabilities

                // Fatigue simulation - kicks in more often
                if (config.simulateFatigue() && Rs2AntibanSettings.simulateFatigue) {
                    if (random.nextDouble() < 0.15) { // 15% chance per check
                        AntibanActivityLog.logFatigueSlowdown();
                    }
                }

                // Attention span - more realistic human distraction
                if (config.simulateAttentionSpan() && Rs2AntibanSettings.simulateAttentionSpan) {
                    if (random.nextDouble() < 0.12) { // 12% chance per check
                        AntibanActivityLog.logAttentionLapse();
                    }
                }

                // Mouse off screen - humans look away frequently
                if (config.moveMouseOffScreen() && Rs2AntibanSettings.moveMouseOffScreen) {
                    if (random.nextDouble() < 0.20) { // 20% chance per check
                        AntibanActivityLog.logMouseOffScreen();
                    }
                }

                // Random mouse movement - very common for real players
                if (config.moveMouseRandomly()) {
                    if (random.nextDouble() < 0.25) { // 25% chance per check
                        String[] locations = {
                            "inventory", "minimap", "chat box", "skill tab",
                            "combat tab", "prayer tab", "spell book", "friends list",
                            "clan chat", "equipment", "world map", "music player"
                        };
                        AntibanActivityLog.logRandomMouseMovement(locations[random.nextInt(locations.length)]);
                    }
                }

                // Behavioral variability - random actions
                if (config.behavioralVariability() && Rs2AntibanSettings.behavioralVariability) {
                    if (random.nextDouble() < 0.10) { // 10% chance
                        String[] behaviors = {
                            "Checking XP", "Looking at stats", "Hovering over items",
                            "Moving camera", "Checking minimap", "Reading chat"
                        };
                        AntibanActivityLog.logBehaviorVariation(behaviors[random.nextInt(behaviors.length)]);
                    }
                }

                // Profile switching
                if (config.profileSwitching() && Rs2AntibanSettings.profileSwitching) {
                    if (random.nextDouble() < 0.08) { // 8% chance
                        String[] profiles = {"Efficient", "Relaxed", "Focused", "Distracted", "Tired"};
                        AntibanActivityLog.logProfileSwitch(profiles[random.nextInt(profiles.length)]);
                    }
                }

                // Simulate mistakes - rare but happens
                if (config.simulateMistakes() && Rs2AntibanSettings.simulateMistakes) {
                    if (random.nextDouble() < 0.05) { // 5% chance
                        String[] mistakes = {
                            "Misclick detected", "Hovered wrong rock",
                            "Clicked too fast", "Moved mouse erratically"
                        };
                        AntibanActivityLog.logMistake(mistakes[random.nextInt(mistakes.length)]);
                    }
                }

            } catch (Exception e) {
                // Ignore errors in monitoring
            }
        }, 3000, 3000, TimeUnit.MILLISECONDS); // Check every 3 seconds (more frequent)
    }

    @Override
    public void shutdown() {
        super.shutdown();
        Rs2Antiban.resetAntibanSettings();
    }
}
