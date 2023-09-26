package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.api.util.Temperature;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

public class FreezingTempModifier extends TempModifier
{
    public FreezingTempModifier(double chill)
    {
        this.getNBT().putDouble("chill", chill);
    }

    public FreezingTempModifier()
    {
        this(0);
    }

    @Override
    public Function<Double, Double> calculate(LivingEntity entity, Temperature.Type type)
    {
        return temp -> temp - this.getNBT().getDouble("chill");
    }

    @Override
    public String getID() {
        return "cold_sweat:freezing";
    }
}
