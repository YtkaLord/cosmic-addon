package com.cosmic.betterlevelingaddon.event;

import com.cosmic.betterlevelingaddon.BetterLevelingCosmicAddon;
import com.cosmic.betterlevelingaddon.capability.CosmicSkillCapability;
import com.cosmic.betterlevelingaddon.capability.ICosmicSkillData;
import com.cosmic.betterlevelingaddon.compat.BetterLevelingBridge;
import com.cosmic.betterlevelingaddon.compat.StackSizeFixer;
import com.cosmic.betterlevelingaddon.network.PacketHandler;
import com.cosmic.betterlevelingaddon.network.SyncCosmicDataPacket;
import net.minecraft.block.*;
import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.Property;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;

import java.util.*;

public class CommonForgeEvents {

    private static final ResourceLocation CAP_ID = new ResourceLocation(BetterLevelingCosmicAddon.MOD_ID, "cosmic_skills");

    private static final UUID MOVE_SPEED_UUID = UUID.fromString("91aeaa56-376b-4498-935b-2f7f68070635");
    private static final UUID MAX_HEALTH_UUID = UUID.fromString("92aeaa56-376b-4498-935b-2f7f68070636");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("93aeaa56-376b-4498-935b-2f7f68070637");
    private static final UUID ARMOR_UUID = UUID.fromString("94aeaa56-376b-4498-935b-2f7f68070638");
    private static final UUID LUCK_UUID = UUID.fromString("95aeaa56-376b-4498-935b-2f7f68070639");
    private static final UUID REACH_UUID = UUID.fromString("96aeaa56-376b-4498-935b-2f7f68070640");
    private static final UUID KNOCKBACK_RES_UUID = UUID.fromString("97aeaa56-376b-4498-935b-2f7f68070641");

    private final Random random = new Random();
    private final Map<UUID, Long> rightClickCropCooldown = new HashMap<>();

