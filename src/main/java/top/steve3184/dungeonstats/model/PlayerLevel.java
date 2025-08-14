package top.steve3184.dungeonstats.model;

// 用于封装从地牢日志中解析出的玩家及其在该次游戏中的等级
public record PlayerLevel(String playerName, int level) {}