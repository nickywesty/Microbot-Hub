package net.runelite.client.plugins.microbot.wildernessagilitywithpk.enums;

import net.runelite.client.plugins.microbot.util.prayer.Rs2PrayerEnum;

/**
 * Comprehensive mapping of all projectile IDs found in wilderness PvP combat
 * Maps projectile IDs to their corresponding protection prayer
 *
 * Sources: OSRS Wiki, runelite.net projectile database, in-game testing
 */
public enum WildernessProjectileType {

    // ===========================
    // MAGIC PROJECTILES
    // ===========================

    // Standard Spellbook - Combat Spells
    WIND_STRIKE(90, Rs2PrayerEnum.PROTECT_MAGIC),
    WATER_STRIKE(91, Rs2PrayerEnum.PROTECT_MAGIC),
    EARTH_STRIKE(92, Rs2PrayerEnum.PROTECT_MAGIC),
    FIRE_STRIKE(93, Rs2PrayerEnum.PROTECT_MAGIC),

    WIND_BOLT(117, Rs2PrayerEnum.PROTECT_MAGIC),
    WATER_BOLT(118, Rs2PrayerEnum.PROTECT_MAGIC),
    EARTH_BOLT(119, Rs2PrayerEnum.PROTECT_MAGIC),
    FIRE_BOLT(120, Rs2PrayerEnum.PROTECT_MAGIC),

    WIND_BLAST(133, Rs2PrayerEnum.PROTECT_MAGIC),
    WATER_BLAST(134, Rs2PrayerEnum.PROTECT_MAGIC),
    EARTH_BLAST(135, Rs2PrayerEnum.PROTECT_MAGIC),
    FIRE_BLAST(136, Rs2PrayerEnum.PROTECT_MAGIC),

    WIND_WAVE(159, Rs2PrayerEnum.PROTECT_MAGIC),
    WATER_WAVE(160, Rs2PrayerEnum.PROTECT_MAGIC),
    EARTH_WAVE(161, Rs2PrayerEnum.PROTECT_MAGIC),
    FIRE_WAVE(162, Rs2PrayerEnum.PROTECT_MAGIC),

    WIND_SURGE(165, Rs2PrayerEnum.PROTECT_MAGIC),
    WATER_SURGE(166, Rs2PrayerEnum.PROTECT_MAGIC),
    EARTH_SURGE(167, Rs2PrayerEnum.PROTECT_MAGIC),
    FIRE_SURGE(168, Rs2PrayerEnum.PROTECT_MAGIC),

    // God Spells
    SARADOMIN_STRIKE(127, Rs2PrayerEnum.PROTECT_MAGIC),
    CLAWS_OF_GUTHIX(128, Rs2PrayerEnum.PROTECT_MAGIC),
    FLAMES_OF_ZAMORAK(129, Rs2PrayerEnum.PROTECT_MAGIC),

    // Ancient Magicks - Ice Spells
    ICE_RUSH(360, Rs2PrayerEnum.PROTECT_MAGIC),
    ICE_BURST(361, Rs2PrayerEnum.PROTECT_MAGIC),
    ICE_BLITZ(366, Rs2PrayerEnum.PROTECT_MAGIC),
    ICE_BARRAGE(369, Rs2PrayerEnum.PROTECT_MAGIC),

    // Ancient Magicks - Blood Spells
    BLOOD_RUSH(372, Rs2PrayerEnum.PROTECT_MAGIC),
    BLOOD_BURST(376, Rs2PrayerEnum.PROTECT_MAGIC),
    BLOOD_BLITZ(374, Rs2PrayerEnum.PROTECT_MAGIC),
    BLOOD_BARRAGE(378, Rs2PrayerEnum.PROTECT_MAGIC),

    // Ancient Magicks - Smoke Spells
    SMOKE_RUSH(384, Rs2PrayerEnum.PROTECT_MAGIC),
    SMOKE_BURST(385, Rs2PrayerEnum.PROTECT_MAGIC),
    SMOKE_BLITZ(386, Rs2PrayerEnum.PROTECT_MAGIC),
    SMOKE_BARRAGE(387, Rs2PrayerEnum.PROTECT_MAGIC),

