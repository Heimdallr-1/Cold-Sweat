package dev.momostudios.coldsweat.common.capability;

import dev.momostudios.coldsweat.common.temperature.Temperature;
import dev.momostudios.coldsweat.common.temperature.modifier.TempModifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.List;

public interface ITemperatureCap
{
    double get(Temperature.Types type);
    void set(Temperature.Types type, double value);
    List<TempModifier> getModifiers(Temperature.Types type);
    boolean hasModifier(Temperature.Types type, Class<? extends TempModifier> mod);
    void clearModifiers(Temperature.Types type);
    void copy(ITemperatureCap cap);
    void tickUpdate(Player player);
    void tickClient(Player player);

    CompoundTag serializeNBT();
    void deserializeNBT(CompoundTag tag);
}
