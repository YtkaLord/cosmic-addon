package com.cosmic.betterlevelingaddon.command;

import com.cosmic.betterlevelingaddon.capability.CosmicSkillCapability;
import com.cosmic.betterlevelingaddon.event.CommonForgeEvents;
import com.cosmic.betterlevelingaddon.network.PacketHandler;
import com.cosmic.betterlevelingaddon.network.SyncCosmicDataPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;

/**
 * Команды администратора / тестирования:
 * /cosmicskills info - показать текущие очки и прогресс Better Leveling
 * /cosmicskills addpoints <кол-во> - выдать очки навыков (требует права оператора 2)
 * /cosmicskills reset - сбросить распределённые навыки и вернуть очки (требует права оператора 2)
 */
public class CosmicSkillCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("cosmicskills")
                .then(Commands.literal("info")
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrException();
                            CommonForgeEvents.syncAndCheckMilestones(player, true);
                            player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
                                ctx.getSource().sendSuccess(new StringTextComponent(
                                        "§b✦ [Космическое Дерево] §fДоступно очков: §a" + cap.getAvailablePoints()
                                                + " §7| Получено вех Better Leveling (по 5 ур.): §e" + cap.getLastRewardedMilestones()
                                ), false);
                            });
                            return 1;
                        }))
                .then(Commands.literal("addpoints")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 10000))
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrException();
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
                                        cap.addPoints(amount);
                                        PacketHandler.sendToPlayer(player, new SyncCosmicDataPacket(cap.serializeNBT()));
                                        ctx.getSource().sendSuccess(new StringTextComponent(
                                                "§b✦ [Космическое Дерево] §fНачислено §a+" + amount
                                                        + " §fочков навыков. Всего: §b" + cap.getAvailablePoints()
                                        ), true);
                                    });
                                    return 1;
                                })))
                .then(Commands.literal("reset")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrException();
                            player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
                                cap.resetAllSkills();
                                CommonForgeEvents.applyPassiveAttributes(player, cap);
                                PacketHandler.sendToPlayer(player, new SyncCosmicDataPacket(cap.serializeNBT()));
                                ctx.getSource().sendSuccess(new StringTextComponent(
                                        "§b✦ [Космическое Дерево] §fВсе навыки сброшены, очки возвращены (§a"
                                                + cap.getAvailablePoints() + "§f)!"
                                ), true);
                            });
                            return 1;
                        }))
        );
    }
}
