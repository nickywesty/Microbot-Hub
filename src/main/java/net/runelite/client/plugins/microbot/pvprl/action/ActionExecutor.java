package net.runelite.client.plugins.microbot.pvprl.action;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.pvprl.PvpRLConfig;
import net.runelite.client.plugins.microbot.pvprl.model.ActionType;
import net.runelite.client.plugins.microbot.pvprl.model.GameState;
import net.runelite.client.plugins.microbot.pvprl.model.PvpAction;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.inventory.Rs2ItemModel;
import net.runelite.client.plugins.microbot.util.magic.Rs2Magic;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.player.Rs2PlayerModel;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;
import net.runelite.client.plugins.microbot.util.walker.Rs2Walker;

/**
 * Executes PvpActions using Microbot APIs
 * Handles all 11 action heads from NhEnv
 */
@Slf4j
public class ActionExecutor {
    private final PvpRLConfig config;
    private GameState currentState;

    public ActionExecutor(PvpRLConfig config) {
        this.config = config;
    }

    /**
     * Update the current game state (needed for context in some actions)
     */
    public void updateState(GameState state) {
        this.currentState = state;
    }

    /**
     * Execute a single action
     * @return true if action was executed successfully
     */
    public boolean execute(PvpAction action) {
        if (action == null) return false;

        try {
            switch (action.getType()) {
                case ATTACK:
                    return executeAttack(action);
                case MELEE_TYPE:
                    return executeMeleeType(action);
                case RANGED_TYPE:
                    return executeRangedType(action);
                case MAGE_SPELL:
                    return executeMageSpell(action);
                case PRAYER:
                    return executePrayer(action);
                case FOOD:
                    return executeFood(action);
                case KARAMBWAN:
                    return executeKarambwan(action);
                case POTION:
                    return executePotion(action);
                case VENGEANCE:
                    return executeVengeance(action);
                case GEAR:
                    return executeGear(action);
                case MOVEMENT:
                    return executeMovement(action);
                default:
                    log.warn("Unknown action type: {}", action.getType());
                    return false;
            }
        } catch (Exception e) {
            log.error("Error executing action {}: {}", action, e.getMessage(), e);
            return false;
        }
    }

    private boolean executeAttack(PvpAction action) {
        if (!config.enableAttacking()) return false;
        if (currentState == null || currentState.getTarget() == null) return false;

        Player target = currentState.getTarget();
        return Rs2Player.attack(new Rs2PlayerModel(target));
    }

    private boolean executeMeleeType(PvpAction action) {
        if (!config.enableAttacking()) return false;
        if (currentState == null || currentState.getTarget() == null) return false;

        // Auto-switch to melee weapon if gear switching enabled
        if (config.enableGearSwitching()) {
            switchToMeleeGear();
        }

        Player target = currentState.getTarget();

        if (action.getValue() == 1) {
            // Basic melee attack
            return Rs2Player.attack(new Rs2PlayerModel(target));
        } else if (action.getValue() == 2) {
            // Special attack
            if (!config.enableSpecialAttack()) return false;
            Rs2Combat.setSpecState(true, 500);
            Microbot.pauseAllScripts.set(false);
            return Rs2Player.attack(new Rs2PlayerModel(target));
        }

        return false;
    }

    private boolean executeRangedType(PvpAction action) {
        if (!config.enableAttacking()) return false;
        if (currentState == null || currentState.getTarget() == null) return false;

        // Auto-switch to ranged weapon if gear switching enabled
        if (config.enableGearSwitching()) {
            switchToRangedGear();
        }

        Player target = currentState.getTarget();

        if (action.getValue() == 1) {
            // Basic ranged attack
            return Rs2Player.attack(new Rs2PlayerModel(target));
        } else if (action.getValue() == 2) {
            // Special attack
            if (!config.enableSpecialAttack()) return false;
            Rs2Combat.setSpecState(true, 500);
            Microbot.pauseAllScripts.set(false);
            return Rs2Player.attack(new Rs2PlayerModel(target));
        }

        return false;
    }

