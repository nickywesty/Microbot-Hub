package net.runelite.client.plugins.microbot.pvprl.state;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.pvprl.model.GameState;
import net.runelite.client.plugins.microbot.util.combat.Rs2Combat;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;
import net.runelite.client.plugins.microbot.util.prayer.Rs2Prayer;
import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;

/**
 * Extracts current game state from Microbot APIs
 * Converts RuneLite/OSRS game state into our GameState model
 */
@Slf4j
public class GameStateExtractor {
    private long tickCounter = 0;

    // Timer tracking (in game ticks)
    private int lastAttackTick = 0;
    private int lastFoodTick = 0;
    private int lastPotionTick = 0;
    private int lastKarambwanTick = 0;

    /**
     * Extract complete game state
     */
    public GameState extract() {
        GameState state = new GameState();

        try {
            tickCounter++;
            state.setCurrentGameTick(tickCounter);

            extractPlayerStats(state);
            extractEquipmentState(state);
            extractPrayerState(state);
            extractInventory(state);
            extractCombatState(state);
            extractTimers(state);

        } catch (Exception e) {
            log.error("Error extracting game state", e);
        }

        return state;
    }

    private void extractPlayerStats(GameState state) {
        // Health
        state.setHealth(Rs2Player.getBoostedSkillLevel(net.runelite.api.Skill.HITPOINTS));
        state.setMaxHealth(Rs2Player.getRealSkillLevel(net.runelite.api.Skill.HITPOINTS));

        // Prayer
        state.setPrayer(Rs2Player.getBoostedSkillLevel(net.runelite.api.Skill.PRAYER));
        state.setMaxPrayer(Rs2Player.getRealSkillLevel(net.runelite.api.Skill.PRAYER));

        // Special attack energy (0-100)
        state.setSpecialEnergy(Microbot.getClient().getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT) / 10);

        // Boosted stats
        state.setAttackLevel(Rs2Player.getBoostedSkillLevel(Skill.ATTACK));
        state.setStrengthLevel(Rs2Player.getBoostedSkillLevel(Skill.STRENGTH));
        state.setDefenseLevel(Rs2Player.getBoostedSkillLevel(Skill.DEFENCE));
        state.setRangedLevel(Rs2Player.getBoostedSkillLevel(Skill.RANGED));
        state.setMagicLevel(Rs2Player.getBoostedSkillLevel(Skill.MAGIC));

