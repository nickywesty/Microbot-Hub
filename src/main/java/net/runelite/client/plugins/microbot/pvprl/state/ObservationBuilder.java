package net.runelite.client.plugins.microbot.pvprl.state;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pvprl.model.GameState;

/**
 * Builds observation vectors from GameState
 * Maps to NhEnv observation format (176 observations)
 *
 * The trained model expects exactly 176 observations
 * For production, you would implement all observations from NhEnv.json
 */
@Slf4j
public class ObservationBuilder {

    // Observation vector size - MUST MATCH TRAINED MODEL (176)
    private static final int OBS_SIZE = 176;

    /**
     * Build observation vector from game state
     * Returns float array matching NhEnv observation space
     */
    public float[] build(GameState state) {
        float[] obs = new float[OBS_SIZE];
        int idx = 0;

        try {
            // Equipment state (3 binary flags)
            obs[idx++] = state.isUsingMelee() ? 1.0f : 0.0f;
            obs[idx++] = state.isUsingRanged() ? 1.0f : 0.0f;
            obs[idx++] = state.isUsingMage() ? 1.0f : 0.0f;
            obs[idx++] = state.isSpecWeaponEquipped() ? 1.0f : 0.0f;

            // Special attack energy (normalized 0-1)
            obs[idx++] = state.getSpecialPercent() / 100.0f;

            // Player prayers (binary flags)
            obs[idx++] = state.isProtectFromMelee() ? 1.0f : 0.0f;
            obs[idx++] = state.isProtectFromRanged() ? 1.0f : 0.0f;
            obs[idx++] = state.isProtectFromMagic() ? 1.0f : 0.0f;
            obs[idx++] = state.isSmite() ? 1.0f : 0.0f;
            obs[idx++] = state.isRedemption() ? 1.0f : 0.0f;

            // Player health (normalized 0-1)
            obs[idx++] = state.getHealthPercent();

            // Target health (normalized 0-1, or 0 if unknown)
            Float targetHP = state.getTargetHealthPercent();
            obs[idx++] = targetHP != null ? targetHP : 0.0f;

            // Target equipment state (binary flags or 0 if unknown)
            obs[idx++] = state.getTargetUsingMelee() != null && state.getTargetUsingMelee() ? 1.0f : 0.0f;
            obs[idx++] = state.getTargetUsingRanged() != null && state.getTargetUsingRanged() ? 1.0f : 0.0f;
            obs[idx++] = state.getTargetUsingMage() != null && state.getTargetUsingMage() ? 1.0f : 0.0f;
            obs[idx++] = 0.0f; // target spec equipped (unknown)

            // Target prayers (binary flags or 0 if unknown)
            obs[idx++] = state.getTargetProtectFromMelee() != null && state.getTargetProtectFromMelee() ? 1.0f : 0.0f;
            obs[idx++] = state.getTargetProtectFromRanged() != null && state.getTargetProtectFromRanged() ? 1.0f : 0.0f;
            obs[idx++] = state.getTargetProtectFromMagic() != null && state.getTargetProtectFromMagic() ? 1.0f : 0.0f;
            obs[idx++] = 0.0f; // target smite (unknown)
            obs[idx++] = 0.0f; // target redemption (unknown)
            obs[idx++] = 0.0f; // target special percent (unknown)

            // Potion doses (normalized by typical max, e.g., 16 doses)
            obs[idx++] = Math.min(state.getRangingPotionDoses() / 16.0f, 1.0f);
            obs[idx++] = Math.min(state.getCombatPotionDoses() / 16.0f, 1.0f);
            obs[idx++] = Math.min(state.getRestoreDoses() / 16.0f, 1.0f);
            obs[idx++] = Math.min(state.getBrewDoses() / 16.0f, 1.0f);

            // Food counts (normalized by typical max, e.g., 10 food)
            obs[idx++] = Math.min(state.getFoodCount() / 10.0f, 1.0f);
            obs[idx++] = Math.min(state.getKarambwanCount() / 5.0f, 1.0f);

            // Prayer points (normalized 0-1)
            obs[idx++] = state.getPrayerPercent();

            // Freeze timers (normalized by typical max freeze, e.g., 33 ticks for ice barrage)
            obs[idx++] = Math.min(state.getFrozenTicks() / 33.0f, 1.0f);
            obs[idx++] = Math.min(state.getTargetFrozenTicks() / 33.0f, 1.0f);
            obs[idx++] = Math.min(state.getFrozenImmunityTicks() / 33.0f, 1.0f);
            obs[idx++] = Math.min(state.getTargetFrozenImmunityTicks() / 33.0f, 1.0f);

            // Combat position
            obs[idx++] = state.isCanMeleeTarget() ? 1.0f : 0.0f;

            // Combat stats (normalized by typical max boost of ~25%)
            obs[idx++] = normalizeBoost(state.getStrengthLevel(), state.getBaseStrengthLevel());
            obs[idx++] = normalizeBoost(state.getAttackLevel(), state.getBaseAttackLevel());
            obs[idx++] = normalizeBoost(state.getDefenseLevel(), state.getBaseDefenseLevel());
            obs[idx++] = normalizeBoost(state.getRangedLevel(), state.getBaseRangedLevel());
            obs[idx++] = normalizeBoost(state.getMagicLevel(), state.getBaseMagicLevel());

            // Cooldown timers (normalized by typical cooldown)
            obs[idx++] = Math.min(state.getAttackCycleTicks() / 6.0f, 1.0f);
            obs[idx++] = Math.min(state.getFoodCycleTicks() / 3.0f, 1.0f);
            obs[idx++] = Math.min(state.getPotionCycleTicks() / 3.0f, 1.0f);
            obs[idx++] = Math.min(state.getKarambwanCycleTicks() / 2.0f, 1.0f);

            // Combat state
            obs[idx++] = 0.0f; // pending damage on target (unknown)
            obs[idx++] = 0.0f; // ticks until hit on target (unknown)
            obs[idx++] = 0.0f; // ticks until hit on player (unknown)
            obs[idx++] = 0.0f; // player just attacked (unknown)
            obs[idx++] = 0.0f; // target just attacked (unknown)
            obs[idx++] = 0.0f; // damage on player this tick (unknown)
            obs[idx++] = 0.0f; // damage on target this tick (unknown)
            obs[idx++] = state.isInCombat() ? 1.0f : 0.0f;
            obs[idx++] = 0.0f; // player is moving (unknown)
            obs[idx++] = 0.0f; // target is moving (unknown)
            obs[idx++] = 0.0f; // player has PID (unknown)

            // Spell availability (simplified - assume available if prayer points > 0)
            obs[idx++] = state.getPrayer() > 20 ? 1.0f : 0.0f; // ice barrage usable
            obs[idx++] = state.getPrayer() > 20 ? 1.0f : 0.0f; // blood barrage usable

            // Distances (normalized by max distance, e.g., 15 tiles)
            obs[idx++] = Math.min(state.getDistanceToTarget() / 15.0f, 1.0f);
            obs[idx++] = 0.0f; // destination to target distance (unknown)
            obs[idx++] = 0.0f; // player to destination distance (unknown)

            // Prayer correctness
            obs[idx++] = 0.0f; // player prayer correct (unknown)
            obs[idx++] = 0.0f; // target prayer correct (unknown)

            // Damage tracking
            obs[idx++] = 0.0f; // total damage dealt scale (unknown)
            obs[idx++] = 0.0f; // target attack confidence (unknown)

            // Hit percentages (unknown without tracking)
            obs[idx++] = 0.0f; // target melee hit percent
            obs[idx++] = 0.0f; // target magic hit percent
            obs[idx++] = 0.0f; // target ranged hit percent
            obs[idx++] = 0.0f; // player melee hit percent
            obs[idx++] = 0.0f; // player magic hit percent
            obs[idx++] = 0.0f; // player ranged hit percent

            // Prayer tracking (unknown without history)
            obs[idx++] = 0.0f; // target number of hits off prayer
            obs[idx++] = 0.0f; // target prayer confidence
            obs[idx++] = 0.0f; // target magic prayer percent
            obs[idx++] = 0.0f; // target ranged prayer percent
            obs[idx++] = 0.0f; // target melee prayer percent
            obs[idx++] = 0.0f; // player magic prayer percent
            obs[idx++] = 0.0f; // player ranged prayer percent
            obs[idx++] = 0.0f; // player melee prayer percent
            obs[idx++] = 0.0f; // target correct pray percent

            // Recent stats (last 5 actions - unknown without tracking)
            for (int i = 0; i < 15; i++) {
                obs[idx++] = 0.0f;
            }

            // Base stats (normalized by max level 99)
            obs[idx++] = state.getBaseAttackLevel() / 99.0f;
            obs[idx++] = state.getBaseStrengthLevel() / 99.0f;
            obs[idx++] = state.getBaseDefenseLevel() / 99.0f;
            obs[idx++] = state.getBaseRangedLevel() / 99.0f;
            obs[idx++] = state.getBaseMagicLevel() / 99.0f;
            obs[idx++] = state.getBasePrayerLevel() / 99.0f;
            obs[idx++] = state.getBaseHitpointsLevel() / 99.0f;

            // Equipment constants (simplified - would need actual gear detection)
            for (int i = 0; i < 20; i++) {
                obs[idx++] = 0.0f; // Various equipment flags
            }

            // Gear stats (simplified)
            for (int i = 0; i < 25; i++) {
                obs[idx++] = 0.5f; // Magic/ranged/melee accuracy and strength
            }

            // Fill remaining with zeros
            while (idx < OBS_SIZE) {
                obs[idx++] = 0.0f;
            }

        } catch (Exception e) {
            log.error("Error building observation", e);
        }

        return obs;
    }

    /**
     * Normalize stat boost (current / base, centered around 1.0)
     */
    private float normalizeBoost(int current, int base) {
        if (base == 0) return 1.0f;
        return current / (float) base;
    }

    /**
     * Build frame-stacked observations
     * For now, just returns single frame repeated
     * In production, you'd maintain history
     */
    public float[][] buildFrameStacked(GameState state, int frameCount) {
        float[] singleFrame = build(state);
        float[][] stacked = new float[frameCount][];

        for (int i = 0; i < frameCount; i++) {
            stacked[i] = singleFrame.clone();
        }

        return stacked;
    }
}
