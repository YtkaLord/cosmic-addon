package com.cosmic.betterlevelingaddon.skill;

public enum SkillCategory {
    CORE("Ядро Космоса", 0xFF00E5FF, 0xFF005577),
    MINING("Ветвь Шахтёра и Недр", 0xFFFFB300, 0xFF664400),
    FARMING("Ветвь Фермерства и Природы", 0xFF76FF03, 0xFF2E6600),
    ARCANE("Ветвь Магии и Стихий", 0xFF7C4DFF, 0xFF311B92),
    AGILITY("Ветвь Скорости и Живучести", 0xFF00E676, 0xFF005522),
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
