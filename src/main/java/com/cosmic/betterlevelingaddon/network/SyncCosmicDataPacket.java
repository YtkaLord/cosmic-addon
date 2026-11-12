package com.cosmic.betterlevelingaddon.network;

import com.cosmic.betterlevelingaddon.client.ClientSkillCache;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncCosmicDataPacket {
    private final CompoundNBT data;

    public SyncCosmicDataPacket(CompoundNBT data) {
        this.data = data;
    }

    public static void encode(SyncCosmicDataPacket pkt, PacketBuffer buf) {
        buf.writeNbt(pkt.data);
    }

    public static SyncCosmicDataPacket decode(PacketBuffer buf) {
        return new SyncCosmicDataPacket(buf.readNbt());
    }

    public static void handle(SyncCosmicDataPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (pkt.data != null) {
                ClientSkillCache.updateFromServer(pkt.data);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
