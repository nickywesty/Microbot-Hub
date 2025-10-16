package net.runelite.client.plugins.microbot.smartminer.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.bank.enums.BankLocation;

@Getter
@RequiredArgsConstructor
public enum MiningLocation {
    // F2P Locations
    LUMBRIDGE_SWAMP_WEST("Lumbridge Swamp West Mine", new WorldPoint(3149, 3148, 0), BankLocation.DRAYNOR_VILLAGE, false),
    LUMBRIDGE_SWAMP_EAST("Lumbridge Swamp East Mine", new WorldPoint(3229, 3148, 0), BankLocation.LUMBRIDGE_TOP, false),
    VARROCK_EAST("Varrock East Mine", new WorldPoint(3285, 3363, 0), BankLocation.VARROCK_EAST, false),
    VARROCK_WEST("Varrock West Mine", new WorldPoint(3181, 3377, 0), BankLocation.VARROCK_WEST, false),
    AL_KHARID("Al Kharid Mine", new WorldPoint(3296, 3315, 0), BankLocation.AL_KHARID, false),
    DWARVEN_MINE("Dwarven Mine", new WorldPoint(3034, 9822, 0), BankLocation.FALADOR_WEST, false),
    BARBARIAN_VILLAGE("Barbarian Village Mine", new WorldPoint(3081, 3421, 0), BankLocation.EDGEVILLE, false),
    RIMMINGTON("Rimmington Mine", new WorldPoint(2976, 3240, 0), BankLocation.FALADOR_EAST, false),

    // P2P Locations
    MINING_GUILD("Mining Guild", new WorldPoint(3046, 9756, 0), BankLocation.MINING_GUILD, true),
    MOTHERLODE_MINE("Motherlode Mine", new WorldPoint(3755, 5666, 0), BankLocation.MOTHERLOAD, true),
    CRAFTING_GUILD("Crafting Guild", new WorldPoint(2938, 3283, 0), BankLocation.CRAFTING_GUILD, true),
    SHILO_VILLAGE("Shilo Village Mine", new WorldPoint(2824, 2997, 0), BankLocation.SHILO_VILLAGE, true),
    LEGENDS_GUILD("Legends' Guild Mine", new WorldPoint(2729, 3352, 0), BankLocation.LEGENDS_GUILD, true),
    ARDOUGNE_SOUTHEAST("Ardougne Southeast Mine", new WorldPoint(2704, 3330, 0), BankLocation.ARDOUGNE_SOUTH, true),
    PISCATORIS("Piscatoris Mine", new WorldPoint(2338, 3641, 0), BankLocation.PISCATORIS_FISHING_COLONY, true);

    private final String name;
    private final WorldPoint location;
    private final BankLocation nearestBank;
    private final boolean membersOnly;

    @Override
    public String toString() {
        return name + (membersOnly ? " (Members)" : "");
    }

    public WorldPoint getBankLocation() {
        return nearestBank.getWorldPoint();
    }
}
