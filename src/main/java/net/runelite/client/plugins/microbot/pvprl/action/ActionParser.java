package net.runelite.client.plugins.microbot.pvprl.action;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.pvprl.model.ActionPriority;
import net.runelite.client.plugins.microbot.pvprl.model.ActionType;
import net.runelite.client.plugins.microbot.pvprl.model.PvpAction;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses AI action vectors into executable PvpAction objects
 * Maps from NhEnv action space (11 action heads) to game actions
 */
@Slf4j
public class ActionParser {

    /**
     * Parse action vector from AI into list of PvpActions
     * Action vector format from NhEnv.json:
     * [0] attack (0-3): no-op, mage, ranged, melee
     * [1] melee_type (0-2): no, basic, spec
     * [2] ranged_type (0-2): no, basic, spec
     * [3] mage_spell (0-3): no, ice, blood, spec
     * [4] potion (0-4): no, brew, restore, combat, ranged
     * [5] food (0-1): no, eat
     * [6] karambwan (0-1): no, eat
     * [7] veng (0-1): no, cast
     * [8] gear (0-1): no, tank gear
     * [9] movement (0-4): stay, next to, under, farcast, diagonal
     * [10] farcast_distance (0-6): no-op, 2-7 tiles
     * [11] prayer (0-5): no-op, mage, ranged, melee, smite, redemption
     */
    public List<PvpAction> parse(int[] actionVector) {
        if (actionVector == null || actionVector.length != 12) {
            log.error("Invalid action vector length: {}", actionVector == null ? "null" : actionVector.length);
            return List.of();
        }

        List<PvpAction> actions = new ArrayList<>();

        // Parse each action head
        parseAttack(actionVector, actions);
        parsePrayer(actionVector, actions);
        parseFood(actionVector, actions);
        parseKarambwan(actionVector, actions);
        parsePotion(actionVector, actions);
        parseVengeance(actionVector, actions);
        parseGear(actionVector, actions);
        parseMovement(actionVector, actions);

        return actions;
    }

    private void parseAttack(int[] vec, List<PvpAction> actions) {
        int attackStyle = vec[0]; // 0=no-op, 1=mage, 2=ranged, 3=melee

        if (attackStyle == 0) return; // No attack

        switch (attackStyle) {
            case 1: // Mage attack
                int mageSpell = vec[3]; // 0=no, 1=ice, 2=blood, 3=spec
                if (mageSpell == 1) {
                    actions.add(new PvpAction(ActionType.MAGE_SPELL, 1,
                        ActionPriority.MEDIUM, "Cast Ice Barrage"));
                } else if (mageSpell == 2) {
                    actions.add(new PvpAction(ActionType.MAGE_SPELL, 2,
                        ActionPriority.MEDIUM, "Cast Blood Barrage"));
                } else if (mageSpell == 3) {
                    actions.add(new PvpAction(ActionType.MAGE_SPELL, 3,
                        ActionPriority.HIGH, "Cast Mage Special"));
                }
                break;

            case 2: // Ranged attack
                int rangedType = vec[2]; // 0=no, 1=basic, 2=spec
                if (rangedType == 1) {
                    actions.add(new PvpAction(ActionType.RANGED_TYPE, 1,
                        ActionPriority.MEDIUM, "Ranged Attack"));
                } else if (rangedType == 2) {
                    actions.add(new PvpAction(ActionType.RANGED_TYPE, 2,
                        ActionPriority.HIGH, "Ranged Special Attack"));
                }
                break;

            case 3: // Melee attack
                int meleeType = vec[1]; // 0=no, 1=basic, 2=spec
                if (meleeType == 1) {
                    actions.add(new PvpAction(ActionType.MELEE_TYPE, 1,
                        ActionPriority.MEDIUM, "Melee Attack"));
                } else if (meleeType == 2) {
                    actions.add(new PvpAction(ActionType.MELEE_TYPE, 2,
                        ActionPriority.HIGH, "Melee Special Attack"));
                }
                break;
        }
    }

    private void parsePrayer(int[] vec, List<PvpAction> actions) {
        int prayer = vec[11]; // 0=no-op, 1=mage, 2=ranged, 3=melee, 4=smite, 5=redemption

        if (prayer == 0) return; // No prayer change

        String desc = "";
        switch (prayer) {
            case 1:
                desc = "Protect from Magic";
                break;
            case 2:
                desc = "Protect from Ranged";
                break;
            case 3:
                desc = "Protect from Melee";
                break;
            case 4:
                desc = "Smite";
                break;
            case 5:
                desc = "Redemption";
                break;
        }

        actions.add(new PvpAction(ActionType.PRAYER, prayer,
            ActionPriority.CRITICAL, desc));
    }

    private void parseFood(int[] vec, List<PvpAction> actions) {
        int food = vec[5]; // 0=no, 1=eat

        if (food == 1) {
            actions.add(new PvpAction(ActionType.FOOD, 1,
                ActionPriority.CRITICAL, "Eat Food"));
        }
    }

    private void parseKarambwan(int[] vec, List<PvpAction> actions) {
        int karambwan = vec[6]; // 0=no, 1=eat

        if (karambwan == 1) {
            actions.add(new PvpAction(ActionType.KARAMBWAN, 1,
                ActionPriority.HIGH, "Eat Karambwan"));
        }
    }

    private void parsePotion(int[] vec, List<PvpAction> actions) {
        int potion = vec[4]; // 0=no, 1=brew, 2=restore, 3=combat, 4=ranged

        if (potion == 0) return;

        String desc = "";
        ActionPriority priority = ActionPriority.HIGH;

        switch (potion) {
            case 1:
                desc = "Drink Saradomin Brew";
                priority = ActionPriority.CRITICAL; // Brew is defensive
                break;
            case 2:
                desc = "Drink Super Restore";
                priority = ActionPriority.HIGH;
                break;
            case 3:
                desc = "Drink Combat Potion";
                priority = ActionPriority.MEDIUM;
                break;
            case 4:
                desc = "Drink Ranging Potion";
                priority = ActionPriority.MEDIUM;
                break;
        }

        actions.add(new PvpAction(ActionType.POTION, potion, priority, desc));
    }

    private void parseVengeance(int[] vec, List<PvpAction> actions) {
        int veng = vec[7]; // 0=no, 1=cast

        if (veng == 1) {
            actions.add(new PvpAction(ActionType.VENGEANCE, 1,
                ActionPriority.MEDIUM, "Cast Vengeance"));
        }
    }

    private void parseGear(int[] vec, List<PvpAction> actions) {
        int gear = vec[8]; // 0=no, 1=tank gear

        if (gear == 1) {
            actions.add(new PvpAction(ActionType.GEAR, 1,
                ActionPriority.MEDIUM, "Switch to Tank Gear"));
        }
    }

    private void parseMovement(int[] vec, List<PvpAction> actions) {
        int movement = vec[9]; // 0=stay, 1=next to, 2=under, 3=farcast, 4=diagonal

        if (movement == 0) return; // No movement

        String desc = "";
        switch (movement) {
            case 1:
                desc = "Move Next to Target";
                break;
            case 2:
                desc = "Move Under Target";
                break;
            case 3:
                int farcastDist = vec[10]; // 0=no-op, 1-6=2-7 tiles
                if (farcastDist > 0) {
                    desc = "Farcast " + (farcastDist + 1) + " tiles";
                } else {
                    desc = "Move to Farcast";
                }
                break;
            case 4:
                desc = "Move Diagonal to Target";
                break;
        }

        actions.add(new PvpAction(ActionType.MOVEMENT, movement,
            ActionPriority.LOW, desc));
    }
}
