package com.cosmic.betterlevelingaddon.event;

import com.cosmic.betterlevelingaddon.BetterLevelingCosmicAddon;
import com.cosmic.betterlevelingaddon.capability.CosmicSkillCapability;
import com.cosmic.betterlevelingaddon.capability.ICosmicSkillData;
import com.cosmic.betterlevelingaddon.compat.BetterLevelingBridge;
import com.cosmic.betterlevelingaddon.network.PacketHandler;
import com.cosmic.betterlevelingaddon.network.SyncCosmicDataPacket;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class CommonForgeEvents {

    private static final ResourceLocation CAP_ID = new ResourceLocation(BetterLevelingCosmicAddon.MOD_ID, "cosmic_skills");

    private static final UUID MOVE_SPEED_UUID = UUID.fromString("91aeaa56-376b-4498-935b-2f7f68070635");
    private static final UUID MAX_HEALTH_UUID = UUID.fromString("92aeaa56-376b-4498-935b-2f7f68070636");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("93aeaa56-376b-4498-935b-2f7f68070637");
    private static final UUID ARMOR_UUID = UUID.fromString("94aeaa56-376b-4498-935b-2f7f68070638");
    private static final UUID LUCK_UUID = UUID.fromString("95aeaa56-376b-4498-935b-2f7f68070639");

    private final Random random = new Random();

    @SubscribeEvent
    public void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof PlayerEntity) {
            CosmicSkillCapability.Provider provider = new CosmicSkillCapability.Provider();
            event.addCapability(CAP_ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        // Сохраняем всё космическое дерево и очки навыков при смерти и возвращении из Энда
        event.getOriginal().getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(oldCap -> {
            event.getPlayer().getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(newCap -> {
                newCap.copyFrom(oldCap);
            });
        });
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) event.getPlayer();
            syncAndCheckMilestones(serverPlayer, true);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) event.getPlayer();
            syncAndCheckMilestones(serverPlayer, true);
            serverPlayer.setHealth(serverPlayer.getMaxHealth());
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getPlayer() instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) event.getPlayer();
            syncAndCheckMilestones(serverPlayer, true);
        }
    }

    /**
     * Каждые 2 секунды (40 тиков) проверяем уровни всех навыков в моде Better Leveling.
     * Если игрок прокачал любой навык в Better Leveling до уровня, кратного 5 (5, 10, 15, 20...),
     * ему автоматически начисляется 1 очко навыков в Космическом Дереве!
     */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PlayerEntity player = event.player;
        if (player.level.isClientSide) return;

        // Проверка Better Leveling каждые 40 тиков
        if (player.tickCount % 40 == 0 && player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            syncAndCheckMilestones(serverPlayer, false);

            // Пассивная регенерация от навыка "Метаболизм Туманности" (cosmic_regen)
            player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
                int regenLvl = cap.getNodeLevel("cosmic_regen");
                int transLvl = cap.getNodeLevel("cosmic_transcendence");
                if ((regenLvl > 0 || transLvl > 0) && player.getHealth() < player.getMaxHealth()) {
                    float healAmount = (float) (regenLvl * 0.08 + transLvl * 0.15);
                    player.heal(healAmount);
                }
            });
        }

        // Бонус скорости в воде от навыка "Гидродинамика Кометы" (swim_jet)
        if (player.isInWater()) {
            player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
                int swimLvl = cap.getNodeLevel("swim_jet");
                if (swimLvl > 0 && player.zza > 0) {
                    double boost = 1.0 + Math.min(0.08, swimLvl * 0.004);
                    player.setDeltaMovement(player.getDeltaMovement().multiply(boost, 1.0, boost));
                }
            });
        }
    }

    public static void syncAndCheckMilestones(ServerPlayerEntity player, boolean forceSync) {
        player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
            BetterLevelingBridge.ScanResult scan = BetterLevelingBridge.scanPlayerBetterLeveling(player);
            int currentMilestones = scan.totalMilestones;
            int lastRewarded = cap.getLastRewardedMilestones();

            boolean changed = false;
            if (currentMilestones > lastRewarded) {
                int newlyEarned = currentMilestones - lastRewarded;
                cap.addPoints(newlyEarned);
                cap.setLastRewardedMilestones(currentMilestones);
                changed = true;

                player.displayClientMessage(
                        new TranslationTextComponent("msg.blcosmicaddon.point_gained", newlyEarned, cap.getAvailablePoints()),
                        false
                );
                player.level.playSound(null, player.blockPosition(),
                        SoundEvents.PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.9F, 1.25F);
            }

            if (changed || forceSync) {
                applyPassiveAttributes(player, cap);
                PacketHandler.sendToPlayer(player, new SyncCosmicDataPacket(cap.serializeNBT()));
            }
        });
    }

    /**
     * Применяет постоянные модификаторы атрибутов:
     * - Скорость бега (move_speed_root, wind_step, momentum_miner, warp_phantom, nebula_synthesis_left, cosmic_transcendence)
     * - Максимальное здоровье (vitality_star, nebula_synthesis_left, cosmic_transcendence)
     * - Скорость атаки (attack_speed_node, cosmic_transcendence)
     * - Броня (iron_skin_cosmos, cosmic_transcendence)
     * - Удача (angler_reflex, poseidon_bounty)
     */
    public static void applyPassiveAttributes(PlayerEntity player, ICosmicSkillData cap) {
        double transcendencePct = cap.getNodeLevel("cosmic_transcendence") * 0.02; // +2% за уровень финала

        // 1. Скорость бега (процентный мультипликатор)
        double moveSpeedPct = (cap.getNodeLevel("move_speed_root") * 0.0075)
                + (cap.getNodeLevel("wind_step") * 0.008)
                + (cap.getNodeLevel("momentum_miner") * 0.01)
                + (cap.getNodeLevel("warp_phantom") * 0.02)
                + (cap.getNodeLevel("nebula_synthesis_left") * 0.02)
                + transcendencePct;
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVE_SPEED_UUID,
                "Cosmic Move Speed", moveSpeedPct, AttributeModifier.Operation.MULTIPLY_BASE);

        // 2. Максимальное здоровье (плоский бонус ХП)
        double bonusHealth = (cap.getNodeLevel("vitality_star") * 1.0)
                + (cap.getNodeLevel("nebula_synthesis_left") * 2.0)
                + (cap.getNodeLevel("cosmic_transcendence") * 2.0);
        applyModifier(player.getAttribute(Attributes.MAX_HEALTH), MAX_HEALTH_UUID,
                "Cosmic Vitality", bonusHealth, AttributeModifier.Operation.ADDITION);

        // 3. Скорость атаки (%)
        double attackSpeedPct = (cap.getNodeLevel("attack_speed_node") * 0.0125)
                + transcendencePct;
        applyModifier(player.getAttribute(Attributes.ATTACK_SPEED), ATTACK_SPEED_UUID,
                "Cosmic Attack Speed", attackSpeedPct, AttributeModifier.Operation.MULTIPLY_BASE);

        // 4. Броня (%)
        double armorPct = (cap.getNodeLevel("iron_skin_cosmos") * 0.0125)
                + transcendencePct;
        applyModifier(player.getAttribute(Attributes.ARMOR), ARMOR_UUID,
                "Cosmic Armor Boost", armorPct, AttributeModifier.Operation.MULTIPLY_TOTAL);

        // 5. Удача (для рыбалки и сундуков)
        double luckBonus = (cap.getNodeLevel("angler_reflex") * 0.15)
                + (cap.getNodeLevel("poseidon_bounty") * 0.4);
        applyModifier(player.getAttribute(Attributes.LUCK), LUCK_UUID,
                "Cosmic Luck", luckBonus, AttributeModifier.Operation.ADDITION);
    }

    private static void applyModifier(ModifiableAttributeInstance instance, UUID uuid, String name,
                                      double amount, AttributeModifier.Operation op) {
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(uuid);
        if (existing != null) {
            if (Math.abs(existing.getAmount() - amount) < 1e-5) {
                return;
            }
            instance.removeModifier(uuid);
        }
        if (amount > 0) {
            instance.addPermanentModifier(new AttributeModifier(uuid, name, amount, op));
        }
    }

    // =========================================================================
    // СКОРОСТЬ КОПАНИЯ (BreakSpeed)
    // =========================================================================
    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        PlayerEntity player = event.getPlayer();
        player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
            double bonusPct = (cap.getNodeLevel("mining_speed_root") * 0.015) // +1.5% за ур.
                    + (cap.getNodeLevel("haste_pulse") * 0.02)                // +2.0% за ур.
                    + (cap.getNodeLevel("vein_resonance") * 0.03)             // +3.0% за ур.
                    + (cap.getNodeLevel("tectonic_overlord") * 0.05)          // +5.0% за ур.
                    + (cap.getNodeLevel("momentum_miner") * 0.01)             // +1.0% за ур.
                    + (cap.getNodeLevel("nebula_synthesis_left") * 0.02)      // +2.0% за ур.
                    + (cap.getNodeLevel("cosmic_transcendence") * 0.02);      // +2.0% за ур.

            // Навык "Мастер Глубинных Пластов" (deep_miner) даёт полный бонус ниже Y=45 и половинный выше
            int deepLvl = cap.getNodeLevel("deep_miner");
            if (deepLvl > 0) {
                if (player.getY() <= 45) {
                    bonusPct += deepLvl * 0.025;
                } else {
                    bonusPct += deepLvl * 0.01;
                }
            }

            if (bonusPct > 0) {
                event.setNewSpeed((float) (event.getOriginalSpeed() * (1.0 + bonusPct)));
            }
        });
    }

    // =========================================================================
    // ДОБЫЧА БЛОКОВ И РУД (BlockEvent.BreakEvent)
    // =========================================================================
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player == null || player.level.isClientSide || player.isCreative()) return;

        BlockState state = event.getState();
        String blockRegName = state.getBlock().getRegistryName() != null
                ? state.getBlock().getRegistryName().toString().toLowerCase() : "";
        boolean isOre = blockRegName.contains("ore") || state.getHarvestTool() == ToolType.PICKAXE && event.getExpToDrop() > 0;

        player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
            // 1. Бонус опыта за руду (vein_resonance + smelting_mastery)
            int veinLvl = cap.getNodeLevel("vein_resonance");
            int smeltLvl = cap.getNodeLevel("smelting_mastery");
            if (isOre && (veinLvl > 0 || smeltLvl > 0)) {
                int baseExp = Math.max(1, event.getExpToDrop());
                int extraExp = (int) Math.ceil(baseExp * (veinLvl * 0.05 + smeltLvl * 0.03));
                event.setExpToDrop(event.getExpToDrop() + extraExp);
            }

            // 2. Шанс сохранить прочность инструмента (unbreaking_spirit)
            int unbreakLvl = cap.getNodeLevel("unbreaking_spirit");
            if (unbreakLvl > 0) {
                ItemStack held = player.getMainHandItem();
                if (!held.isEmpty() && held.isDamageableItem() && held.getDamageValue() > 0) {
                    double saveChance = unbreakLvl * 0.015; // до 30%
                    if (random.nextDouble() < saveChance) {
                        held.setDamageValue(Math.max(0, held.getDamageValue() - 1));
                    }
                }
            }

            // 3. Геолог Звёздных Кристаллов (gem_prospector) — шанс выпадения алмаза/изумруда/кварца из руд или камня
            int gemLvl = cap.getNodeLevel("gem_prospector") + cap.getNodeLevel("tectonic_overlord");
            if (gemLvl > 0 && (isOre || blockRegName.contains("stone"))) {
                double gemChance = gemLvl * (isOre ? 0.008 : 0.0012);
                if (random.nextDouble() < gemChance && event.getWorld() instanceof ServerWorld) {
                    ServerWorld sw = (ServerWorld) event.getWorld();
                    ItemStack bonusGem = random.nextBoolean()
                            ? new ItemStack(Items.EMERALD)
                            : (random.nextInt(3) == 0 ? new ItemStack(Items.DIAMOND) : new ItemStack(Items.LAPIS_LAZULI, 2));
                    spawnItemAt(sw, event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5, bonusGem);
                }
            }

            // 4. Археолог Древних Миров (treasure_excavator) — клад при копке земли/песка/гравия
            int excavLvl = cap.getNodeLevel("treasure_excavator");
            if (excavLvl > 0 && (state.getHarvestTool() == ToolType.SHOVEL || blockRegName.contains("dirt") || blockRegName.contains("sand") || blockRegName.contains("gravel"))) {
                if (random.nextDouble() < excavLvl * 0.004 && event.getWorld() instanceof ServerWorld) {
                    ServerWorld sw = (ServerWorld) event.getWorld();
                    ItemStack[] treasures = new ItemStack[]{
                            new ItemStack(Items.GOLD_NUGGET, 2 + random.nextInt(3)),
                            new ItemStack(Items.IRON_NUGGET, 3 + random.nextInt(4)),
                            new ItemStack(Items.BONE, 1 + random.nextInt(2)),
                            new ItemStack(Items.FLINT, 2),
                            new ItemStack(Items.GOLD_INGOT, 1)
                    };
                    spawnItemAt(sw, event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5,
                            treasures[random.nextInt(treasures.length)]);
                }
            }
        });
    }

    // =========================================================================
    // УРОН, ПРОЦЕНТНЫЕ БОНУСЫ (+0.5%), КРИТЫ, ВАМПИРИЗМ И ЗАЩИТА (LivingHurtEvent)
    // =========================================================================
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        // А) КОГДА ИГРОК АТАКУЕТ ВРАГА
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof PlayerEntity) {
            PlayerEntity attacker = (PlayerEntity) sourceEntity;
            LivingEntity target = event.getEntityLiving();

            attacker.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
                float damage = event.getAmount();

                // 1. Процентное повышение общего урона (+0.5% за уровень damage_root и других узлов)
                double damageBonusPct = (cap.getNodeLevel("damage_root") * 0.005)         // +0.5% за ур.
                        + (cap.getNodeLevel("heavy_crusher") * 0.006)                     // +0.6% за ур.
                        + (cap.getNodeLevel("cosmic_warlord") * 0.025)                    // +2.5% за ур.
                        + (cap.getNodeLevel("nebula_synthesis_right") * 0.015)            // +1.5% за ур.
                        + (cap.getNodeLevel("abyssal_ascension") * 0.025)                 // +2.5% за ур.
                        + (cap.getNodeLevel("cosmic_transcendence") * 0.02);              // +2.0% за ур.

                // 2. Дальний бой (archery_velocity)
                if (event.getSource().isProjectile()) {
                    damageBonusPct += cap.getNodeLevel("archery_velocity") * 0.01;
                }

                // 3. Урон по боссам и элитным мобам (boss_slayer, если макс ХП цели >= 50)
                int bossLvl = cap.getNodeLevel("boss_slayer");
                if (bossLvl > 0 && target != null && target.getMaxHealth() >= 50.0F) {
                    damageBonusPct += bossLvl * 0.0125;
                }

                // 4. Приговор Палача (executioner_strike, если у цели < 35% ХП)
                int execLvl = cap.getNodeLevel("executioner_strike");
                if (execLvl > 0 && target != null && (target.getHealth() / target.getMaxHealth()) <= 0.35F) {
                    damageBonusPct += execLvl * 0.02;
                }

                damage *= (float) (1.0 + damageBonusPct);

                // 5. Критический удар (crit_chance_node + crit_damage_node + meteor_forged_blade)
                double critChance = (cap.getNodeLevel("crit_chance_node") * 0.008)        // +0.8% за ур.
                        + (cap.getNodeLevel("cosmic_warlord") * 0.015)
                        + (cap.getNodeLevel("cosmic_transcendence") * 0.01);

                if (critChance > 0 && random.nextDouble() < critChance) {
                    double critMultiplier = 1.50
                            + (cap.getNodeLevel("crit_damage_node") * 0.015)
                            + (cap.getNodeLevel("meteor_forged_blade") * 0.02)
                            + (cap.getNodeLevel("cosmic_warlord") * 0.025);
                    damage *= (float) critMultiplier;

                    if (attacker.level instanceof ServerWorld && target != null) {
                        ((ServerWorld) attacker.level).sendParticles(ParticleTypes.CRIT,
                                target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                                12, 0.3, 0.3, 0.3, 0.15);
                        attacker.level.playSound(null, attacker.blockPosition(),
                                SoundEvents.PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.7F, 1.2F);
                    }
                }

                // 6. Метеоритная Закалка Оружия (meteor_forged_blade) — поджог цели
                int meteorLvl = cap.getNodeLevel("meteor_forged_blade");
                if (meteorLvl > 0 && target != null && random.nextDouble() < meteorLvl * 0.04) {
                    target.setSecondsOnFire(3 + meteorLvl / 3);
                }

                // 7. Пробитие брони (armor_penetration) — добавляет чистый бонус сквозь броню
                int penLvl = cap.getNodeLevel("armor_penetration");
                if (penLvl > 0 && target != null && target.getArmorValue() > 0) {
                    damage *= (float) (1.0 + penLvl * 0.01);
                }

                event.setAmount(damage);

                // 8. Вампиризм Чёрной Дыры (lifesteal_fang)
                double lifestealPct = (cap.getNodeLevel("lifesteal_fang") * 0.0035)
                        + (cap.getNodeLevel("cosmic_warlord") * 0.005);
                if (lifestealPct > 0 && damage > 0) {
                    float heal = (float) (damage * lifestealPct);
                    if (heal >= 0.1F) {
                        attacker.heal(heal);
                    }
                }
            });
        }

        // Б) КОГДА ИГРОК ПОЛУЧАЕТ УРОН (Защита, Уклонение, Второе дыхание)
        if (event.getEntityLiving() instanceof PlayerEntity) {
            PlayerEntity defender = (PlayerEntity) event.getEntityLiving();
            defender.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
                DamageSource src = event.getSource();

                // 1. Матрица Уклонения (evasion_matrix + warp_phantom)
                if (!src.isBypassInvul()) {
                    double dodgeChance = (cap.getNodeLevel("evasion_matrix") * 0.0075)
                            + (cap.getNodeLevel("warp_phantom") * 0.02);
                    if (dodgeChance > 0 && random.nextDouble() < Math.min(0.45, dodgeChance)) {
                        event.setCanceled(true);
                        defender.level.playSound(null, defender.blockPosition(),
                                SoundEvents.ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.5F, 1.6F);
                        return;
                    }
                }

                // 2. Снижение физического урона (stone_warden)
                double damageReduction = cap.getNodeLevel("stone_warden") * 0.008;

                // 3. Снижение урона от огня, лавы и магии (Astral_strider_hybrid)
                if (src.isFire() || src.isMagic()) {
                    damageReduction += cap.getNodeLevel("Astral_strider_hybrid") * 0.02;
                }

                // 4. Второе дыхание Пустоты (second_wind) при ХП < 35%
                int secondWindLvl = cap.getNodeLevel("second_wind");
                if (secondWindLvl > 0 && (defender.getHealth() / defender.getMaxHealth()) <= 0.35F) {
                    damageReduction += secondWindLvl * 0.03;
                }

                damageReduction += cap.getNodeLevel("cosmic_transcendence") * 0.01;

                if (damageReduction > 0) {
                    double clampedReduction = Math.min(0.70, damageReduction);
                    event.setAmount((float) (event.getAmount() * (1.0 - clampedReduction)));
                }
            });
        }
    }

    // =========================================================================
    // СНИЖЕНИЕ УРОНА ОТ ПАДЕНИЯ (feather_gravity)
    // =========================================================================
    @SubscribeEvent
    public void onLivingFall(LivingFallEvent event) {
        if (event.getEntityLiving() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntityLiving();
            player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
                int featherLvl = cap.getNodeLevel("feather_gravity") + cap.getNodeLevel("warp_phantom");
                if (featherLvl > 0) {
                    double reduction = Math.min(0.85, featherLvl * 0.03);
                    event.setDamageMultiplier((float) (event.getDamageMultiplier() * (1.0 - reduction)));
                }
            });
        }
    }

    // =========================================================================
    // ПОВЫШЕНИЕ ШАНСА ДРОПА С МОБОВ И РЕДКИЕ ТРОФЕИ (LivingDropsEvent)
    // =========================================================================
    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) killer;
        if (player.level.isClientSide) return;

        player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
            // 1. Ловец Трофеев (mob_loot_chance + poseidon_bounty + abyssal_ascension) — шанс удвоить каждый предмет дропа
            double doubleDropChance = (cap.getNodeLevel("mob_loot_chance") * 0.012)
                    + (cap.getNodeLevel("poseidon_bounty") * 0.04)
                    + (cap.getNodeLevel("abyssal_ascension") * 0.025)
                    + (cap.getNodeLevel("cosmic_transcendence") * 0.02);

            if (doubleDropChance > 0) {
                List<ItemEntity> extraDrops = new ArrayList<>();
                for (ItemEntity itemEntity : event.getDrops()) {
                    ItemStack stack = itemEntity.getItem();
                    // Удваиваем стакающиеся ресурсы/лут с мобов (не дублируем уникальную броню с maxStackSize == 1)
                    if (stack.getMaxStackSize() > 1 && random.nextDouble() < doubleDropChance) {
                        ItemStack copy = stack.copy();
                        if (doubleDropChance > 1.0 && random.nextDouble() < (doubleDropChance - 1.0)) {
                            copy.setCount(Math.min(copy.getMaxStackSize(), copy.getCount() * 2));
                        }
                        extraDrops.add(new ItemEntity(
                                itemEntity.level,
                                itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                                copy
                        ));
                    }
                }
                event.getDrops().addAll(extraDrops);
            }

            // 2. Инкассатор Реликвий (rare_mob_trophy) — шанс редкого трофея с мобов
            int trophyLvl = cap.getNodeLevel("rare_mob_trophy") + cap.getNodeLevel("poseidon_bounty");
            if (trophyLvl > 0 && random.nextDouble() < trophyLvl * 0.006) {
                LivingEntity victim = event.getEntityLiving();
                ItemStack[] trophies = new ItemStack[]{
                        new ItemStack(Items.ENDER_PEARL, 1),
                        new ItemStack(Items.EMERALD, 1 + random.nextInt(2)),
                        new ItemStack(Items.GOLD_INGOT, 1 + random.nextInt(2)),
                        new ItemStack(Items.EXPERIENCE_BOTTLE, 2),
                        new ItemStack(Items.DIAMOND, 1)
                };
                ItemStack chosen = trophies[random.nextInt(trophies.length)];
                event.getDrops().add(new ItemEntity(victim.level, victim.getX(), victim.getY() + 0.5, victim.getZ(), chosen));
            }
        });
    }

    // =========================================================================
    // УВЕЛИЧЕНИЕ ОПЫТА С МОБОВ (hunter_xp_boost)
    // =========================================================================
    @SubscribeEvent
    public void onLivingXpDrop(LivingExperienceDropEvent event) {
        PlayerEntity player = event.getAttackingPlayer();
        if (player == null) return;

        player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
            double xpBoostPct = (cap.getNodeLevel("hunter_xp_boost") * 0.02)
                    + (cap.getNodeLevel("cosmic_transcendence") * 0.02);
            if (xpBoostPct > 0) {
                int newXp = (int) Math.ceil(event.getDroppedExperience() * (1.0 + xpBoostPct));
                event.setDroppedExperience(newXp);
            }
        });
    }

    // =========================================================================
    // РЫБАЛКА: БОЛЬШЕ КРЮЧКОВ НА ЛЕСКЕ И МОРСКИЕ СОКРОВИЩА (ItemFishedEvent)
    // =========================================================================
    @SubscribeEvent
    public void onItemFished(ItemFishedEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player == null || player.level.isClientSide) return;

        player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
            // 1. Многокрючковая Снасть (multi_hook_fishing + poseidon_bounty + abyssal_ascension)
            // Каждый дополнительный крючок даёт шанс вытянуть +1, +2 или +3 дополнительных улова за один раз!
            double multiHookChance = (cap.getNodeLevel("multi_hook_fishing") * 0.05) // до 75%
                    + (cap.getNodeLevel("poseidon_bounty") * 0.04)                   // +40%
                    + (cap.getNodeLevel("abyssal_ascension") * 0.025)                // +25%
                    + (cap.getNodeLevel("cosmic_transcendence") * 0.02);

            if (multiHookChance > 0 && !event.getDrops().isEmpty() && player.level instanceof ServerWorld) {
                ServerWorld sw = (ServerWorld) player.level;
                int extraHooksTriggered = (int) multiHookChance;
                double remainderChance = multiHookChance - extraHooksTriggered;
                if (random.nextDouble() < remainderChance) {
                    extraHooksTriggered++;
                }

                for (int h = 0; h < extraHooksTriggered; h++) {
                    for (ItemStack caught : event.getDrops()) {
                        ItemStack extraCatch = caught.copy();
                        spawnItemTowardsPlayer(sw, player, extraCatch);
                    }
                }
            }

            // 2. Искатель Сокровищ Океана (abyssal_treasure_fisher)
            int treasureLvl = cap.getNodeLevel("abyssal_treasure_fisher") + cap.getNodeLevel("poseidon_bounty");
            if (treasureLvl > 0 && random.nextDouble() < treasureLvl * 0.008 && player.level instanceof ServerWorld) {
                ServerWorld sw = (ServerWorld) player.level;
                ItemStack[] seaTreasures = new ItemStack[]{
                        new ItemStack(Items.NAUTILUS_SHELL, 1),
                        new ItemStack(Items.PRISMARINE_CRYSTALS, 2 + random.nextInt(3)),
                        new ItemStack(Items.PRISMARINE_SHARD, 2 + random.nextInt(3)),
                        new ItemStack(Items.GOLD_INGOT, 1 + random.nextInt(3)),
                        new ItemStack(Items.EMERALD, 1 + random.nextInt(2)),
                        new ItemStack(Items.DIAMOND, 1),
                        new ItemStack(Items.HEART_OF_THE_SEA, 1)
                };
                ItemStack bonusTreasure = seaTreasures[random.nextInt(seaTreasures.length)];
                spawnItemTowardsPlayer(sw, player, bonusTreasure);
            }

            // 3. Дополнительный опыт рыбалки (angler_reflex)
            int anglerLvl = cap.getNodeLevel("angler_reflex");
            if (anglerLvl > 0 && player.level instanceof ServerWorld) {
                int bonusXp = 1 + (anglerLvl / 3);
                player.level.addFreshEntity(new ExperienceOrbEntity(
                        player.level, player.getX(), player.getY() + 0.5, player.getZ(), bonusXp));
            }
        });
    }

    private static void spawnItemAt(ServerWorld world, double x, double y, double z, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity entity = new ItemEntity(world, x, y, z, stack);
        entity.setDefaultPickUpDelay();
        world.addFreshEntity(entity);
    }

    private static void spawnItemTowardsPlayer(ServerWorld world, PlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity entity = new ItemEntity(world, player.getX(), player.getY() + 0.6, player.getZ(), stack);
        entity.setPickUpDelay(0);
        world.addFreshEntity(entity);
    }
}