    // =========================================================================
    // ВОССТАНОВЛЕНИЕ СТАНДАРТНЫХ СТАКОВ (1 / 16 / 64) ПОСЛЕ ВСЕХ МОДОВ (IguanaTweaksReborn и др.)
    // =========================================================================
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTagsUpdated(TagsUpdatedEvent event) {
        StackSizeFixer.restoreStandardStackSizes();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onServerStarted(FMLServerStartedEvent event) {
        StackSizeFixer.restoreStandardStackSizes();
    }

    // =========================================================================
    // CAPABILITY И СИНХРОНИЗАЦИЯ
    // =========================================================================
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
            StackSizeFixer.restoreStandardStackSizes();
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

    // =========================================================================
    // ТИК ИГРОКА: ПРОВЕРКА УРОВНЕЙ PMMO/BL, РЕГЕНЕРАЦИЯ, МАГНИТ, НОЧНОЕ ЗРЕНИЕ, ЭНЕРГОЩИТ
    // =========================================================================
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        PlayerEntity player = event.player;
        if (player.level.isClientSide) return;

        // 1. Гравитационный Магнит (void_magnet) — притяжение предметов и опыта каждые 5 тиков
        if (player.tickCount % 5 == 0 && !player.isShiftKeyDown()) {
            player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
                int magnetLvl = cap.getNodeLevel("void_magnet");
                if (magnetLvl > 0) {
                    double radius = 2.0 + magnetLvl * 0.45;
                    AxisAlignedBB box = player.getBoundingBox().inflate(radius);
                    for (ItemEntity item : player.level.getEntitiesOfClass(ItemEntity.class, box)) {
                        if (item.isAlive() && !item.hasPickUpDelay()) {
                            double dx = player.getX() - item.getX();
                            double dy = (player.getY() + 0.5) - item.getY();
                            double dz = player.getZ() - item.getZ();
                            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                            if (dist > 0.3 && dist <= radius) {
                                item.setDeltaMovement(item.getDeltaMovement().add(dx / dist * 0.14, dy / dist * 0.14, dz / dist * 0.14));
                            }
                        }
                    }
                    for (ExperienceOrbEntity orb : player.level.getEntitiesOfClass(ExperienceOrbEntity.class, box)) {
                        if (orb.isAlive()) {
                            double dx = player.getX() - orb.getX();
                            double dy = (player.getY() + 0.5) - orb.getY();
                            double dz = player.getZ() - orb.getZ();
                            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                            if (dist > 0.3 && dist <= radius) {
                                orb.setDeltaMovement(orb.getDeltaMovement().add(dx / dist * 0.18, dy / dist * 0.18, dz / dist * 0.18));
                            }
                        }
                    }
                }
            });
        }

        // 2. Раз в секунду (20 тиков) проверяем вехи Project MMO и Better Leveling
        if (player.tickCount % 20 == 0 && player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            syncAndCheckMilestones(serverPlayer, false);

            player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
                // Регенерация (cosmic_regen + world_tree_guardian + cosmic_transcendence)
                if (player.tickCount % 40 == 0) {
                    int regenLvl = cap.getNodeLevel("cosmic_regen");
                    int treeLvl = cap.getNodeLevel("world_tree_guardian");
                    int transLvl = cap.getNodeLevel("cosmic_transcendence");
                    if ((regenLvl > 0 || treeLvl > 0 || transLvl > 0) && player.getHealth() < player.getMaxHealth()) {
                        float healAmount = (float) (regenLvl * 0.08 + treeLvl * 0.12 + transLvl * 0.15);
                        player.heal(healAmount);
                    }
                }

                // Ночное зрение в пещерах (cave_night_vision) при Y <= 35
                if (player.tickCount % 60 == 0 && cap.getNodeLevel("cave_night_vision") > 0 && player.getY() <= 35) {
                    player.addEffect(new EffectInstance(Effects.NIGHT_VISION, 260, 0, true, false, false));
                }

                // Энергощит Сверхновой (cosmic_shield_absorb + cosmic_archmage) каждые 12 секунд
                if (player.tickCount % 240 == 0) {
                    int shieldLvl = cap.getNodeLevel("cosmic_shield_absorb");
                    int archmageLvl = cap.getNodeLevel("cosmic_archmage");
                    if (shieldLvl > 0 || archmageLvl > 0) {
                        float maxAbsorb = (float) (shieldLvl * 0.5 + archmageLvl * 0.8);
                        if (player.getAbsorptionAmount() < maxAbsorb) {
                            player.setAbsorptionAmount(Math.min(maxAbsorb, player.getAbsorptionAmount() + 2.0F));
                        }
                    }
                }
            });
        }

        // 3. Бонус скорости в воде (swim_jet)
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
            Map<String, Integer> currentSkills = BetterLevelingBridge.scanAllSkillLevels(player);

            int newlyEarnedPoints = 0;
            int totalMilestonesAcrossAllSkills = 0;

            for (Map.Entry<String, Integer> entry : currentSkills.entrySet()) {
                String skillKey = entry.getKey();
                int skillLevel = entry.getValue();
                if (skillLevel <= 0 || skillLevel > 10000) continue;

                int reachedMilestone = skillLevel / 5;
                totalMilestonesAcrossAllSkills += reachedMilestone;

                if (reachedMilestone > 0) {
                    int previouslyRewardedForThisSkill = cap.getSkillMilestone(skillKey);
                    if (reachedMilestone > previouslyRewardedForThisSkill) {
                        newlyEarnedPoints += (reachedMilestone - previouslyRewardedForThisSkill);
                        cap.setSkillMilestone(skillKey, reachedMilestone);
                    }
                }
            }

            boolean changed = false;
            if (newlyEarnedPoints > 0) {
                cap.addPoints(newlyEarnedPoints);
                cap.setLastRewardedMilestones(Math.max(cap.getLastRewardedMilestones() + newlyEarnedPoints, totalMilestonesAcrossAllSkills));
                changed = true;

                player.displayClientMessage(
                        new TranslationTextComponent("msg.blcosmicaddon.point_gained", newlyEarnedPoints, cap.getAvailablePoints()),
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

    public static void applyPassiveAttributes(PlayerEntity player, ICosmicSkillData cap) {
        double transcendencePct = cap.getNodeLevel("cosmic_transcendence") * 0.02;

        // 1. Скорость бега (%)
        double moveSpeedPct = (cap.getNodeLevel("move_speed_root") * 0.0075)
                + (cap.getNodeLevel("wind_step") * 0.008)
                + (cap.getNodeLevel("momentum_miner") * 0.01)
                + (cap.getNodeLevel("warp_phantom") * 0.02)
                + (cap.getNodeLevel("nebula_synthesis_left") * 0.02)
                + transcendencePct;
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), MOVE_SPEED_UUID,
                "Cosmic Move Speed", moveSpeedPct, AttributeModifier.Operation.MULTIPLY_BASE);

        // 2. Максимальное здоровье
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

        // 5. Удача (Luck)
        double luckBonus = (cap.getNodeLevel("angler_reflex") * 0.15)
                + (cap.getNodeLevel("dungeon_luck") * 0.20)
                + (cap.getNodeLevel("poseidon_bounty") * 0.40);
        applyModifier(player.getAttribute(Attributes.LUCK), LUCK_UUID,
                "Cosmic Luck", luckBonus, AttributeModifier.Operation.ADDITION);

        // 6. Дальность дотягивания (Reach Distance)
        double reachBonus = cap.getNodeLevel("reach_extension") * 0.15;
        if (ForgeMod.REACH_DISTANCE.isPresent()) {
            applyModifier(player.getAttribute(ForgeMod.REACH_DISTANCE.get()), REACH_UUID,
                    "Cosmic Reach", reachBonus, AttributeModifier.Operation.ADDITION);
        }

        // 7. Сопротивление отбрасыванию (Knockback Resistance)
        double kbRes = Math.min(0.95, cap.getNodeLevel("gravity_anchor") * 0.03);
        applyModifier(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_RES_UUID,
                "Cosmic Gravity Anchor", kbRes, AttributeModifier.Operation.ADDITION);
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
    // СКОРОСТЬ КОПАНИЯ И РУБКИ ДЕРЕВА (BreakSpeed)
    // =========================================================================
    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        PlayerEntity player = event.getPlayer();
        BlockState state = event.getState();

        player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
            double bonusPct = (cap.getNodeLevel("mining_speed_root") * 0.015)
                    + (cap.getNodeLevel("haste_pulse") * 0.02)
                    + (cap.getNodeLevel("vein_resonance") * 0.03)
                    + (cap.getNodeLevel("tectonic_overlord") * 0.05)
                    + (cap.getNodeLevel("momentum_miner") * 0.01)
                    + (cap.getNodeLevel("nebula_synthesis_left") * 0.02)
                    + (cap.getNodeLevel("cosmic_transcendence") * 0.02);

            int deepLvl = cap.getNodeLevel("deep_miner");
            if (deepLvl > 0) {
                bonusPct += (player.getY() <= 45) ? (deepLvl * 0.025) : (deepLvl * 0.01);
            }

            int caveVisLvl = cap.getNodeLevel("cave_night_vision");
            if (caveVisLvl > 0 && player.getY() <= 35) {
                bonusPct += caveVisLvl * 0.025;
            }

            // Дровосек Туманности (lumberjack_fury) — ускорение рубки любых брёвен и дерева
            int lumberLvl = cap.getNodeLevel("lumberjack_fury");
            if (lumberLvl > 0 && (state.is(BlockTags.LOGS) || state.getHarvestTool() == ToolType.AXE)) {
                bonusPct += lumberLvl * 0.025;
            }

            if (bonusPct > 0) {
                event.setNewSpeed((float) (event.getOriginalSpeed() * (1.0 + bonusPct)));
            }
        });
    }

    // =========================================================================
    // СБОР МОДОВЫХ РАСТЕНИЙ ПКМ (Farmer's Delight, Mystical Agriculture и др.) -> PMMO FARMING XP!
    // =========================================================================
    @SubscribeEvent
    public void onRightClickCrop(PlayerInteractEvent.RightClickBlock event) {
        PlayerEntity player = event.getPlayer();
        if (player == null || player.level.isClientSide || !(player instanceof ServerPlayerEntity)) return;
        if (event.getItemStack().getItem() instanceof BoneMealItem) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getWorld().getBlockState(pos);
        Block block = state.getBlock();
        ResourceLocation reg = block.getRegistryName();
        if (reg == null || reg.getNamespace().equals("minecraft")) return;

        if (isModdedPlantOrCrop(block, reg) && hasGrowthStage(block, state) && isMaturePlant(block, state)) {
            long now = player.level.getGameTime();
            Long last = rightClickCropCooldown.get(player.getUUID());
            if (last != null && (now - last) < 4) return;
            rightClickCropCooldown.put(player.getUUID(), now);

            awardModdedFarmingXp((ServerPlayerEntity) player, 20.0, reg.toString());
        }
    }

    // =========================================================================
    // ДОБЫЧА БЛОКОВ, РУД, ДЕРЕВЬЕВ И МОДОВЫХ РАСТЕНИЙ (BlockEvent.BreakEvent)
    // =========================================================================
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player == null || player.level.isClientSide || player.isCreative()) return;

        BlockState state = event.getState();
        Block block = state.getBlock();
        ResourceLocation reg = block.getRegistryName();
        String blockRegName = reg != null ? reg.toString().toLowerCase() : "";
        boolean isModded = reg != null && !reg.getNamespace().equals("minecraft");
        boolean isOre = blockRegName.contains("ore") || (state.getHarvestTool() == ToolType.PICKAXE && event.getExpToDrop() > 0);
        boolean isPlant = isModdedPlantOrCrop(block, reg);

        // 1. ФИКС PROJECT MMO: Начисляем опыт Farming в Project MMO за модовые растения!
        //    - Зрелые модовые культуры (с возрастом AGE) -> 20.0 XP
        //    - Незрелые ростки -> 0 XP (защита от абьюза)
        //    - Модовые цветы, грибы и дикие растения (без стадий роста) -> 10.0 XP (как коричневый гриб в PMMO)
        if (isModded && isPlant && player instanceof ServerPlayerEntity) {
            double baseFarmingXp = 0.0;
            if (hasGrowthStage(block, state)) {
                if (isMaturePlant(block, state)) {
                    baseFarmingXp = 20.0;
                }
            } else {
                baseFarmingXp = 10.0;
            }
            if (baseFarmingXp > 0.0) {
                awardModdedFarmingXp((ServerPlayerEntity) player, baseFarmingXp, blockRegName);
            }
        }

        player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
            // 2. Щедрый Урожай (harvest_bounty + world_tree_guardian) — шанс удвоить только зрелые культуры
            if (isPlant && hasGrowthStage(block, state) && isMaturePlant(block, state) && event.getWorld() instanceof ServerWorld) {
                double harvestDoubleChance = (cap.getNodeLevel("harvest_bounty") * 0.02)
                        + (cap.getNodeLevel("world_tree_guardian") * 0.05);
                if (harvestDoubleChance > 0 && random.nextDouble() < harvestDoubleChance) {
                    ServerWorld sw = (ServerWorld) event.getWorld();
                    List<ItemStack> drops = Block.getDrops(state, sw, event.getPos(), sw.getBlockEntity(event.getPos()), player, player.getMainHandItem());
                    for (ItemStack drop : drops) {
                        if (!drop.isEmpty()) {
                            spawnItemAt(sw, event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5, drop.copy());
                        }
                    }
                }
            }

            // 3. Дровосек Туманности (lumberjack_fury) — шанс дополнительного бревна
            if (state.is(BlockTags.LOGS) && event.getWorld() instanceof ServerWorld) {
                int lumberLvl = cap.getNodeLevel("lumberjack_fury") + cap.getNodeLevel("world_tree_guardian");
                if (lumberLvl > 0 && random.nextDouble() < lumberLvl * 0.015) {
                    ServerWorld sw = (ServerWorld) event.getWorld();
                    spawnItemAt(sw, event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5, new ItemStack(block));
                }
            }

            // 4. Бонус ванильного опыта (vein_resonance + smelting_mastery + enchanting_wisdom + cosmic_archmage)
            double globalXpPct = (cap.getNodeLevel("enchanting_wisdom") * 0.025)
                    + (cap.getNodeLevel("cosmic_archmage") * 0.03)
                    + (cap.getNodeLevel("cosmic_transcendence") * 0.02);
            if (isOre) {
                globalXpPct += (cap.getNodeLevel("vein_resonance") * 0.05) + (cap.getNodeLevel("smelting_mastery") * 0.03);
            }
            if (event.getExpToDrop() > 0 && globalXpPct > 0) {
                int extraExp = (int) Math.ceil(event.getExpToDrop() * globalXpPct);
                event.setExpToDrop(event.getExpToDrop() + extraExp);
            }

            // 5. Нерушимый Инструмент (unbreaking_spirit)
            int unbreakLvl = cap.getNodeLevel("unbreaking_spirit");
            if (unbreakLvl > 0) {
                ItemStack held = player.getMainHandItem();
                if (!held.isEmpty() && held.isDamageableItem() && held.getDamageValue() > 0) {
                    if (random.nextDouble() < unbreakLvl * 0.015) {
                        held.setDamageValue(Math.max(0, held.getDamageValue() - 1));
                    }
                }
            }

            // 6. Геолог Звёздных Кристаллов (gem_prospector)
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

            // 7. Археолог Древних Миров (treasure_excavator)
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

    private void awardModdedFarmingXp(ServerPlayerEntity player, double baseXp, String blockId) {
        final double[] multiplier = {1.0};
        player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
            multiplier[0] += (cap.getNodeLevel("modded_farming_xp") * 0.04)
                    + (cap.getNodeLevel("world_tree_guardian") * 0.05)
                    + (cap.getNodeLevel("cosmic_transcendence") * 0.02);
        });
        BetterLevelingBridge.awardPmmoXp(player, "farming", baseXp * multiplier[0], "Harvested modded plant: " + blockId);
    }

    private boolean isModdedPlantOrCrop(Block block, ResourceLocation reg) {
        if (block instanceof CropsBlock || block instanceof BushBlock || block instanceof StemBlock
                || block instanceof AttachedStemBlock || block instanceof SweetBerryBushBlock
                || block instanceof CocoaBlock || block instanceof SugarCaneBlock
                || block instanceof CactusBlock || block instanceof NetherWartBlock
                || block instanceof IPlantable) {
            return true;
        }
        if (reg != null) {
            String path = reg.getPath().toLowerCase();
            return path.contains("crop") || path.contains("bush") || path.contains("berry")
                    || path.contains("plant") || path.contains("tomato") || path.contains("cabbage")
                    || path.contains("onion") || path.contains("rice") || path.contains("herb")
                    || path.contains("flower") || path.contains("wild_") || path.contains("_essence");
        }
        return false;
    }

    private boolean hasGrowthStage(Block block, BlockState state) {
        if (block instanceof CropsBlock || block instanceof SweetBerryBushBlock) {
            return true;
        }
        for (Property<?> prop : state.getProperties()) {
            if (prop instanceof IntegerProperty && prop.getName().equalsIgnoreCase("age")) {
                return true;
            }
        }
        return false;
    }

    private boolean isMaturePlant(Block block, BlockState state) {
        if (block instanceof CropsBlock) {
            return ((CropsBlock) block).isMaxAge(state);
        }
        if (block instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) >= 2;
        }
        for (Property<?> prop : state.getProperties()) {
            if (prop instanceof IntegerProperty && prop.getName().equalsIgnoreCase("age")) {
                IntegerProperty intProp = (IntegerProperty) prop;
                int val = state.getValue(intProp);
                int max = Collections.max(intProp.getPossibleValues());
                return val >= max;
            }
        }
        return true;
    }

    // =========================================================================
    // ЕДА И НАСЫЩЕНИЕ (gourmet_saturation)
    // =========================================================================
    @SubscribeEvent
    public void onFinishEating(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntityLiving() instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) event.getEntityLiving();
        if (player.level.isClientSide) return;
        if (!event.getItem().isEdible()) return;

        player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
            int gourmetLvl = cap.getNodeLevel("gourmet_saturation") + cap.getNodeLevel("world_tree_guardian");
            if (gourmetLvl > 0 && random.nextDouble() < gourmetLvl * 0.03) {
                player.getFoodData().eat(2, 0.6F);
            }
        });
    }

    // =========================================================================
    // УБИЙСТВО МОНСТРА: ИМПУЛЬС ОХОТНИКА (battle_momentum)
    // =========================================================================
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) killer;
        if (player.level.isClientSide) return;

        player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
            int momLvl = cap.getNodeLevel("battle_momentum");
            if (momLvl > 0) {
                int ticks = (int) ((1.5 + momLvl * 0.4) * 20);
                player.addEffect(new EffectInstance(Effects.MOVEMENT_SPEED, ticks, 1, false, false, true));
            }
        });
    }

    // =========================================================================
    // УРОН, КРИТЫ, БЕРСЕРК, НЕЖИТЬ, ШИПЫ, ЗАЩИТА ОТ МАГИИ И СТИХИЙ (LivingHurtEvent)
    // =========================================================================
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        // А) ИГРОК АТАКУЕТ ВРАГА
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof PlayerEntity) {
            PlayerEntity attacker = (PlayerEntity) sourceEntity;
            LivingEntity target = event.getEntityLiving();

            attacker.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
                float damage = event.getAmount();

                // 1. Общий процентный урон (+0.5% за уровень damage_root и т.д.)
                double damageBonusPct = (cap.getNodeLevel("damage_root") * 0.005)
                        + (cap.getNodeLevel("heavy_crusher") * 0.006)
                        + (cap.getNodeLevel("cosmic_warlord") * 0.025)
                        + (cap.getNodeLevel("nebula_synthesis_right") * 0.015)
                        + (cap.getNodeLevel("abyssal_ascension") * 0.025)
                        + (cap.getNodeLevel("cosmic_transcendence") * 0.02);

                // 2. Дальний бой (archery_velocity)
                if (event.getSource().isProjectile()) {
                    damageBonusPct += cap.getNodeLevel("archery_velocity") * 0.01;
                }

                // 3. Убийца боссов (boss_slayer)
                int bossLvl = cap.getNodeLevel("boss_slayer");
                if (bossLvl > 0 && target != null && target.getMaxHealth() >= 50.0F) {
                    damageBonusPct += bossLvl * 0.0125;
                }

                // 4. Приговор Палача (executioner_strike)
                int execLvl = cap.getNodeLevel("executioner_strike");
                if (execLvl > 0 && target != null && (target.getHealth() / target.getMaxHealth()) <= 0.35F) {
                    damageBonusPct += execLvl * 0.02;
                }

                // 5. Ярость Берсерка (berserker_rage) — когда у игрока < 50% ХП
                int berserkLvl = cap.getNodeLevel("berserker_rage");
                if (berserkLvl > 0 && (attacker.getHealth() / attacker.getMaxHealth()) <= 0.50F) {
                    damageBonusPct += berserkLvl * 0.015;
                }

                // 6. Инквизитор Нежити (undead_purger)
                int undeadLvl = cap.getNodeLevel("undead_purger");
                if (undeadLvl > 0 && target != null && target.getMobType() == CreatureAttribute.UNDEAD) {
                    damageBonusPct += undeadLvl * 0.015;
                }

                // 7. Сокрушитель Чемпионов (champion_breaker) — в Незере, Энде и других модовых измерениях
                int champLvl = cap.getNodeLevel("champion_breaker");
                if (champLvl > 0 && !attacker.level.dimension().equals(World.OVERWORLD)) {
                    damageBonusPct += champLvl * 0.015;
                }

                damage *= (float) (1.0 + damageBonusPct);

                // 8. Критический удар
                double critChance = (cap.getNodeLevel("crit_chance_node") * 0.008)
                        + (cap.getNodeLevel("cosmic_warlord") * 0.015)
                        + (cap.getNodeLevel("nebula_synthesis_right") * 0.01)
                        + (cap.getNodeLevel("cosmic_transcendence") * 0.01);

                if (critChance > 0 && random.nextDouble() < critChance) {
                    double critMultiplier = 1.50
                            + (cap.getNodeLevel("crit_damage_node") * 0.015)
                            + (cap.getNodeLevel("meteor_forged_blade") * 0.02)
                            + (cap.getNodeLevel("cosmic_archmage") * 0.02)
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

                // 9. Метеоритная Закалка (поджог)
                int meteorLvl = cap.getNodeLevel("meteor_forged_blade");
                if (meteorLvl > 0 && target != null && random.nextDouble() < meteorLvl * 0.04) {
                    target.setSecondsOnFire(3 + meteorLvl / 3);
                }

                // 10. Пробитие брони
                int penLvl = cap.getNodeLevel("armor_penetration");
                if (penLvl > 0 && target != null && target.getArmorValue() > 0) {
                    damage *= (float) (1.0 + penLvl * 0.01);
                }

                event.setAmount(damage);

                // 11. Вампиризм
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

        // Б) ИГРОК ПОЛУЧАЕТ УРОН
        if (event.getEntityLiving() instanceof PlayerEntity) {
            PlayerEntity defender = (PlayerEntity) event.getEntityLiving();
            defender.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
                DamageSource src = event.getSource();

                // 1. Уклонение
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

                // 2. Шипы Сверхновой (supernova_thorns) — отражение урона атакующему
                int thornsLvl = cap.getNodeLevel("supernova_thorns");
                if (thornsLvl > 0 && src.getEntity() instanceof LivingEntity && src.getEntity() != defender) {
                    float reflected = (float) (event.getAmount() * (thornsLvl * 0.02));
                    if (reflected >= 0.5F) {
                        src.getEntity().hurt(DamageSource.thorns(defender), reflected);
                    }
                }

                double damageReduction = cap.getNodeLevel("stone_warden") * 0.008;

                // Защита от магии (arcane_ward + Astral_strider_hybrid + cosmic_archmage)
                if (src.isMagic()) {
                    damageReduction += (cap.getNodeLevel("arcane_ward") * 0.015)
                            + (cap.getNodeLevel("Astral_strider_hybrid") * 0.02)
                            + (cap.getNodeLevel("cosmic_archmage") * 0.025);
                }

                // Защита от огня, лавы, стихий (thermal_suit + Astral_strider_hybrid)
                if (src.isFire() || src == DamageSource.LIGHTNING_BOLT || src == DamageSource.IN_WALL) {
                    damageReduction += (cap.getNodeLevel("thermal_suit") * 0.025)
                            + (cap.getNodeLevel("Astral_strider_hybrid") * 0.02);
                }

                // Второе дыхание (second_wind) при ХП <= 35%
                int secondWindLvl = cap.getNodeLevel("second_wind");
                if (secondWindLvl > 0 && (defender.getHealth() / defender.getMaxHealth()) <= 0.35F) {
                    damageReduction += secondWindLvl * 0.03;
                }

                damageReduction += cap.getNodeLevel("cosmic_transcendence") * 0.01;

                if (damageReduction > 0) {
                    double clampedReduction = Math.min(0.75, damageReduction);
                    event.setAmount((float) (event.getAmount() * (1.0 - clampedReduction)));
                }
            });
        }
    }

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

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) killer;
        if (player.level.isClientSide) return;

        player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
            double doubleDropChance = (cap.getNodeLevel("mob_loot_chance") * 0.012)
                    + (cap.getNodeLevel("poseidon_bounty") * 0.04)
                    + (cap.getNodeLevel("abyssal_ascension") * 0.025)
                    + (cap.getNodeLevel("cosmic_transcendence") * 0.02);

            if (doubleDropChance > 0) {
                List<ItemEntity> extraDrops = new ArrayList<>();
                for (ItemEntity itemEntity : event.getDrops()) {
                    ItemStack stack = itemEntity.getItem();
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

    @SubscribeEvent
    public void onLivingXpDrop(LivingExperienceDropEvent event) {
        PlayerEntity player = event.getAttackingPlayer();
        if (player == null) return;

        player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
            double xpBoostPct = (cap.getNodeLevel("hunter_xp_boost") * 0.02)
                    + (cap.getNodeLevel("enchanting_wisdom") * 0.025)
                    + (cap.getNodeLevel("cosmic_archmage") * 0.03)
                    + (cap.getNodeLevel("cosmic_transcendence") * 0.02);
            if (xpBoostPct > 0) {
                int newXp = (int) Math.ceil(event.getDroppedExperience() * (1.0 + xpBoostPct));
                event.setDroppedExperience(newXp);
            }
        });
    }

    @SubscribeEvent
    public void onItemFished(ItemFishedEvent event) {
        PlayerEntity player = event.getPlayer();
        if (player == null || player.level.isClientSide) return;

        if (player instanceof ServerPlayerEntity) {
            syncAndCheckMilestones((ServerPlayerEntity) player, false);
        }

        player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
            double multiHookChance = (cap.getNodeLevel("multi_hook_fishing") * 0.05)
                    + (cap.getNodeLevel("poseidon_bounty") * 0.04)
                    + (cap.getNodeLevel("abyssal_ascension") * 0.025)
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
                        spawnItemTowardsPlayer(sw, player, caught.copy());
                    }
                }
            }

            int treasureLvl = cap.getNodeLevel("abyssal_treasure_fisher")
                    + cap.getNodeLevel("deep_sea_angler")
                    + cap.getNodeLevel("poseidon_bounty");
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
                spawnItemTowardsPlayer(sw, player, seaTreasures[random.nextInt(seaTreasures.length)]);
            }

            int anglerLvl = cap.getNodeLevel("angler_reflex") + cap.getNodeLevel("deep_sea_angler");
            if (anglerLvl > 0 && player.level instanceof ServerWorld) {
                int bonusXp = 1 + (anglerLvl / 3);
                player.level.addFreshEntity(new ExperienceOrbEntity(
                        player.level, player.getX(), player.getY() + 0.5, player.getZ(), bonusXp));
                if (player instanceof ServerPlayerEntity && cap.getNodeLevel("deep_sea_angler") > 0) {
                    BetterLevelingBridge.awardPmmoXp((ServerPlayerEntity) player, "fishing",
                            cap.getNodeLevel("deep_sea_angler") * 4.0, "Cosmic Deep Sea Angler");
                }
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
