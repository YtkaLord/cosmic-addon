package com.cosmic.betterlevelingaddon.skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SkillNode {
    private final String id;
    private final String name;
    private final String description;
    private final String bonusFormat;
    private final SkillCategory category;
    private final int x;
    private final int y;
    private final int maxLevel;
    private final int baseCost;
    private final int costIncrementPer5Levels;
    private final double valuePerLevel;
    private final boolean isKeystone;
    private final String iconItemRegistryName;
    // Карта пререквизитов: ID родительского навыка -> минимальный требуемый уровень этого навыка
    private final Map<String, Integer> prerequisites = new LinkedHashMap<>();

    public SkillNode(String id, String name, String description, String bonusFormat,
                     SkillCategory category, int x, int y, int maxLevel,
                     int baseCost, int costIncrementPer5Levels, double valuePerLevel,
                     boolean isKeystone, String iconItemRegistryName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.bonusFormat = bonusFormat;
        this.category = category;
        this.x = x;
        this.y = y;
        this.maxLevel = maxLevel;
        this.baseCost = baseCost;
        this.costIncrementPer5Levels = costIncrementPer5Levels;
        this.valuePerLevel = valuePerLevel;
        this.isKeystone = isKeystone;
        this.iconItemRegistryName = iconItemRegistryName;
    }

    public SkillNode addPrerequisite(String parentNodeId, int requiredLevel) {
        this.prerequisites.put(parentNodeId, requiredLevel);
        return this;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getBonusFormat() {
        return bonusFormat;
    }

    public SkillCategory getCategory() {
        return category;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getCostForNextLevel(int currentLevel) {
        if (costIncrementPer5Levels <= 0) {
            return baseCost;
        }
        return baseCost + (currentLevel / 5) * costIncrementPer5Levels;
    }

    public double getValuePerLevel() {
        return valuePerLevel;
    }

    public double getTotalBonus(int currentLevel) {
        return currentLevel * valuePerLevel;
    }

    public boolean isKeystone() {
        return isKeystone;
    }

    public String getIconItemRegistryName() {
        return iconItemRegistryName;
    }

    public Map<String, Integer> getPrerequisites() {
        return Collections.unmodifiableMap(prerequisites);
    }

    public List<String> getParentIds() {
        return new ArrayList<>(prerequisites.keySet());
    }
}
