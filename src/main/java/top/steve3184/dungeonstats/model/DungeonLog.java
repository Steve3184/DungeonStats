package top.steve3184.dungeonstats.model;

public record DungeonLog(
        int recordId,
        int maxLevel,
        int doorsOpened,
        int enemiesKilled,
        int bossesDefeated,
        long durationSeconds
) {}