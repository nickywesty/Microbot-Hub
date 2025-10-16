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

    public boolean run(SmartMinerConfig config) {
        Microbot.enableAutoRunOn = false;
        Rs2Antiban.resetAntibanSettings();
        setupAntiban();

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
                sleep(1200, 1800);
            }
            return;
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
        if (Rs2Player.isMoving()) return;

        if (miningLocation == null) {
            miningLocation = config.usePresetLocation()
                    ? config.miningLocation().getLocation()
                    : startingLocation;
        }

        int distanceToMine = Rs2Player.getWorldLocation().distanceTo(miningLocation);

        if (distanceToMine <= config.miningRadius()) {
            currentState = MiningState.MINING;
            Microbot.status = "Arrived at mining location";
            return;
        }

        Rs2Walker.walkTo(miningLocation, Math.min(config.miningRadius(), 5));
        Rs2Antiban.actionCooldown();
    }

    private void handleMining(SmartMinerConfig config) {
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
        if (Rs2AntibanSettings.naturalMouse) {
            Rs2GameObject.hoverOverObject(nearestRock);
            sleep(100, 300);
        }

        // Click the rock
        if (Rs2GameObject.interact(nearestRock)) {
            lastMinedRock = nearestRock;
            lastRockClickTime = System.currentTimeMillis();
            Microbot.status = "Mining " + getRockName(nearestRock);

            // Wait for XP drop to confirm mining started
            Rs2Player.waitForXpDrop(Skill.MINING, true);
            Rs2Antiban.actionCooldown();
            Rs2Antiban.takeMicroBreakByChance();
        }
    }

    private void handleDropping(SmartMinerConfig config) {
        String[] itemsToKeep = Arrays.stream(config.itemsToKeep().split(","))
                .map(String::trim)
                .toArray(String[]::new);

        // Get list of ores to drop
        List<String> oresToDrop = getSelectedOreTypes(config).stream()
                .map(OreType::getOreName)
                .collect(Collectors.toList());

        // Drop ores
        for (String ore : oresToDrop) {
            if (Rs2Inventory.contains(ore)) {
                Rs2Inventory.drop(ore);
                sleep(50, 150);
            }
        }

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

    private void setupAntiban() {
        Rs2Antiban.antibanSetupTemplates.applyMiningSetup();
        Rs2AntibanSettings.actionCooldownChance = 0.2;
        Rs2AntibanSettings.microBreakChance = 0.05;
        Rs2AntibanSettings.naturalMouse = true;
        Rs2AntibanSettings.moveMouseOffScreen = true;
        Rs2AntibanSettings.moveMouseRandomly = true;
        Rs2AntibanSettings.moveMouseRandomlyChance = 0.1;
    }

    @Override
    public void shutdown() {
        super.shutdown();
        Rs2Antiban.resetAntibanSettings();
    }
}