    // Ancient Magicks - Shadow Spells
    SHADOW_RUSH(379, Rs2PrayerEnum.PROTECT_MAGIC),
    SHADOW_BURST(380, Rs2PrayerEnum.PROTECT_MAGIC),
    SHADOW_BLITZ(381, Rs2PrayerEnum.PROTECT_MAGIC),
    SHADOW_BARRAGE(382, Rs2PrayerEnum.PROTECT_MAGIC),

    // Powered Staves
    TRIDENT_OF_SEAS(1252, Rs2PrayerEnum.PROTECT_MAGIC),
    TRIDENT_OF_SWAMP(1253, Rs2PrayerEnum.PROTECT_MAGIC),
    SANGUINESTI_STAFF(1539, Rs2PrayerEnum.PROTECT_MAGIC),
    TUMEKENS_SHADOW(2143, Rs2PrayerEnum.PROTECT_MAGIC),
    ACCURSED_SCEPTRE(2073, Rs2PrayerEnum.PROTECT_MAGIC),

    // Special Magic Attacks
    DAWNBRINGER(1546, Rs2PrayerEnum.PROTECT_MAGIC),
    VOLATILE_NIGHTMARE_STAFF(8532, Rs2PrayerEnum.PROTECT_MAGIC),

    // ===========================
    // RANGED PROJECTILES
    // ===========================

    // Arrows
    BRONZE_ARROW(19, Rs2PrayerEnum.PROTECT_RANGE),
    IRON_ARROW(20, Rs2PrayerEnum.PROTECT_RANGE),
    STEEL_ARROW(21, Rs2PrayerEnum.PROTECT_RANGE),
    MITHRIL_ARROW(22, Rs2PrayerEnum.PROTECT_RANGE),
    ADAMANT_ARROW(23, Rs2PrayerEnum.PROTECT_RANGE),
    RUNE_ARROW(24, Rs2PrayerEnum.PROTECT_RANGE),
    AMETHYST_ARROW(1301, Rs2PrayerEnum.PROTECT_RANGE),
    DRAGON_ARROW(1111, Rs2PrayerEnum.PROTECT_RANGE),

    // Crossbow Bolts
    BRONZE_BOLT(27, Rs2PrayerEnum.PROTECT_RANGE),
    IRON_BOLT(28, Rs2PrayerEnum.PROTECT_RANGE),
    STEEL_BOLT(29, Rs2PrayerEnum.PROTECT_RANGE),
    MITHRIL_BOLT(30, Rs2PrayerEnum.PROTECT_RANGE),
    ADAMANT_BOLT(31, Rs2PrayerEnum.PROTECT_RANGE),
    RUNITE_BOLT(32, Rs2PrayerEnum.PROTECT_RANGE),
    DRAGON_BOLT(1300, Rs2PrayerEnum.PROTECT_RANGE),

    // Special Bolts
    DIAMOND_BOLTS_E(1123, Rs2PrayerEnum.PROTECT_RANGE),
    RUBY_BOLTS_E(1122, Rs2PrayerEnum.PROTECT_RANGE),
    DRAGONSTONE_BOLTS_E(1124, Rs2PrayerEnum.PROTECT_RANGE),
    ONYX_BOLTS_E(1125, Rs2PrayerEnum.PROTECT_RANGE),

    // Dark Bow Special Attack
    DARK_BOW_SPEC(1099, Rs2PrayerEnum.PROTECT_RANGE),

    // Blowpipe
    TOXIC_BLOWPIPE(1043, Rs2PrayerEnum.PROTECT_RANGE),

    // Chinchompas
    CHINCHOMPA_RED(908, Rs2PrayerEnum.PROTECT_RANGE),
    CHINCHOMPA_GREY(909, Rs2PrayerEnum.PROTECT_RANGE),
    CHINCHOMPA_BLACK(910, Rs2PrayerEnum.PROTECT_RANGE),

    // Thrown Weapons
    THROWING_KNIFE(219, Rs2PrayerEnum.PROTECT_RANGE),
    THROWING_AXE(221, Rs2PrayerEnum.PROTECT_RANGE),

    // Javelins
    JAVELIN(206, Rs2PrayerEnum.PROTECT_RANGE),

