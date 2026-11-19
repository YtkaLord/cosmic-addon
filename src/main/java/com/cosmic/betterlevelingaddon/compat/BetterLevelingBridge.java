package com.cosmic.betterlevelingaddon.compat;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Мост интеграции с Project MMO (3.67.6) и Better Leveling (0.3.0):
 * 1) Читает уровни навыков из Project MMO и Better Leveling (СТРОГО исключая наш собственный аддон!).
 * 2) Начисляет опыт Farming в Project MMO за модовые растения (Farmer's Delight, Mystical Agriculture и др.).
 */
public class BetterLevelingBridge {

    private static Field providersFieldCached = null;
    private static boolean capReflectionFailed = false;

    // Кэш рефлексии для Project MMO (PMMO)
    private static boolean pmmoChecked = false;
    private static boolean pmmoPresent = false;
    private static Method pmmoGetXpMapMethod = null;
    private static Method pmmoLevelAtXpDecimalMethod = null;
    private static Method pmmoLevelAtXpIntMethod = null;
    private static Method pmmoSkillGetLevelMethod = null;

    // Кэш метода выдачи опыта в PMMO (APIUtils.addXp или XP.awardXp)
    private static Method pmmoAwardXpMethod = null;
    private static int pmmoAwardSignatureType = 0; // 1 = APIUtils.addXp(6), 2 = XP.awardXp(7), 3 = XP.awardXp(6)

    public static Map<String, Integer> scanAllSkillLevels(ServerPlayerEntity player) {
        Map<String, Integer> discoveredSkills = new HashMap<>();

        // 1. Project MMO (PMMO 1.16.5)
        scanProjectMmoSkills(player, discoveredSkills);

        // 2. Better Leveling (строго без самосканирования нашего аддона!)
        scanBetterLevelingCapabilities(player, discoveredSkills);

        return discoveredSkills;
    }

    private static boolean isOurOwnModKey(String key) {
        if (key == null) return true;
        String lower = key.toLowerCase();
        return lower.contains("blcosmicaddon")
                || lower.contains("betterlevelingaddon")
                || lower.contains("com.cosmic")
                || lower.contains("cosmic_skills")
                || lower.contains("icosmicskilldata");
    }

    private static void initPmmoReflection() {
        if (pmmoChecked) return;
        pmmoChecked = true;
        try {
            Class<?> configClass = Class.forName("harmonised.pmmo.config.Config");
            pmmoGetXpMapMethod = configClass.getMethod("getXpMap", PlayerEntity.class);

            Class<?> xpClass = Class.forName("harmonised.pmmo.util.XP");
            try {
                pmmoLevelAtXpDecimalMethod = xpClass.getMethod("levelAtXpDecimal", double.class);
            } catch (NoSuchMethodException ignored) {
            }
            try {
                pmmoLevelAtXpIntMethod = xpClass.getMethod("levelAtXp", double.class);
            } catch (NoSuchMethodException ignored) {
            }

            try {
                Class<?> skillClass = Class.forName("harmonised.pmmo.skills.Skill");
                pmmoSkillGetLevelMethod = skillClass.getMethod("getLevel", String.class, PlayerEntity.class);
            } catch (Exception ignored) {
            }

            // Поиск метода начисления опыта в Project MMO
            try {
                Class<?> apiUtilsClass = Class.forName("harmonised.pmmo.api.APIUtils");
                for (Method m : apiUtilsClass.getMethods()) {
                    if (m.getName().equals("addXp")) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length == 6
                                && params[0] == String.class
                                && PlayerEntity.class.isAssignableFrom(params[1])
                                && params[2] == double.class
                                && params[3] == String.class
                                && params[4] == boolean.class
                                && params[5] == boolean.class) {
                            pmmoAwardXpMethod = m;
                            pmmoAwardSignatureType = 1;
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            if (pmmoAwardXpMethod == null) {
                for (Method m : xpClass.getMethods()) {
                    if (m.getName().equals("awardXp")) {
                        Class<?>[] params = m.getParameterTypes();
                        if (params.length == 7
                                && PlayerEntity.class.isAssignableFrom(params[0])
                                && params[1] == String.class
                                && params[2] == String.class
                                && params[3] == double.class) {
                            pmmoAwardXpMethod = m;
                            pmmoAwardSignatureType = 2;
                            break;
                        } else if (params.length == 6
                                && PlayerEntity.class.isAssignableFrom(params[0])
                                && params[1] == String.class
                                && params[2] == String.class
                                && params[3] == double.class) {
                            pmmoAwardXpMethod = m;
                            pmmoAwardSignatureType = 3;
                            break;
                        }
                    }
                }
            }

            pmmoPresent = true;
        } catch (ClassNotFoundException e) {
            pmmoPresent = false;
        } catch (Exception e) {
            pmmoPresent = false;
        }
    }

    /**
     * Выдаёт опыт в навык Project MMO (например, "farming" за сбор модовых растений).
     */
    public static boolean awardPmmoXp(ServerPlayerEntity player, String skillName, double amount, String sourceDesc) {
        if (player == null || amount <= 0) return false;
        initPmmoReflection();
        if (!pmmoPresent || pmmoAwardXpMethod == null) return false;

        try {
            if (pmmoAwardSignatureType == 1) {
                pmmoAwardXpMethod.invoke(null, skillName, player, amount, sourceDesc, false, false);
                return true;
            } else if (pmmoAwardSignatureType == 2) {
                pmmoAwardXpMethod.invoke(null, player, skillName, sourceDesc, amount, false, false, false);
                return true;
            } else if (pmmoAwardSignatureType == 3) {
                pmmoAwardXpMethod.invoke(null, player, skillName, sourceDesc, amount, false, false);
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static void scanProjectMmoSkills(ServerPlayerEntity player, Map<String, Integer> outSkills) {
        initPmmoReflection();
        if (!pmmoPresent) return;

        try {
            if (pmmoGetXpMapMethod != null) {
                Object rawMap = pmmoGetXpMapMethod.invoke(null, player);
                if (rawMap instanceof Map) {
                    Map<String, ?> xpMap = (Map<String, ?>) rawMap;
                    for (Map.Entry<String, ?> entry : xpMap.entrySet()) {
                        String skillName = entry.getKey();
                        Object xpValObj = entry.getValue();
                        if (skillName == null || xpValObj == null) continue;

                        int level = 0;
                        if (pmmoSkillGetLevelMethod != null) {
                            try {
                                Object lvlObj = pmmoSkillGetLevelMethod.invoke(null, skillName, player);
                                if (lvlObj instanceof Number) {
                                    level = ((Number) lvlObj).intValue();
                                }
                            } catch (Exception ignored) {
                            }
                        }

                        if (level <= 0 && xpValObj instanceof Number) {
                            double xp = ((Number) xpValObj).doubleValue();
                            if (pmmoLevelAtXpDecimalMethod != null) {
                                Object decLvl = pmmoLevelAtXpDecimalMethod.invoke(null, xp);
                                if (decLvl instanceof Number) {
                                    level = (int) Math.floor(((Number) decLvl).doubleValue());
                                }
                            } else if (pmmoLevelAtXpIntMethod != null) {
                                Object intLvl = pmmoLevelAtXpIntMethod.invoke(null, xp);
                                if (intLvl instanceof Number) {
                                    level = ((Number) intLvl).intValue();
                                }
                            }
                        }

                        if (level > 0) {
                            recordMaxLevel(outSkills, "pmmo:" + skillName.toLowerCase(), level);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void scanBetterLevelingCapabilities(ServerPlayerEntity player, Map<String, Integer> discoveredSkills) {
        // А) Читаем ForgeCaps игрока (исключая наш собственный мод!)
        try {
            CompoundNBT fullPlayerTag = new CompoundNBT();
            player.saveWithoutId(fullPlayerTag);

            if (fullPlayerTag.contains("ForgeCaps", 10)) {
                CompoundNBT forgeCaps = fullPlayerTag.getCompound("ForgeCaps");
                for (String capKey : forgeCaps.getAllKeys()) {
                    if (isOurOwnModKey(capKey)) continue;
                    String lowerKey = capKey.toLowerCase();
                    if (lowerKey.startsWith("betterleveling:") || lowerKey.contains("darkorg")) {
                        INBT capData = forgeCaps.get(capKey);
                        extractSkillLevelsRecursive("bl:" + capKey, capData, discoveredSkills, 0);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Б) Проверяем CapabilityManager (СТРОГО исключая наш пакет com.cosmic.betterlevelingaddon!)
        if (!capReflectionFailed) {
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
                        String name = entry.getKey();
                        if (isOurOwnModKey(name)) continue;
                        String lower = name.toLowerCase();
                        if (lower.contains("darkorg") || (lower.contains("betterleveling") && !lower.contains("addon"))) {
                            Capability<?> cap = entry.getValue();
                            player.getCapability(cap).ifPresent(inst -> {
                                try {
                                    INBT nbt = writeCapabilitySafe(cap, inst);
                                    if (nbt != null) {
                                        extractSkillLevelsRecursive("blcap:" + lower, nbt, discoveredSkills, 0);
                                    }
                                } catch (Exception ignored2) {
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                capReflectionFailed = true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> INBT writeCapabilitySafe(Capability<T> cap, Object instance) {
        return cap.getStorage().writeNBT(cap, (T) instance, null);
    }

    private static void extractSkillLevelsRecursive(String path, INBT nbt, Map<String, Integer> outSkills, int depth) {
        if (nbt == null || depth > 6) return;

        if (nbt instanceof CompoundNBT) {
            CompoundNBT compound = (CompoundNBT) nbt;

            if ((compound.contains("Level", 99) || compound.contains("level", 99))
                    && (compound.contains("Name", 8) || compound.contains("name", 8) || compound.contains("Id", 8) || compound.contains("id", 8))) {
                String id = compound.contains("Id", 8) ? compound.getString("Id")
                        : (compound.contains("id", 8) ? compound.getString("id")
                        : (compound.contains("Name", 8) ? compound.getString("Name") : compound.getString("name")));
                int lvl = compound.contains("Level", 99) ? compound.getInt("Level") : compound.getInt("level");
                recordMaxLevel(outSkills, "blskill:" + id.toLowerCase(), lvl);
                return;
            }

            for (String key : compound.getAllKeys()) {
                INBT child = compound.get(key);
                String lowerKey = key.toLowerCase();

                if (lowerKey.contains("xp") || lowerKey.contains("exp") || lowerKey.contains("point")
                        || lowerKey.contains("cost") || lowerKey.contains("unlocked") || lowerKey.contains("choice")
                        || lowerKey.contains("spec") || lowerKey.contains("uuid") || lowerKey.contains("time")
                        || lowerKey.contains("milestone")) {
                    continue;
                }

                if (child != null && (child.getId() == 3 || child.getId() == 2 || child.getId() == 1)) {
                    int val = compound.getInt(key);
                    if (val > 0 && val <= 1000) {
                        String skillKey = (lowerKey.equals("level") || lowerKey.equals("lvl")) ? path : (path + "." + key);
                        recordMaxLevel(outSkills, skillKey.toLowerCase(), val);
                    }
                } else if (child != null && (child.getId() == 5 || child.getId() == 6)) {
                    double dVal = compound.getDouble(key);
                    if (dVal >= 1.0 && dVal <= 1000.0 && (lowerKey.contains("level") || lowerKey.contains("lvl"))) {
                        recordMaxLevel(outSkills, (path + "." + key).toLowerCase(), (int) Math.floor(dVal));
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
