package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.api.util.Temperature;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

public class InsulationTempModifier extends TempModifier
{
    public InsulationTempModifier()
    {
        this(0d, 0d);
    }

    public InsulationTempModifier(double cold, double hot)
    {
        this.getNBT().putDouble("cold", cold);
        this.getNBT().putDouble("hot", hot);
    }

    @Override
    public Function<Double, Double> calculate(LivingEntity entity, Temperature.Type type)
    {
        double cold = this.getNBT().getDouble("cold");
        double hot = this.getNBT().getDouble("hot");
        return temp ->
        {
            double insulation = temp > 0 ? hot : cold;
            return temp * (insulation >= 0 ? Math.pow(0.1, insulation / 60) : -(insulation / 20) + 1);
        };
    }

    public String getID()
    {
        return "cold_sweat:armor";
    }
}