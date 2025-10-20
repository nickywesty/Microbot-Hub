package net.runelite.client.plugins.microbot.pvprl.state;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pvprl.model.GameState;

/**
 * Generates action masks based on current game state
 * Action masks indicate which actions are valid/available
 * Format matches NhEnv action space
 */
@Slf4j
public class ActionMaskGenerator {

    /**
     * Generate action masks for all 12 action heads
     * Returns boolean[][] where each inner array is the mask for one action head
     *
     * Action head sizes:
     * [0] attack: 4 actions
     * [1] melee_type: 3 actions
     * [2] ranged_type: 3 actions
     * [3] mage_spell: 4 actions
     * [4] potion: 5 actions
     * [5] food: 2 actions
     * [6] karambwan: 2 actions
     * [7] veng: 2 actions
     * [8] gear: 2 actions
     * [9] movement: 5 actions
     * [10] farcast_distance: 7 actions
     * [11] prayer: 6 actions
     */
    public boolean[][] generate(GameState state) {
        boolean[][] masks = new boolean[12][];

        masks[0] = generateAttackMask(state);          // 4 actions
        masks[1] = generateMeleeTypeMask(state);       // 3 actions
        masks[2] = generateRangedTypeMask(state);      // 3 actions
        masks[3] = generateMageSpellMask(state);       // 4 actions
        masks[4] = generatePotionMask(state);          // 5 actions
        masks[5] = generateFoodMask(state);            // 2 actions
        masks[6] = generateKarambwanMask(state);       // 2 actions
        masks[7] = generateVengMask(state);            // 2 actions
        masks[8] = generateGearMask(state);            // 2 actions
        masks[9] = generateMovementMask(state);        // 5 actions
        masks[10] = generateFarcastMask(state);        // 7 actions
        masks[11] = generatePrayerMask(state);         // 6 actions

        return masks;
    }

    private boolean[] generateAttackMask(GameState state) {
        boolean[] mask = new boolean[4];
        boolean hasTarget = state.getTarget() != null;

        mask[0] = true; // No-op always available
        mask[1] = hasTarget && state.getPrayer() >= 20; // Mage attack (needs prayer for barrage)
        mask[2] = hasTarget; // Ranged attack
        mask[3] = hasTarget && state.isCanMeleeTarget(); // Melee attack (needs to be in range)

        return mask;
    }

    private boolean[] generateMeleeTypeMask(GameState state) {
        boolean[] mask = new boolean[3];
        boolean hasTarget = state.getTarget() != null;
        boolean canMelee = state.isCanMeleeTarget();

        mask[0] = true; // No melee always available
        mask[1] = hasTarget && canMelee; // Basic melee
        mask[2] = hasTarget && canMelee && state.getSpecialEnergy() >= 50; // Special attack

        return mask;
    }

    private boolean[] generateRangedTypeMask(GameState state) {
        boolean[] mask = new boolean[3];
        boolean hasTarget = state.getTarget() != null;

        mask[0] = true; // No ranged always available
        mask[1] = hasTarget; // Basic ranged
        mask[2] = hasTarget && state.getSpecialEnergy() >= 50; // Special attack

        return mask;
    }

    private boolean[] generateMageSpellMask(GameState state) {
        boolean[] mask = new boolean[4];
        boolean hasTarget = state.getTarget() != null;
        boolean hasPrayer = state.getPrayer() >= 20; // Barrage costs ~20 prayer

        mask[0] = true; // No spell always available
        mask[1] = hasTarget && hasPrayer; // Ice Barrage
        mask[2] = hasTarget && hasPrayer; // Blood Barrage
        mask[3] = hasTarget && state.getSpecialEnergy() >= 50; // Mage spec

        return mask;
    }

    private boolean[] generatePotionMask(GameState state) {
        boolean[] mask = new boolean[5];

        mask[0] = true; // No potion always available
        mask[1] = state.getBrewDoses() > 0; // Saradomin brew
        mask[2] = state.getRestoreDoses() > 0; // Super restore
        mask[3] = state.getCombatPotionDoses() > 0; // Combat potion
        mask[4] = state.getRangingPotionDoses() > 0; // Ranging potion

        return mask;
    }

    private boolean[] generateFoodMask(GameState state) {
        boolean[] mask = new boolean[2];

        mask[0] = true; // Don't eat always available
        mask[1] = state.getFoodCount() > 0; // Eat food

        return mask;
    }

    private boolean[] generateKarambwanMask(GameState state) {
        boolean[] mask = new boolean[2];

        mask[0] = true; // Don't eat always available
        mask[1] = state.getKarambwanCount() > 0; // Eat karambwan

        return mask;
    }

    private boolean[] generateVengMask(GameState state) {
        boolean[] mask = new boolean[2];
        boolean hasPrayer = state.getPrayer() >= 10; // Veng costs ~10 prayer
        boolean notOnCooldown = state.getVengCooldownTicks() == 0;

        mask[0] = true; // Don't cast always available
        mask[1] = hasPrayer && notOnCooldown; // Cast vengeance

        return mask;
    }

    private boolean[] generateGearMask(GameState state) {
        boolean[] mask = new boolean[2];

        mask[0] = true; // No gear swap always available
        mask[1] = true; // Tank gear (simplified - assume always available)

        return mask;
    }

    private boolean[] generateMovementMask(GameState state) {
        boolean[] mask = new boolean[5];
        boolean hasTarget = state.getTarget() != null;

        mask[0] = true; // Stay always available
        mask[1] = hasTarget; // Move next to target
        mask[2] = hasTarget; // Move under target
        mask[3] = hasTarget; // Farcast
        mask[4] = hasTarget; // Diagonal

        return mask;
    }

    private boolean[] generateFarcastMask(GameState state) {
        boolean[] mask = new boolean[7];
        boolean hasTarget = state.getTarget() != null;

        mask[0] = true; // No farcast always available
        // Farcast distances 2-7 tiles
        for (int i = 1; i < 7; i++) {
            mask[i] = hasTarget;
        }

        return mask;
    }

    private boolean[] generatePrayerMask(GameState state) {
        boolean[] mask = new boolean[6];
        boolean hasPrayer = state.getPrayer() > 0;

        mask[0] = true; // No prayer change always available
        mask[1] = hasPrayer; // Protect from Magic
        mask[2] = hasPrayer; // Protect from Ranged
        mask[3] = hasPrayer; // Protect from Melee
        mask[4] = hasPrayer; // Smite
        mask[5] = hasPrayer; // Redemption

        return mask;
    }
}
