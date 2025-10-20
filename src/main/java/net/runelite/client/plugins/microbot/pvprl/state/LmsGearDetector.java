package net.runelite.client.plugins.microbot.pvprl.state;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.api.EquipmentInventorySlot;

/**
 * Detects LMS gear loadout and provides combat style information
 *
 * LMS gives you one of several gear sets:
 * - Melee (AGS, Dragon Claws, Ballista)
 * - Range (ACB, Dark Bow, Claws)
 * - Mage (Ancient Staff, Mystic, Claws)
 * - Tribrid (All styles available)
 */
@Slf4j
public class LmsGearDetector {

    /**
     * Detected gear configuration
     */
    public static class LmsGearInfo {
        public boolean hasMeleeWeapon;
        public boolean hasRangedWeapon;
        public boolean hasMageWeapon;
        public boolean hasSpecWeapon;

        public String meleeWeapon;
        public String rangedWeapon;
        public String mageWeapon;
        public String specWeapon;

        public boolean hasMeleeArmor;
        public boolean hasRangedArmor;
        public boolean hasMageArmor;

        public boolean isTribrid() {
            return hasMeleeWeapon && hasRangedWeapon && hasMageWeapon;
        }

        public boolean isPureStyle() {
            int styleCount = 0;
            if (hasMeleeWeapon) styleCount++;
            if (hasRangedWeapon) styleCount++;
            if (hasMageWeapon) styleCount++;
            return styleCount == 1;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("LMS Gear: ");
            if (isTribrid()) {
                sb.append("TRIBRID");
            } else if (hasMeleeWeapon) {
                sb.append("MELEE");
            } else if (hasRangedWeapon) {
                sb.append("RANGED");
            } else if (hasMageWeapon) {
                sb.append("MAGE");
            } else {
                sb.append("UNKNOWN");
            }

            if (hasSpecWeapon) {
                sb.append(" (Spec: ").append(specWeapon).append(")");
            }

            return sb.toString();
        }
    }

    /**
     * Detect current LMS gear configuration
     */
    public static LmsGearInfo detectGear() {
        LmsGearInfo info = new LmsGearInfo();

        try {
            // Check weapon slot
            detectWeapon(info);

            // Check armor slots
            detectArmor(info);

            log.info("Detected LMS gear: {}", info);

        } catch (Exception e) {
            log.error("Error detecting LMS gear", e);
        }

        return info;
    }

    private static void detectWeapon(LmsGearInfo info) {
        // Check equipped weapon first
        checkEquippedWeapon(info);

        // Then check inventory for additional weapons (for tribrid)
        checkInventoryWeapons(info);
    }

