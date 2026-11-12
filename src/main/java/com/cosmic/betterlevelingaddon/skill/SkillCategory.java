package com.cosmic.betterlevelingaddon.skill;

public enum SkillCategory {
    CORE("Ядро Космоса", 0xFF00E5FF, 0xFF005577),
    MINING("Ветвь Шахтёра и Ремесла", 0xFFFFB300, 0xFF664400),
    AGILITY("Ветвь Скорости и Странствий", 0xFF00E676, 0xFF005522),
    COMBAT("Ветвь Воина и Ярости", 0xFFFF1744, 0xFF660011),
    FISHING_LOOT("Ветвь Охотника и Бездны Океана", 0xFFAA00FF, 0xFF440066),
    CELESTIAL("Звёздное Слияние (Эндгейм)", 0xFFFFD700, 0xFF665500);

    private final String displayName;
    private final int primaryColor;
    private final int darkColor;

    SkillCategory(String displayName, int primaryColor, int darkColor) {
        this.displayName = displayName;
        this.primaryColor = primaryColor;
        this.darkColor = darkColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public int getDarkColor() {
        return darkColor;
    }
}
