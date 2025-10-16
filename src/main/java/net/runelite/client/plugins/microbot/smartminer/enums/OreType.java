package net.runelite.client.plugins.microbot.smartminer.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Skill;
import net.runelite.client.plugins.microbot.util.player.Rs2Player;

@Getter
@RequiredArgsConstructor
public enum OreType {
    COPPER("Copper rocks", "Copper ore", 1),
    TIN("Tin rocks", "Tin ore", 1),
    CLAY("Clay rocks", "Clay", 1),
    IRON("Iron rocks", "Iron ore", 15),
    SILVER("Silver rocks", "Silver ore", 20),
    COAL("Coal rocks", "Coal", 30),
    GOLD("Gold rocks", "Gold ore", 40),
    MITHRIL("Mithril rocks", "Mithril ore", 55),
    ADAMANTITE("Adamantite rocks", "Adamantite ore", 70),
    RUNITE("Runite rocks", "Runite ore", 85);

    private final String rockName;
    private final String oreName;
    private final int levelRequired;

    @Override
    public String toString() {
        return rockName;
    }

    public boolean hasRequiredLevel() {
        return Rs2Player.getSkillRequirement(Skill.MINING, this.levelRequired);
    }
}
