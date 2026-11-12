package com.cosmic.betterlevelingaddon.capability;

import com.cosmic.betterlevelingaddon.skill.SkillNode;
import net.minecraft.nbt.CompoundNBT;

import java.util.Map;

public interface ICosmicSkillData {
    int getAvailablePoints();
    void setAvailablePoints(int points);
    void addPoints(int amount);

    int getSpentPoints();

    int getLastRewardedMilestones();
    void setLastRewardedMilestones(int milestones);

    int getSkillMilestone(String skillKey);
    void setSkillMilestone(String skillKey, int milestone);
    Map<String, Integer> getAllSkillMilestones();

    int getNodeLevel(String nodeId);
    void setNodeLevel(String nodeId, int level);
    Map<String, Integer> getAllNodeLevels();

    boolean arePrerequisitesMet(SkillNode node);
    boolean canUpgradeNode(SkillNode node);
    boolean upgradeNode(SkillNode node);
    void resetAllSkills();

    CompoundNBT serializeNBT();
    void deserializeNBT(CompoundNBT nbt);
    void copyFrom(ICosmicSkillData other);
}
