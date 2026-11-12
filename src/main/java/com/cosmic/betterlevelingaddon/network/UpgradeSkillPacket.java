package com.cosmic.betterlevelingaddon.network;

import com.cosmic.betterlevelingaddon.capability.CosmicSkillCapability;
import com.cosmic.betterlevelingaddon.event.CommonForgeEvents;
import com.cosmic.betterlevelingaddon.skill.SkillNode;
import com.cosmic.betterlevelingaddon.skill.SkillTreeRegistry;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class UpgradeSkillPacket {
    private final String nodeId;

    public UpgradeSkillPacket(String nodeId) {
        this.nodeId = nodeId;
    }

    public static void encode(UpgradeSkillPacket pkt, PacketBuffer buf) {
        buf.writeUtf(pkt.nodeId, 64);
    }

    public static UpgradeSkillPacket decode(PacketBuffer buf) {
        return new UpgradeSkillPacket(buf.readUtf(64));
    }

    public static void handle(UpgradeSkillPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) return;

            SkillNode node = SkillTreeRegistry.getNode(pkt.nodeId);
            if (node == null) return;

            player.getCapability(CosmicSkillCapability.COSMIC_SKILL_CAP).ifPresent(cap -> {
                if (cap.upgradeNode(node)) {
                    // Применяем обновлённые атрибуты игрока (скорость бега, ХП, скорость атаки, броня)
                    CommonForgeEvents.applyPassiveAttributes(player, cap);
                    // Синхронизируем с клиентом
                    PacketHandler.sendToPlayer(player, new SyncCosmicDataPacket(cap.serializeNBT()));
                    // Звуковой эффект прокачки созвездия
                    float pitch = node.isKeystone() ? 0.85F : (1.1F + (cap.getNodeLevel(node.getId()) * 0.03F));
                    player.level.playSound(null, player.blockPosition(),
                            node.isKeystone() ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.EXPERIENCE_ORB_PICKUP,
                            SoundCategory.PLAYERS, 0.8F, Math.min(2.0F, pitch));
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
