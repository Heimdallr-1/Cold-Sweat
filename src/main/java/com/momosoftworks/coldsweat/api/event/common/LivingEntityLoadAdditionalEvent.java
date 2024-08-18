package com.momosoftworks.coldsweat.api.event.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

public class LivingEntityLoadAdditionalEvent extends Event
{
    private final LivingEntity entity;
    private final CompoundTag nbt;

    public LivingEntityLoadAdditionalEvent(LivingEntity entity, CompoundTag nbt)
    {
        this.entity = entity;
        this.nbt = nbt;
    }

    public LivingEntity getEntity()
    {   return entity;
    }

    public CompoundTag getNBT()
    {   return nbt;
    }
}
