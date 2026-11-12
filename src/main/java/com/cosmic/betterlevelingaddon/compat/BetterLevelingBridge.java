package com.cosmic.betterlevelingaddon.compat;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Мост интеграции с модом Better Leveling (1.16.5).
 * Сканирует все уровни навыков игрока из Better Leveling (как через Forge Capability Dispatcher /
 * внутренний реестр CapabilityManager, так и через PersistentData игрока).
 *
 * Правило начисления:
 * За каждые 5 уровней ЛЮБОГО навыка в Better Leveling (например, боевой навык достиг 5, 10, 15 ур.,
 * или шахтёрский достиг 5 ур.) начисляется +1 очко навыков для Космического Дерева:
 * milestones = sum( skillLevel / 5 ).
 */
public class BetterLevelingBridge {

    private static Field providersFieldCached = null;
    private static boolean reflectionFailed = false;

    public static class ScanResult {
        public final int totalMilestones; // Сумма (level / 5) по всем навыкам Better Leveling
        public final int totalLevels;     // Сумма всех уровней в Better Leveling

        public ScanResult(int totalMilestones, int totalLevels) {
            this.totalMilestones = totalMilestones;
            this.totalLevels = totalLevels;
        }
    }

    public static ScanResult scanPlayerBetterLeveling(ServerPlayerEntity player) {
        Map<String, Integer> discoveredSkills = new HashMap<>();

        // 1. Сканируем полный сериализованный снимок сущности игрока (включает ForgeCaps всех модов, включая betterleveling)
        try {
            CompoundNBT fullPlayerTag = new CompoundNBT();
            player.saveWithoutId(fullPlayerTag);

            if (fullPlayerTag.contains("ForgeCaps", 10)) {
                CompoundNBT forgeCaps = fullPlayerTag.getCompound("ForgeCaps");
                for (String capKey : forgeCaps.getAllKeys()) {
                    String lowerKey = capKey.toLowerCase();
                    // Игнорируем наш собственный capability, ищем betterleveling / skill / level / darkorg
                    if (lowerKey.contains("blcosmicaddon")) continue;
                    if (lowerKey.contains("betterleveling") || lowerKey.contains("better_leveling")
                            || lowerKey.contains("darkorg") || lowerKey.contains("skill") || lowerKey.contains("level")) {
                        INBT capData = forgeCaps.get(capKey);
                        extractSkillLevelsRecursive(capKey, capData, discoveredSkills, 0);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 2. Дополнительно проверяем PersistentData игрока (на случай хранения в PlayerPersisted)
        try {
            CompoundNBT persistent = player.getPersistentData();
            for (String key : persistent.getAllKeys()) {
                String lower = key.toLowerCase();
                if (lower.contains("blcosmicaddon")) continue;
                if (lower.contains("betterleveling") || lower.contains("better_leveling")
                        || lower.contains("skill") || lower.contains("playerpersisted")) {
                    extractSkillLevelsRecursive(key, persistent.get(key), discoveredSkills, 0);
                }
            }
        } catch (Exception ignored) {
        }

        // 3. Дополнительно опрашиваем зарегистрированные в CapabilityManager провайдеры с пакетами betterleveling
        if (!reflectionFailed) {
            try {
                if (providersFieldCached == null) {
                    providersFieldCached = CapabilityManager.class.getDeclaredField("providers");
                    providersFieldCached.setAccessible(true);
                }
                @SuppressWarnings("unchecked")
                IdentityHashMap<String, Capability<?>> providers =
                        (IdentityHashMap<String, Capability<?>>) providersFieldCached.get(CapabilityManager.INSTANCE);
                if (providers != null) {
                    for (Map.Entry<String, Capability<?>> entry : providers.entrySet()) {
                        String name = entry.getKey().toLowerCase();
                        if (name.contains("betterleveling") || name.contains("darkorg")) {
                            Capability<?> cap = entry.getValue();
                            player.getCapability(cap).ifPresent(inst -> {
                                try {
                                    INBT nbt = writeCapabilitySafe(cap, inst);
                                    if (nbt != null) {
                                        extractSkillLevelsRecursive(name, nbt, discoveredSkills, 0);
                                    }
                                } catch (Exception ignored2) {
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                reflectionFailed = true;
            }
        }

        int totalMilestones = 0;
        int totalLevels = 0;
        for (Map.Entry<String, Integer> entry : discoveredSkills.entrySet()) {
            int lvl = entry.getValue();
            if (lvl > 0 && lvl <= 10000) {
                totalLevels += lvl;
                totalMilestones += (lvl / 5);
            }
        }

        return new ScanResult(totalMilestones, totalLevels);
    }

    @SuppressWarnings("unchecked")
    private static <T> INBT writeCapabilitySafe(Capability<T> cap, Object instance) {
        return cap.getStorage().writeNBT(cap, (T) instance, null);
    }

    private static void extractSkillLevelsRecursive(String path, INBT nbt, Map<String, Integer> outSkills, int depth) {
        if (nbt == null || depth > 6) return;

        if (nbt instanceof CompoundNBT) {
            CompoundNBT compound = (CompoundNBT) nbt;

            // Если это структура одного навыка вида { "Name"/"Id": "...", "Level"/"level": X }
            if ((compound.contains("Level", 99) || compound.contains("level", 99))
                    && (compound.contains("Name", 8) || compound.contains("name", 8) || compound.contains("Id", 8) || compound.contains("id", 8))) {
                String id = compound.contains("Id", 8) ? compound.getString("Id")
                        : (compound.contains("id", 8) ? compound.getString("id")
                        : (compound.contains("Name", 8) ? compound.getString("Name") : compound.getString("name")));
                int lvl = compound.contains("Level", 99) ? compound.getInt("Level") : compound.getInt("level");
                recordMaxLevel(outSkills, id, lvl);
                return;
            }

            for (String key : compound.getAllKeys()) {
                INBT child = compound.get(key);
                String lowerKey = key.toLowerCase();

                // Пропускаем поля опыта (xp, experience, cost), нас интересуют только целочисленные уровни навыков
                if (lowerKey.contains("xp") || lowerKey.contains("exp") || lowerKey.contains("point")
                        || lowerKey.contains("cost") || lowerKey.contains("unlocked") || lowerKey.contains("choice")) {
                    continue;
                }

                if (child != null && (child.getId() == 3 || child.getId() == 2 || child.getId() == 1)) {
                    // IntNBT (3), ShortNBT (2), ByteNBT (1)
                    int val = compound.getInt(key);
                    if (val > 0 && val <= 1000) {
                        String skillKey = (lowerKey.equals("level") || lowerKey.equals("lvl")) ? path : (path + "." + key);
                        recordMaxLevel(outSkills, skillKey, val);
                    }
                } else {
                    extractSkillLevelsRecursive(path + "." + key, child, outSkills, depth + 1);
                }
            }
        } else if (nbt instanceof ListNBT) {
            ListNBT list = (ListNBT) nbt;
            for (int i = 0; i < list.size(); i++) {
                extractSkillLevelsRecursive(path + "[" + i + "]", list.get(i), outSkills, depth + 1);
            }
        }
    }

    private static void recordMaxLevel(Map<String, Integer> map, String skillName, int level) {
        if (level <= 0) return;
        int prev = map.getOrDefault(skillName, 0);
        if (level > prev) {
            map.put(skillName, level);
        }
    }
}