        // Base stats
        state.setBaseAttackLevel(Rs2Player.getRealSkillLevel(Skill.ATTACK));
        state.setBaseStrengthLevel(Rs2Player.getRealSkillLevel(Skill.STRENGTH));
        state.setBaseDefenseLevel(Rs2Player.getRealSkillLevel(Skill.DEFENCE));
        state.setBaseRangedLevel(Rs2Player.getRealSkillLevel(Skill.RANGED));
        state.setBaseMagicLevel(Rs2Player.getRealSkillLevel(Skill.MAGIC));
        state.setBasePrayerLevel(Rs2Player.getRealSkillLevel(Skill.PRAYER));
        state.setBaseHitpointsLevel(Rs2Player.getRealSkillLevel(Skill.HITPOINTS));
    }

    private void extractEquipmentState(GameState state) {
        // Determine combat style based on equipped weapon
        Player player = Microbot.getClient().getLocalPlayer();
        if (player == null) return;

        // Check weapon type
        boolean hasMeleeWeapon = Rs2Equipment.isWearing("Abyssal whip") ||
                                Rs2Equipment.isWearing("Dragon scimitar") ||
                                Rs2Equipment.isWearing("Dragon claws") ||
                                Rs2Equipment.isWearing("Armadyl godsword");

        boolean hasRangedWeapon = Rs2Equipment.isWearing("Rune crossbow") ||
                                 Rs2Equipment.isWearing("Dragon crossbow") ||
                                 Rs2Equipment.isWearing("Toxic blowpipe") ||
                                 Rs2Equipment.isWearing("Dark bow");

        boolean hasMageWeapon = Rs2Equipment.isWearing("Staff") ||
                               Rs2Equipment.isWearing("Trident") ||
                               Rs2Equipment.isWearing("Wand");

        state.setUsingMelee(hasMeleeWeapon);
        state.setUsingRanged(hasRangedWeapon);
        state.setUsingMage(hasMageWeapon || (!hasMeleeWeapon && !hasRangedWeapon)); // Default to mage

        // Check for special attack weapons
        state.setSpecWeaponEquipped(
            Rs2Equipment.isWearing("Dragon claws") ||
            Rs2Equipment.isWearing("Armadyl godsword") ||
            Rs2Equipment.isWearing("Dragon dagger") ||
            Rs2Equipment.isWearing("Dark bow")
        );
    }

    private void extractPrayerState(GameState state) {
        state.setProtectFromMelee(Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MELEE));
        state.setProtectFromRanged(Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_RANGE));
        state.setProtectFromMagic(Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PROTECT_MAGIC));
        state.setSmite(Rs2Prayer.isPrayerActive(Rs2PrayerEnum.SMITE));
        state.setRedemption(Rs2Prayer.isPrayerActive(Rs2PrayerEnum.REDEMPTION));

        // Offensive prayers
        state.setPiety(Rs2Prayer.isPrayerActive(Rs2PrayerEnum.PIETY));
        state.setRigour(Rs2Prayer.isPrayerActive(Rs2PrayerEnum.RIGOUR));
        state.setAugury(Rs2Prayer.isPrayerActive(Rs2PrayerEnum.AUGURY));
    }

    private void extractInventory(GameState state) {
        // Food count (common PvP foods)
        int foodCount = 0;
        foodCount += Rs2Inventory.count("Manta ray");
        foodCount += Rs2Inventory.count("Shark");
        foodCount += Rs2Inventory.count("Anglerfish");
        foodCount += Rs2Inventory.count("Dark crab");
        foodCount += Rs2Inventory.count("Sea turtle");
        state.setFoodCount(foodCount);

        // Karambwan
        state.setKarambwanCount(Rs2Inventory.count("Cooked karambwan"));

        // Potions (count doses)
        state.setBrewDoses(countPotionDoses("Saradomin brew"));
        state.setRestoreDoses(countPotionDoses("Super restore"));
        state.setCombatPotionDoses(countPotionDoses("Super combat"));
        state.setRangingPotionDoses(countPotionDoses("Ranging potion") + countPotionDoses("Super ranging"));
    }

    private int countPotionDoses(String baseName) {
        int total = 0;
        for (int dose = 1; dose <= 4; dose++) {
            String name = baseName + "(" + dose + ")";
            total += Rs2Inventory.count(name) * dose;
        }
        return total;
    }

    private void extractCombatState(GameState state) {
        Player player = Microbot.getClient().getLocalPlayer();
        if (player == null) return;

        // Get combat target
        Actor interacting = player.getInteracting();
        if (interacting instanceof Player) {
            Player target = (Player) interacting;
            state.setTarget(target);
            state.setInCombat(true);

            // Distance to target
            WorldPoint playerPos = player.getWorldLocation();
            WorldPoint targetPos = target.getWorldLocation();
            if (playerPos != null && targetPos != null) {
                int distance = playerPos.distanceTo(targetPos);
                state.setDistanceToTarget(distance);
                state.setCanMeleeTarget(distance <= 1);
            }

            // Target stats (if visible)
            extractTargetState(state, target);
        } else {
            state.setTarget(null);
            state.setInCombat(false);
            state.setDistanceToTarget(0);
            state.setCanMeleeTarget(false);
        }
    }

    private void extractTargetState(GameState state, Player target) {
        try {
            // Target health (if overhead visible)
            // Note: Can't always see opponent's exact HP in real game
            // This would need packet inspection or overhead reading
            state.setTargetHealth(null); // Unknown in real game
            state.setTargetMaxHealth(null);

            // Target equipment (visible)
            PlayerComposition composition = target.getPlayerComposition();
            if (composition != null) {
                // Try to detect target's combat style from equipment
                // This is simplified - you'd need item ID mapping
                state.setTargetUsingMelee(false);
                state.setTargetUsingRanged(false);
                state.setTargetUsingMage(false);
            }

            // Target prayers (visible from animation/overhead)
            // This requires overhead prayer detection
            state.setTargetProtectFromMelee(null); // Unknown without overhead detection
            state.setTargetProtectFromRanged(null);
            state.setTargetProtectFromMagic(null);

        } catch (Exception e) {
            log.debug("Could not extract target state: {}", e.getMessage());
        }
    }

    private void extractTimers(GameState state) {
        // Track cooldowns in game ticks
        int currentTick = (int) tickCounter;

        // Attack cycle (assuming 4-tick weapons, adjust based on weapon)
        int ticksSinceAttack = currentTick - lastAttackTick;
        state.setAttackCycleTicks(Math.max(0, 4 - ticksSinceAttack));

        // Food delay (3 ticks)
        int ticksSinceFood = currentTick - lastFoodTick;
        state.setFoodCycleTicks(Math.max(0, 3 - ticksSinceFood));

        // Potion delay (3 ticks)
        int ticksSincePot = currentTick - lastPotionTick;
        state.setPotionCycleTicks(Math.max(0, 3 - ticksSincePot));

        // Karambwan delay (2 ticks)
        int ticksSinceKaram = currentTick - lastKarambwanTick;
        state.setKarambwanCycleTicks(Math.max(0, 2 - ticksSinceKaram));

        // Freeze timers (would need varp/varbit tracking)
        state.setFrozenTicks(0); // TODO: Track from varbits
        state.setTargetFrozenTicks(0);
        state.setFrozenImmunityTicks(0);
        state.setTargetFrozenImmunityTicks(0);

        // Vengeance
        state.setVengActive(false); // TODO: Track from varbit
        state.setVengCooldownTicks(0);
    }

    /**
     * Notify that an attack occurred (for timer tracking)
     */
    public void notifyAttack() {
        lastAttackTick = (int) tickCounter;
    }

    /**
     * Notify that food was eaten
     */
    public void notifyFood() {
        lastFoodTick = (int) tickCounter;
    }

    /**
     * Notify that a potion was drunk
     */
    public void notifyPotion() {
        lastPotionTick = (int) tickCounter;
    }

    /**
     * Notify that karambwan was eaten
     */
    public void notifyKarambwan() {
        lastKarambwanTick = (int) tickCounter;
    }
}
