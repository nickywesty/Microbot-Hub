package net.runelite.client.plugins.microbot.smartminer.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

@Getter
@RequiredArgsConstructor
public enum Pickaxe {
    BRONZE("Bronze pickaxe", 1),
    IRON("Iron pickaxe", 1),
    STEEL("Steel pickaxe", 6),
    BLACK("Black pickaxe", 11),
    MITHRIL("Mithril pickaxe", 21),
    ADAMANT("Adamant pickaxe", 31),
    RUNE("Rune pickaxe", 41),
    DRAGON("Dragon pickaxe", 61),
    DRAGON_OR("Dragon pickaxe(or)", 61),
    INFERNAL("Infernal pickaxe", 61),
    CRYSTAL("Crystal pickaxe", 71),
    THIRD_AGE("3rd age pickaxe", 61);

    private final String name;
    private final int levelRequired;

    public boolean hasRequiredLevel() {
        return Rs2Player.getSkillRequirement(Skill.MINING, this.levelRequired);
    }

    public boolean isInInventoryOrEquipped() {
        return Rs2Inventory.contains(name) || Rs2Equipment.isWearing(name);
    }

    public static Pickaxe getBestPickaxeInInventoryOrEquipped() {
        for (int i = Pickaxe.values().length - 1; i >= 0; i--) {
            Pickaxe pickaxe = Pickaxe.values()[i];
            if (pickaxe.isInInventoryOrEquipped() && pickaxe.hasRequiredLevel()) {
                return pickaxe;
            }
        }
        return null;
    }

    public static Pickaxe getBestPickaxeInBank() {
        // This will be used when we implement bank logic
        return null;
    }
}
