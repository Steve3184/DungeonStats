package top.steve3184.dungeonstats.model;

public record PlayerStats(
        String playerName,
        long kills,
        long playtimeSeconds,
        int maxLevel
) {}