    private static void checkEquippedWeapon(LmsGearInfo info) {
        // COMPLETE LMS MELEE WEAPONS
        if (Rs2Equipment.isWearing("Armadyl godsword")) {
            info.hasMeleeWeapon = true;
            info.hasSpecWeapon = true;
            info.meleeWeapon = "Armadyl godsword";
            info.specWeapon = "Armadyl godsword";
        } else if (Rs2Equipment.isWearing("Dragon claws")) {
            info.hasMeleeWeapon = true;
            info.hasSpecWeapon = true;
            info.meleeWeapon = "Dragon claws";
            info.specWeapon = "Dragon claws";
        } else if (Rs2Equipment.isWearing("Abyssal whip")) {
            info.hasMeleeWeapon = true;
            info.meleeWeapon = "Abyssal whip";
        } else if (Rs2Equipment.isWearing("Elder maul")) {
            info.hasMeleeWeapon = true;
            info.meleeWeapon = "Elder maul";
        } else if (Rs2Equipment.isWearing("Dragon scimitar")) {
            info.hasMeleeWeapon = true;
            info.meleeWeapon = "Dragon scimitar";
        } else if (Rs2Equipment.isWearing("Dragon dagger")) {
            info.hasMeleeWeapon = true;
            info.hasSpecWeapon = true;
            info.meleeWeapon = "Dragon dagger";
            info.specWeapon = "Dragon dagger";
        } else if (Rs2Equipment.isWearing("Granite maul")) {
            info.hasMeleeWeapon = true;
            info.hasSpecWeapon = true;
            info.meleeWeapon = "Granite maul";
            info.specWeapon = "Granite maul";
        }

        // COMPLETE LMS RANGED WEAPONS
        if (Rs2Equipment.isWearing("Armadyl crossbow")) {
            info.hasRangedWeapon = true;
            info.rangedWeapon = "Armadyl crossbow";
        } else if (Rs2Equipment.isWearing("Rune crossbow")) {
            info.hasRangedWeapon = true;
            info.rangedWeapon = "Rune crossbow";
        } else if (Rs2Equipment.isWearing("Dark bow")) {
            info.hasRangedWeapon = true;
            info.hasSpecWeapon = true;
            info.rangedWeapon = "Dark bow";
            info.specWeapon = "Dark bow";
        } else if (Rs2Equipment.isWearing("Heavy ballista")) {
            info.hasRangedWeapon = true;
            info.hasSpecWeapon = true;
            info.rangedWeapon = "Heavy ballista";
            info.specWeapon = "Heavy ballista";
        } else if (Rs2Equipment.isWearing("Toxic blowpipe")) {
            info.hasRangedWeapon = true;
            info.hasSpecWeapon = true;
            info.rangedWeapon = "Toxic blowpipe";
            info.specWeapon = "Toxic blowpipe";
        } else if (Rs2Equipment.isWearing("Magic shortbow")) {
            info.hasRangedWeapon = true;
            info.hasSpecWeapon = true;
            info.rangedWeapon = "Magic shortbow";
            info.specWeapon = "Magic shortbow";
        }

        // COMPLETE LMS MAGE WEAPONS
        if (Rs2Equipment.isWearing("Staff of the dead")) {
            info.hasMageWeapon = true;
            info.hasSpecWeapon = true;
            info.mageWeapon = "Staff of the dead";
            info.specWeapon = "Staff of the dead";
        } else if (Rs2Equipment.isWearing("Ancient staff")) {
            info.hasMageWeapon = true;
            info.mageWeapon = "Ancient staff";
        } else if (Rs2Equipment.isWearing("Master wand")) {
            info.hasMageWeapon = true;
            info.mageWeapon = "Master wand";
        } else if (Rs2Equipment.isWearing("Kodai wand")) {
            info.hasMageWeapon = true;
            info.mageWeapon = "Kodai wand";
        }

        // Check if weapon slot contains any staff (fallback for tribrid detection)
        if (!info.hasMageWeapon) {
            if (Rs2Equipment.get(EquipmentInventorySlot.WEAPON) != null) {
                String weaponName = Rs2Equipment.get(EquipmentInventorySlot.WEAPON).getName();
                if (weaponName != null && (weaponName.contains("staff") || weaponName.contains("Staff") ||
                                           weaponName.contains("wand") || weaponName.contains("Wand"))) {
                    info.hasMageWeapon = true;
                    info.mageWeapon = weaponName;
                }
            }
        }
    }

    private static void checkInventoryWeapons(LmsGearInfo info) {
        // COMPLETE LMS WEAPON LIST (as of 2025)
        // Source: https://oldschool.runescape.wiki/w/Last_Man_Standing

        // Check for melee weapons in inventory
        if (!info.hasMeleeWeapon) {
            if (Rs2Inventory.hasItem("Abyssal whip") ||
                Rs2Inventory.hasItem("Elder maul") ||
                Rs2Inventory.hasItem("Dragon scimitar") ||
                Rs2Inventory.hasItem("Dragon dagger") ||
                Rs2Inventory.hasItem("Dragon claws") ||
                Rs2Inventory.hasItem("Armadyl godsword") ||
                Rs2Inventory.hasItem("Granite maul") ||
                Rs2Inventory.hasItem("Dragon defender")) {
                info.hasMeleeWeapon = true;
                info.meleeWeapon = "Melee weapon in inventory";
            }
        }

        // Check for ranged weapons in inventory
        if (!info.hasRangedWeapon) {
            if (Rs2Inventory.hasItem("Rune crossbow") ||
                Rs2Inventory.hasItem("Armadyl crossbow") ||
                Rs2Inventory.hasItem("Dark bow") ||
                Rs2Inventory.hasItem("Heavy ballista") ||
                Rs2Inventory.hasItem("Magic shortbow") ||
                Rs2Inventory.hasItem("Toxic blowpipe") ||
                Rs2Inventory.contains("Dragon javelin")) { // Ballista ammo
                info.hasRangedWeapon = true;
                info.rangedWeapon = "Ranged weapon in inventory";
            }
        }

        // Check for mage weapons in inventory (or rune pouch for casting)
        if (!info.hasMageWeapon) {
            if (Rs2Inventory.hasItem("Ancient staff") ||
                Rs2Inventory.hasItem("Master wand") ||
                Rs2Inventory.hasItem("Kodai wand") ||
                Rs2Inventory.hasItem("Staff of the dead") ||
                Rs2Inventory.hasItem("Occult necklace") || // Indicates mage setup
                Rs2Inventory.hasItem("Rune pouch") || // LMS rune pouch for barrage
                Rs2Inventory.contains("pouch")) { // Catch-all for any pouch variant
                info.hasMageWeapon = true;
                info.mageWeapon = "Mage weapon/runes in inventory";
            }
        }

        // Alternative: If we have magic armor equipped, assume we can cast spells
        if (!info.hasMageWeapon) {
            if (Rs2Equipment.isWearing("Mystic") || // Any mystic piece
                Rs2Equipment.isWearing("Ahrim") ||  // Any ahrims piece
                Rs2Inventory.hasItem("Mystic") ||   // Mystic in inventory for switching
                Rs2Inventory.hasItem("Ahrim")) {    // Ahrims in inventory
                info.hasMageWeapon = true;
                info.mageWeapon = "Mage armor detected (assumes barrage capability)";
            }
        }

        // Check for spec weapons (all weapons with special attacks)
        if (!info.hasSpecWeapon) {
            if (Rs2Inventory.hasItem("Dragon dagger") ||
                Rs2Inventory.hasItem("Dragon claws") ||
                Rs2Inventory.hasItem("Armadyl godsword") ||
                Rs2Inventory.hasItem("Dark bow") ||
                Rs2Inventory.hasItem("Heavy ballista") ||
                Rs2Inventory.hasItem("Granite maul") ||
                Rs2Inventory.hasItem("Magic shortbow")) {
                info.hasSpecWeapon = true;
                info.specWeapon = "Spec weapon in inventory";
            }
        }
    }

