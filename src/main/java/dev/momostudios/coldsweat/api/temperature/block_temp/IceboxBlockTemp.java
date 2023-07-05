package dev.momostudios.coldsweat.api.temperature.block_temp;

import dev.momostudios.coldsweat.api.util.Temperature;
import dev.momostudios.coldsweat.common.block.IceboxBlock;
import dev.momostudios.coldsweat.util.registries.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import dev.momostudios.coldsweat.util.math.CSMath;

public class IceboxBlockTemp extends BlockTemp
{
    public IceboxBlockTemp()
    {
        super(ModBlocks.ICEBOX);
    }

    @Override
    public double getTemperature(Level level, LivingEntity entity, BlockState state, BlockPos pos, double distance)
    {
        if (state.getValue(IceboxBlock.FROSTED))
        {   return CSMath.blend(-0.27, 0, distance, 0.5, 5);
        }
        return 0;
    }

    @Override
    public double minEffect() {
        return CSMath.convertTemp(-40, Temperature.Units.F, Temperature.Units.MC, false);
    }

    @Override
    public double minTemperature() {
        return CSMath.convertTemp(32, Temperature.Units.F, Temperature.Units.MC, true);
    }
}