    // Special Ranged Weapons
    MORRIGANS_THROWING_AXE(1304, Rs2PrayerEnum.PROTECT_RANGE),
    MORRIGANS_JAVELIN(1305, Rs2PrayerEnum.PROTECT_RANGE),
    CRYSTAL_BOW(249, Rs2PrayerEnum.PROTECT_RANGE),
    TWISTED_BOW(1120, Rs2PrayerEnum.PROTECT_RANGE),
    ZARYTE_CROSSBOW(1301, Rs2PrayerEnum.PROTECT_RANGE),
    ARMADYL_CROSSBOW(301, Rs2PrayerEnum.PROTECT_RANGE),
    VENATOR_BOW(2187, Rs2PrayerEnum.PROTECT_RANGE),
    WEBWEAVER_BOW(2195, Rs2PrayerEnum.PROTECT_RANGE),

    // ===========================
    // MELEE PROJECTILES (rare but exist)
    // ===========================

    // Dragon Claws Special (visual effect)
    DRAGON_CLAWS_SPEC(1171, Rs2PrayerEnum.PROTECT_MELEE),

    // Abyssal Tentacle/Whip Special
    ABYSSAL_TENTACLE_SPEC(1658, Rs2PrayerEnum.PROTECT_MELEE),

    // ===========================
    // MULTI-STYLE OR SPECIAL PROJECTILES
    // ===========================

    // These are area-effect or multi-target projectiles
    // Default to magic as most common in wilderness
    BARRAGE_MULTI_TARGET(368, Rs2PrayerEnum.PROTECT_MAGIC),
    BURST_MULTI_TARGET(367, Rs2PrayerEnum.PROTECT_MAGIC),

    // ===========================
    // COMMON WILDERNESS PKer WEAPONS
    // ===========================

    // These are the most frequently seen in wilderness PvP
    // Ballista
    HEAVY_BALLISTA(1301, Rs2PrayerEnum.PROTECT_RANGE),
    LIGHT_BALLISTA(1302, Rs2PrayerEnum.PROTECT_RANGE),

    // Granite Maul (no projectile but tracked for completeness)
    // Melee weapons don't typically have projectiles

    // Unknown/fallback
    UNKNOWN(-1, Rs2PrayerEnum.PROTECT_MELEE);

    private final int projectileId;
    private final Rs2PrayerEnum protectionPrayer;

    WildernessProjectileType(int projectileId, Rs2PrayerEnum protectionPrayer) {
        this.projectileId = projectileId;
        this.protectionPrayer = protectionPrayer;
    }

    public int getProjectileId() {
        return projectileId;
    }

    public Rs2PrayerEnum getProtectionPrayer() {
        return protectionPrayer;
    }

    /**
     * Get the appropriate protection prayer for a given projectile ID
     * @param projectileId The ID of the projectile
     * @return The protection prayer to use, or null if unknown
     */
    public static Rs2PrayerEnum getPrayerForProjectile(int projectileId) {
        for (WildernessProjectileType type : values()) {
            if (type.projectileId == projectileId) {
                return type.protectionPrayer;
            }
        }
        // Default to melee if unknown (most common fallback)
        return Rs2PrayerEnum.PROTECT_MELEE;
    }

    /**
     * Check if a projectile ID is a magic attack
     */
    public static boolean isMagicProjectile(int projectileId) {
        Rs2PrayerEnum prayer = getPrayerForProjectile(projectileId);
        return prayer == Rs2PrayerEnum.PROTECT_MAGIC;
    }

    /**
     * Check if a projectile ID is a ranged attack
     */
    public static boolean isRangedProjectile(int projectileId) {
        Rs2PrayerEnum prayer = getPrayerForProjectile(projectileId);
        return prayer == Rs2PrayerEnum.PROTECT_RANGE;
    }

    /**
     * Check if a projectile ID is a melee attack
     */
    public static boolean isMeleeProjectile(int projectileId) {
        Rs2PrayerEnum prayer = getPrayerForProjectile(projectileId);
        return prayer == Rs2PrayerEnum.PROTECT_MELEE;
    }

    /**
     * Get attack style name for logging
     */
    public static String getAttackStyleName(int projectileId) {
        Rs2PrayerEnum prayer = getPrayerForProjectile(projectileId);
        if (prayer == Rs2PrayerEnum.PROTECT_MAGIC) return "Magic";
        if (prayer == Rs2PrayerEnum.PROTECT_RANGE) return "Ranged";
        if (prayer == Rs2PrayerEnum.PROTECT_MELEE) return "Melee";
        return "Unknown";
    }
}
