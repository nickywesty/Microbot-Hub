package net.runelite.client.plugins.microbot.wildernessnicky;

import lombok.Getter;

public class WildernessNickyObstacleModel {
    @Getter
    private final int objectId;
    @Getter
    private final boolean canFail;

    public WildernessNickyObstacleModel(int objectId, boolean canFail) {
        this.objectId = objectId;
        this.canFail = canFail;
    }
} 