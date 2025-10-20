package net.runelite.client.plugins.microbot.pvprl.model;

import lombok.Data;
import net.runelite.api.Actor;
import net.runelite.api.Player;

/**
 * Represents the current game state extracted from Microbot APIs
 * Used to build observations and action masks
 */
@Data
public class GameState {
    // Player stats
    private int health;
    private int maxHealth;
    private int prayer;
    private int maxPrayer;
    private int specialEnergy;

    // Combat stats (boosted)
    private int attackLevel;
    private int strengthLevel;
    private int defenseLevel;
    private int rangedLevel;
    private int magicLevel;

    // Base stats
    private int baseAttackLevel;
    private int baseStrengthLevel;
    private int baseDefenseLevel;
    private int baseRangedLevel;
    private int baseMagicLevel;
    private int basePrayerLevel;
    private int baseHitpointsLevel;

    // Equipment state
    private boolean usingMelee;
    private boolean usingRanged;
    private boolean usingMage;
    private boolean specWeaponEquipped;

    // Prayer state
    private boolean protectFromMelee;
    private boolean protectFromRanged;
    private boolean protectFromMagic;
    private boolean smite;
    private boolean redemption;
    private boolean piety;
    private boolean rigour;
    private boolean augury;

    // Inventory
    private int foodCount;
    private int karambwanCount;
    private int brewDoses;
    private int restoreDoses;
    private int combatPotionDoses;
    private int rangingPotionDoses;

    // Combat state
    private Player target;
    private boolean inCombat;
    private int distanceToTarget;
    private boolean canMeleeTarget;

    // Target state (if visible)
    private Integer targetHealth;
    private Integer targetMaxHealth;
    private Boolean targetUsingMelee;
    private Boolean targetUsingRanged;
    private Boolean targetUsingMage;
    private Boolean targetProtectFromMelee;
    private Boolean targetProtectFromRanged;
    private Boolean targetProtectFromMagic;

    // Timers (in game ticks)
    private int frozenTicks;
    private int targetFrozenTicks;
    private int frozenImmunityTicks;
    private int targetFrozenImmunityTicks;
    private int attackCycleTicks;
    private int foodCycleTicks;
    private int potionCycleTicks;
    private int karambwanCycleTicks;

    // Misc
    private boolean vengActive;
    private int vengCooldownTicks;
    private long currentGameTick;

    public float getHealthPercent() {
        return maxHealth > 0 ? (float) health / maxHealth : 0f;
    }

    public float getPrayerPercent() {
        return maxPrayer > 0 ? (float) prayer / maxPrayer : 0f;
    }

    public float getSpecialPercent() {
        return specialEnergy / 10f; // Special energy is stored as 0-1000
    }

    public Float getTargetHealthPercent() {
        if (targetHealth == null || targetMaxHealth == null) return null;
        return targetMaxHealth > 0 ? (float) targetHealth / targetMaxHealth : 0f;
    }
}
