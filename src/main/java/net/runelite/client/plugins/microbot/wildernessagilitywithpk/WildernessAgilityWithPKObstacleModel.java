package net.runelite.client.plugins.microbot.wildernessagilitywithpk;

import lombok.Getter;

public class WildernessAgilityWithPKObstacleModel {
    @Getter
    private final int objectId;
    @Getter
    private final boolean canFail;

    public WildernessAgilityWithPKObstacleModel(int objectId, boolean canFail) {
        this.objectId = objectId;
        this.canFail = canFail;
    }
} 