    private static void detectArmor(LmsGearInfo info) {
        // Check for melee armor (Bandos, Torva, Dragon)
        if (Rs2Equipment.isWearing("Bandos chestplate") ||
            Rs2Equipment.isWearing("Torva platebody") ||
            Rs2Equipment.isWearing("Dragon chainbody")) {
            info.hasMeleeArmor = true;
        }

        // Check for ranged armor (Armadyl, Black d'hide)
        if (Rs2Equipment.isWearing("Armadyl chestplate") ||
            Rs2Equipment.isWearing("Black d'hide body") ||
            Rs2Equipment.isWearing("Karil's leathertop")) {
            info.hasRangedArmor = true;
        }

        // Check for mage armor (Mystic, Ancestral, Ahrims)
        if (Rs2Equipment.isWearing("Mystic robe top") ||
            Rs2Equipment.isWearing("Ancestral robe top") ||
            Rs2Equipment.isWearing("Ahrim's robetop")) {
            info.hasMageArmor = true;
        }
    }

    /**
     * Print detailed gear report
     */
    public static void printGearReport(LmsGearInfo info) {
        log.info("=== LMS GEAR REPORT ===");
        log.info("Loadout Type: {}", info.isTribrid() ? "TRIBRID" : info.isPureStyle() ? "PURE STYLE" : "HYBRID");

        if (info.hasMeleeWeapon) {
            log.info("  Melee: {} {}", info.meleeWeapon, info.hasMeleeArmor ? "(with armor)" : "");
        }
        if (info.hasRangedWeapon) {
            log.info("  Ranged: {} {}", info.rangedWeapon, info.hasRangedArmor ? "(with armor)" : "");
        }
        if (info.hasMageWeapon) {
            log.info("  Mage: {} {}", info.mageWeapon, info.hasMageArmor ? "(with armor)" : "");
        }
        if (info.hasSpecWeapon) {
            log.info("  Special Attack: {}", info.specWeapon);
        }
        log.info("=======================");
    }

    /**
     * Suggest configuration based on detected gear
     */
    public static String getConfigSuggestion(LmsGearInfo info) {
        if (info.isTribrid()) {
            return "TRIBRID detected - Enable all attack styles, prayer switching, and gear switching";
        } else if (info.hasMeleeWeapon && !info.hasRangedWeapon && !info.hasMageWeapon) {
            return "MELEE ONLY - Enable melee attacks and overhead prayer switching";
        } else if (info.hasRangedWeapon && !info.hasMeleeWeapon && !info.hasMageWeapon) {
            return "RANGED ONLY - Enable ranged attacks and overhead prayer switching";
        } else if (info.hasMageWeapon && !info.hasMeleeWeapon && !info.hasRangedWeapon) {
            return "MAGE ONLY - Enable mage attacks and overhead prayer switching";
        } else {
            return "HYBRID detected - Enable multiple attack styles and prayer switching";
        }
    }
}
