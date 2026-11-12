package com.cosmic.betterlevelingaddon.client;

import com.cosmic.betterlevelingaddon.capability.CosmicSkillData;
import com.cosmic.betterlevelingaddon.capability.ICosmicSkillData;
import net.minecraft.nbt.CompoundNBT;

public class ClientSkillCache {
    private static final ICosmicSkillData CLIENT_DATA = new CosmicSkillData();

    public static void updateFromServer(CompoundNBT nbt) {
        CLIENT_DATA.deserializeNBT(nbt);
    }

    public static ICosmicSkillData get() {
        return CLIENT_DATA;
    }
}
