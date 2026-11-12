package com.cosmic.betterlevelingaddon.capability;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CosmicSkillCapability {
    @CapabilityInject(ICosmicSkillData.class)
    public static Capability<ICosmicSkillData> COSMIC_SKILL_CAP = null;

    public static void register() {
        CapabilityManager.INSTANCE.register(
                ICosmicSkillData.class,
                new Capability.IStorage<ICosmicSkillData>() {
                    @Nullable
                    @Override
                    public INBT writeNBT(Capability<ICosmicSkillData> capability, ICosmicSkillData instance, Direction side) {
                        return instance.serializeNBT();
                    }

                    @Override
                    public void readNBT(Capability<ICosmicSkillData> capability, ICosmicSkillData instance, Direction side, INBT nbt) {
                        if (nbt instanceof CompoundNBT) {
                            instance.deserializeNBT((CompoundNBT) nbt);
                        }
                    }
                },
                CosmicSkillData::new
        );
    }

    public static class Provider implements ICapabilitySerializable<CompoundNBT> {
        private final CosmicSkillData backend = new CosmicSkillData();
        private final LazyOptional<ICosmicSkillData> optional = LazyOptional.of(() -> backend);

        @Nonnull
        @Override
        public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
            return cap == COSMIC_SKILL_CAP ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundNBT serializeNBT() {
            return backend.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundNBT nbt) {
            backend.deserializeNBT(nbt);
        }

        public void invalidate() {
            optional.invalidate();
        }
    }
}
