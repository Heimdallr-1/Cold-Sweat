package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.api.util.Temperature;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

public class SoulSproutTempModifier extends TempModifier
{

    @Override
    protected Function<Double, Double> calculate(LivingEntity entity, Temperature.Type type)
    {
        if (Math.random() < 0.3 && entity.tickCount % 5 == 0)
        {
            WorldHelper.spawnParticleBatch(entity.level, ParticleTypes.SOUL, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                    entity.getBbWidth() / 2, entity.getBbHeight() / 2, entity.getBbWidth() / 2, 1, 0.02);
        }
        return temp -> temp - 20;
    }

    @Override
    public String getID()
    {
        return "cold_sweat:soul_sprout";
    }
}
