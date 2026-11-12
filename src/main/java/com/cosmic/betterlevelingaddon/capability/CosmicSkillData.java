package com.cosmic.betterlevelingaddon.capability;

import com.cosmic.betterlevelingaddon.skill.SkillNode;
import com.cosmic.betterlevelingaddon.skill.SkillTreeRegistry;
import net.minecraft.nbt.CompoundNBT;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CosmicSkillData implements ICosmicSkillData {
    private int availablePoints = 0;
    private int lastRewardedMilestones = 0;
    private final Map<String, Integer> nodeLevels = new HashMap<>();
    private final Map<String, Integer> skillMilestones = new HashMap<>();

    @Override
    public int getAvailablePoints() {
        return availablePoints;
    }

    @Override
    public void setAvailablePoints(int points) {
        this.availablePoints = Math.max(0, points);
    }

    @Override
    public void addPoints(int amount) {
        if (amount > 0) {
            this.availablePoints += amount;
        }
    }

    @Override
    public int getSpentPoints() {
        int spent = 0;
        for (Map.Entry<String, Integer> entry : nodeLevels.entrySet()) {
            SkillNode node = SkillTreeRegistry.getNode(entry.getKey());
            int lvl = entry.getValue();
            if (node != null) {
                for (int i = 0; i < lvl; i++) {
                    spent += node.getCostForNextLevel(i);
                }
            } else {
                spent += lvl;
            }
        }
        return spent;
    }

    @Override
    public int getLastRewardedMilestones() {
        return lastRewardedMilestones;
    }

    @Override
    public void setLastRewardedMilestones(int milestones) {
        this.lastRewardedMilestones = Math.max(0, milestones);
    }

    @Override
    public int getSkillMilestone(String skillKey) {
        return skillMilestones.getOrDefault(skillKey, 0);
    }

    @Override
    public void setSkillMilestone(String skillKey, int milestone) {
        if (milestone > 0) {
            skillMilestones.put(skillKey, milestone);
        }
    }

    @Override
    public Map<String, Integer> getAllSkillMilestones() {
        return Collections.unmodifiableMap(skillMilestones);
    }

    @Override
    public int getNodeLevel(String nodeId) {
        return nodeLevels.getOrDefault(nodeId, 0);
    }

    @Override
    public void setNodeLevel(String nodeId, int level) {
        if (level <= 0) {
            nodeLevels.remove(nodeId);
        } else {
            nodeLevels.put(nodeId, level);
        }
    }

    @Override
    public Map<String, Integer> getAllNodeLevels() {
        return Collections.unmodifiableMap(nodeLevels);
    }

    @Override
    public boolean arePrerequisitesMet(SkillNode node) {
        if (node == null) return false;
        Map<String, Integer> prereqs = node.getPrerequisites();
        if (prereqs.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Integer> entry : prereqs.entrySet()) {
            if (getNodeLevel(entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canUpgradeNode(SkillNode node) {
        if (node == null) return false;
        int current = getNodeLevel(node.getId());
        if (current >= node.getMaxLevel()) {
            return false;
        }
        if (!arePrerequisitesMet(node)) {
            return false;
        }
        int cost = node.getCostForNextLevel(current);
        return availablePoints >= cost;
    }

    @Override
    public boolean upgradeNode(SkillNode node) {
        if (!canUpgradeNode(node)) {
            return false;
        }
        int current = getNodeLevel(node.getId());
        int cost = node.getCostForNextLevel(current);
        this.availablePoints -= cost;
        setNodeLevel(node.getId(), current + 1);
        return true;
    }

    @Override
    public void resetAllSkills() {
        int refunded = getSpentPoints();
        nodeLevels.clear();
        this.availablePoints += refunded;
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT tag = new CompoundNBT();
        tag.putInt("AvailablePoints", availablePoints);
        tag.putInt("LastRewardedMilestones", lastRewardedMilestones);

        CompoundNBT nodesTag = new CompoundNBT();
        for (Map.Entry<String, Integer> entry : nodeLevels.entrySet()) {
            nodesTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("Nodes", nodesTag);

        CompoundNBT skillsTag = new CompoundNBT();
        for (Map.Entry<String, Integer> entry : skillMilestones.entrySet()) {
            skillsTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("SkillMilestones", skillsTag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        this.availablePoints = nbt.getInt("AvailablePoints");
        this.lastRewardedMilestones = nbt.getInt("LastRewardedMilestones");

        this.nodeLevels.clear();
        if (nbt.contains("Nodes", 10)) {
            CompoundNBT nodesTag = nbt.getCompound("Nodes");
            for (String key : nodesTag.getAllKeys()) {
                int lvl = nodesTag.getInt(key);
                if (lvl > 0) {
                    this.nodeLevels.put(key, lvl);
                }
            }
        }

        this.skillMilestones.clear();
        if (nbt.contains("SkillMilestones", 10)) {
            CompoundNBT skillsTag = nbt.getCompound("SkillMilestones");
            for (String key : skillsTag.getAllKeys()) {
                int m = skillsTag.getInt(key);
                if (m > 0) {
                    this.skillMilestones.put(key, m);
                }
            }
        }
    }

    @Override
    public void copyFrom(ICosmicSkillData other) {
        this.deserializeNBT(other.serializeNBT());
    }
}