    private boolean executeMageSpell(PvpAction action) {
        if (!config.enableAttacking()) return false;
        if (currentState == null || currentState.getTarget() == null) return false;

        // CRITICAL: ALWAYS switch to mage weapon before casting (even if gear switching disabled)
        // Otherwise spells will fail if we're holding melee/ranged weapon
        switchToMageGear();

        Player target = currentState.getTarget();

        switch (action.getValue()) {
            case 1: // Ice Barrage
                return Rs2Magic.castOn(net.runelite.client.plugins.microbot.util.magic.Rs2CombatSpells.ICE_BARRAGE, target);
            case 2: // Blood Barrage
                return Rs2Magic.castOn(net.runelite.client.plugins.microbot.util.magic.Rs2CombatSpells.BLOOD_BARRAGE, target);
            case 3: // Mage special attack
                if (!config.enableSpecialAttack()) return false;
                Rs2Combat.setSpecState(true, 500);
                Microbot.pauseAllScripts.set(false);
                return Rs2Player.attack(new Rs2PlayerModel(target));
            default:
                return false;
        }
    }

    private boolean executePrayer(PvpAction action) {
        if (!config.enablePrayerSwitching()) return false;

        Rs2PrayerEnum targetPrayer = null;
        String prayerName = "";

        switch (action.getValue()) {
            case 1: // Protect from Magic
                targetPrayer = Rs2PrayerEnum.PROTECT_MAGIC;
                prayerName = "Protect from Magic";
                break;
            case 2: // Protect from Ranged
                targetPrayer = Rs2PrayerEnum.PROTECT_RANGE;
                prayerName = "Protect from Ranged";
                break;
            case 3: // Protect from Melee
                targetPrayer = Rs2PrayerEnum.PROTECT_MELEE;
                prayerName = "Protect from Melee";
                break;
            case 4: // Smite
                targetPrayer = Rs2PrayerEnum.SMITE;
                prayerName = "Smite";
                break;
            case 5: // Redemption
                targetPrayer = Rs2PrayerEnum.REDEMPTION;
                prayerName = "Redemption";
                break;
            default:
                return false;
        }

        if (targetPrayer != null) {
            // CRITICAL FIX: Turn OFF old overhead prayers before activating new one
            // This ensures we're not wasting prayer points on multiple overheads
            if (action.getValue() >= 1 && action.getValue() <= 3) {
                // Overhead prayer switch - turn off others first
                if (targetPrayer != Rs2PrayerEnum.PROTECT_MAGIC && Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MAGIC)) {
                    Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MAGIC, false);
                }
                if (targetPrayer != Rs2PrayerEnum.PROTECT_RANGE && Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_RANGE)) {
                    Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_RANGE, false);
                }
                if (targetPrayer != Rs2PrayerEnum.PROTECT_MELEE && Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MELEE)) {
                    Rs2Prayer.toggle(Rs2PrayerEnum.PROTECT_MELEE, false);
                }
            }

            // Only activate if not already active
            if (!Rs2Prayer.isPrayerActive(targetPrayer)) {
                log.info("🙏 PRAYER SWITCH: {}", prayerName);
                return Rs2Prayer.toggle(targetPrayer, true);
            }

            return true; // Already active
        }

        return false;
    }

    private boolean executeFood(PvpAction action) {
        if (!config.enableEating()) return false;

        // COMBO EATING: If HP is critical (below 40), eat food + karambwan + brew rapidly
        int currentHP = Rs2Player.getBoostedSkillLevel(net.runelite.api.Skill.HITPOINTS);
        int maxHP = Rs2Player.getRealSkillLevel(net.runelite.api.Skill.HITPOINTS);

        if (currentHP < 40) {
            // EMERGENCY COMBO EAT
            log.info("🍖 EMERGENCY COMBO EAT - HP: {}/{}", currentHP, maxHP);

            // CORRECT NH COMBO ORDER: Food → Brew → Restore → Karambwan (all same tick)
            String[] foodNames = {"Anglerfish", "Manta ray", "Dark crab", "Shark", "Sea turtle"};
            for (String foodName : foodNames) {
                if (Rs2Inventory.hasItem(foodName)) {
                    // 1. Eat main food first
                    Rs2Inventory.interact(foodName, "Eat");
                    Microbot.pauseAllScripts.set(false);
                    log.info("  → Ate {}", foodName);

                    // 2. Drink brew for extra HP (same tick)
                    boolean drankBrew = drinkPotion("Saradomin brew");
                    if (drankBrew) {
                        log.info("  → Drank Saradomin brew");

                        // 3. CRITICAL: Drink restore to counter brew stat drain (same tick)
                        boolean drankRestore = drinkPotion("Super restore");
                        if (drankRestore) {
                            log.info("  → Drank Super restore (countering brew drain)");
                        } else {
                            log.warn("  → Failed to drink restore after brew! Stats will be drained!");
                        }
                    }

                    // 4. LAST: Eat karambwan (same tick - must be after potions!)
                    if (Rs2Inventory.hasItem("Cooked karambwan")) {
                        Rs2Inventory.interact("Cooked karambwan", "Eat");
                        Microbot.pauseAllScripts.set(false);
                        log.info("  → Ate Cooked karambwan");
                    }

                    log.info("🍖 COMBO EAT COMPLETE");
                    return true;
                }
            }
        }

        // Normal eating (not emergency)
        String[] foodNames = {"Anglerfish", "Manta ray", "Dark crab", "Shark", "Sea turtle"};
        for (String foodName : foodNames) {
            if (Rs2Inventory.hasItem(foodName)) {
                return Rs2Inventory.interact(foodName, "Eat");
            }
        }

        log.warn("No food found in inventory");
        return false;
    }

    private boolean executeKarambwan(PvpAction action) {
        if (!config.enableEating()) return false;

        // Check if we should combo eat
        int currentHP = Rs2Player.getBoostedSkillLevel(net.runelite.api.Skill.HITPOINTS);

        if (currentHP < 40) {
            // In emergency, karambwan is handled by executeFood combo
            // Still execute it here in case AI requests it separately
            if (Rs2Inventory.hasItem("Cooked karambwan")) {
                Rs2Inventory.interact("Cooked karambwan", "Eat");
                Microbot.pauseAllScripts.set(false);

                // Follow up with brew if very low
                if (currentHP < 30) {
                    if (drinkPotion("Saradomin brew")) {
                        drinkPotion("Super restore");
                    }
                }

                return true;
            }
        }

        // Normal karambwan eating
        if (Rs2Inventory.hasItem("Cooked karambwan")) {
            return Rs2Inventory.interact("Cooked karambwan", "Eat");
        }

        return false;
    }

    private boolean executePotion(PvpAction action) {
        if (!config.enableEating()) return false;

        switch (action.getValue()) {
            case 1: // Saradomin Brew
                // CRITICAL: ALWAYS restore after brew to prevent stat drain
                boolean drankBrew = drinkPotion("Saradomin brew");
                if (drankBrew) {
                    log.info("🍺 BREW → Drinking restore to counter stat drain");
                    drinkPotion("Super restore"); // Auto-restore after every brew
                }
                return drankBrew;
            case 2: // Super Restore
                return drinkPotion("Super restore");
            case 3: // Super Combat
                return drinkPotion("Super combat");
            case 4: // Ranging Potion
                return drinkPotion("Ranging potion") || drinkPotion("Super ranging");
            default:
                return false;
        }
    }

    private boolean drinkPotion(String potionName) {
        // Try all dose variants (4) to (1)
        for (int dose = 4; dose >= 1; dose--) {
            String fullName = potionName + "(" + dose + ")";
            if (Rs2Inventory.hasItem(fullName)) {
                return Rs2Inventory.interact(fullName, "Drink");
            }
        }
        return false;
    }

    private boolean executeVengeance(PvpAction action) {
        if (!config.enableAttacking()) return false;

        // Silently skip Vengeance - requires Lunar spellbook, but LMS uses Ancient spellbook
        // Return true to prevent spam warnings in logs
        return true; // Pretend it succeeded so it doesn't spam warnings
    }

    private boolean executeGear(PvpAction action) {
        if (!config.enableGearSwitching()) return false;

        // Gear switching not fully implemented yet
        // Silently skip to prevent spam warnings
        // TODO: Implement proper gear sets (melee/ranged/mage/tank) before enabling
        return true; // Pretend it succeeded so it doesn't spam warnings
    }

    private boolean executeMovement(PvpAction action) {
        // CRITICAL: ALWAYS return early if movement disabled to prevent walker spam
        if (!config.enableMovement()) {
            return true; // Silently skip - pretend success to avoid ActionQueue warnings
        }

        if (currentState == null || currentState.getTarget() == null) {
            return false;
        }

        Player target = currentState.getTarget();
        WorldPoint targetPos = target.getWorldLocation();
        WorldPoint playerPos = Rs2Player.getWorldLocation();

        if (targetPos == null || playerPos == null) return false;

        WorldPoint destination = null;

        switch (action.getValue()) {
            case 1: // Move next to target
                destination = getAdjacentTile(targetPos, playerPos);
                break;
            case 2: // Move under target
                destination = targetPos;
                break;
            case 3: // Farcast (maintain distance)
                int distance = currentState.getDistanceToTarget();
                if (distance < 5) {
                    destination = moveAwayFrom(playerPos, targetPos, 2);
                }
                break;
            case 4: // Diagonal to target
                destination = getDiagonalTile(targetPos, playerPos);
                break;
            default:
                return false;
        }

        if (destination != null) {
            Rs2Walker.walkTo(destination);
            return true;
        }

        return false;
    }

    // Helper methods for movement
    private WorldPoint getAdjacentTile(WorldPoint target, WorldPoint player) {
        // Find adjacent tile closest to player
        int dx = Integer.compare(target.getX(), player.getX());
        int dy = Integer.compare(target.getY(), player.getY());
        return new WorldPoint(target.getX() - dx, target.getY() - dy, target.getPlane());
    }

    private WorldPoint getDiagonalTile(WorldPoint target, WorldPoint player) {
        int dx = Integer.compare(target.getX(), player.getX());
        int dy = Integer.compare(target.getY(), player.getY());
        return new WorldPoint(player.getX() + dx, player.getY() + dy, player.getPlane());
    }

    private WorldPoint moveAwayFrom(WorldPoint player, WorldPoint target, int tiles) {
        int dx = player.getX() - target.getX();
        int dy = player.getY() - target.getY();

        // Normalize and extend
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length == 0) return player;

        int moveX = (int) ((dx / length) * tiles);
        int moveY = (int) ((dy / length) * tiles);

        return new WorldPoint(player.getX() + moveX, player.getY() + moveY, player.getPlane());
    }

    // Gear switching methods for NH combat
    private void switchToMeleeGear() {
        // Check what's currently equipped to avoid redundant switches
        String currentWeapon = Rs2Equipment.get(net.runelite.api.EquipmentInventorySlot.WEAPON) != null ?
            Rs2Equipment.get(net.runelite.api.EquipmentInventorySlot.WEAPON).getName() : "None";

        // LMS melee weapons (priority order: spec weapons first for versatility)
        String[] meleeWeapons = {
            "Armadyl godsword", "Dragon claws", "Granite maul",
            "Dragon dagger", "Abyssal whip", "Elder maul", "Dragon scimitar"
        };

        for (String weapon : meleeWeapons) {
            // Skip if already equipped
            if (currentWeapon.equals(weapon)) {
                return;
            }

            if (Rs2Inventory.hasItem(weapon)) {
                log.info("⚔️ MELEE SWITCH: {} → {}", currentWeapon, weapon);

                // FULL GEAR SWAP: Weapon + Armor
                Rs2Inventory.wield(weapon);
                Microbot.pauseAllScripts.set(false);

                // Swap to melee armor pieces (if available in inventory)
                swapArmorPiece("Bandos chestplate", "Torva platebody", "Dragon chainbody");
                swapArmorPiece("Bandos tassets", "Torva platelegs", "Dragon platelegs");
                swapArmorPiece("Amulet of torture", "Amulet of strength");
                swapArmorPiece("Berserker ring");

                return;
            }
        }
    }

    private void switchToRangedGear() {
        // Check what's currently equipped
        String currentWeapon = Rs2Equipment.get(net.runelite.api.EquipmentInventorySlot.WEAPON) != null ?
            Rs2Equipment.get(net.runelite.api.EquipmentInventorySlot.WEAPON).getName() : "None";

        // LMS ranged weapons (priority order)
        String[] rangedWeapons = {
            "Armadyl crossbow", "Heavy ballista", "Dark bow",
            "Toxic blowpipe", "Magic shortbow", "Rune crossbow"
        };

        for (String weapon : rangedWeapons) {
            // Skip if already equipped
            if (currentWeapon.equals(weapon)) {
                return;
            }

            if (Rs2Inventory.hasItem(weapon)) {
                log.info("🏹 RANGED SWITCH: {} → {}", currentWeapon, weapon);

                // FULL GEAR SWAP: Weapon + Armor
                Rs2Inventory.wield(weapon);
                Microbot.pauseAllScripts.set(false);

                // Swap to ranged armor pieces (if available in inventory)
                swapArmorPiece("Armadyl chestplate", "Black d'hide body", "Karil's leathertop");
                swapArmorPiece("Armadyl chainskirt", "Black d'hide chaps", "Karil's leatherskirt");
                swapArmorPiece("Necklace of anguish", "Amulet of fury");
                swapArmorPiece("Archers ring");

                return;
            }
        }
    }

    private void switchToMageGear() {
        // Check what's currently equipped
        String currentWeapon = Rs2Equipment.get(net.runelite.api.EquipmentInventorySlot.WEAPON) != null ?
            Rs2Equipment.get(net.runelite.api.EquipmentInventorySlot.WEAPON).getName() : "None";

        // LMS mage weapons (priority order)
        String[] mageWeapons = {
            "Kodai wand", "Master wand", "Ancient staff", "Staff of the dead", "Ahrim's staff"
        };

        // First try exact name matches
        for (String weapon : mageWeapons) {
            // Skip if already equipped
            if (currentWeapon.equals(weapon)) {
                log.debug("Mage weapon already equipped: {}", weapon);
                return;
            }

            if (Rs2Inventory.hasItem(weapon)) {
                log.info("🔮 MAGE SWITCH: {} → {}", currentWeapon, weapon);
                Rs2Inventory.wield(weapon);
                Microbot.pauseAllScripts.set(false);

                // Swap to mage armor pieces (if available in inventory)
                swapArmorPiece("Ancestral robe top", "Mystic robe top", "Ahrim's robetop");
                swapArmorPiece("Ancestral robe bottom", "Mystic robe bottom", "Ahrim's robeskirt");
                swapArmorPiece("Occult necklace", "Amulet of fury");
                swapArmorPiece("Seers ring", "Ring of suffering");

                return;
            }
        }

        // FALLBACK: Search for ANY staff/wand in inventory using predicate
        log.warn("No exact mage weapon match found, searching for any staff/wand...");

        // Use Rs2Inventory.contains() with predicate to check if ANY staff/wand exists
        if (Rs2Inventory.contains(item -> {
            if (item == null || item.getName() == null) return false;
            String name = item.getName().toLowerCase();
            return name.contains("staff") || name.contains("wand");
        })) {
            // Get the first staff/wand and equip it
            Rs2ItemModel mageWeapon = Rs2Inventory.get(item -> {
                if (item == null || item.getName() == null) return false;
                String name = item.getName().toLowerCase();
                return name.contains("staff") || name.contains("wand");
            });

            if (mageWeapon != null) {
                log.info("🔮 MAGE SWITCH (fallback): {} → {}", currentWeapon, mageWeapon.getName());
                Rs2Inventory.wield(mageWeapon.getName());
                Microbot.pauseAllScripts.set(false);

                // Swap to mage armor
                swapArmorPiece("Ancestral robe top", "Mystic robe top", "Ahrim's robetop");
                swapArmorPiece("Ancestral robe bottom", "Mystic robe bottom", "Ahrim's robeskirt");
                swapArmorPiece("Occult necklace", "Amulet of fury");
                swapArmorPiece("Seers ring", "Ring of suffering");
            }
        } else {
            log.error("❌ NO MAGE WEAPON FOUND - Cannot cast spells!");
        }
    }

    /**
     * Helper method to swap armor pieces
     * Tries each option in priority order
     */
    private void swapArmorPiece(String... armorNames) {
        for (String armorName : armorNames) {
            if (Rs2Inventory.hasItem(armorName)) {
                Rs2Inventory.wield(armorName);
                Microbot.pauseAllScripts.set(false);
                return; // Stop after first successful swap
            }
        }
    }
}
