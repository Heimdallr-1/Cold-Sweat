package com.momosoftworks.coldsweat.api.temperature.block_temp;


import com.momosoftworks.coldsweat.util.world.BlockPos;
import com.momosoftworks.coldsweat.util.world.BlockState;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class BlockTemp
{
    private final HashSet<Block> validBlocks;

    /**
     * @param state is the {@link BlockState} of the block
     * @param pos is the position of the block
     * @param distance is the distance between the player and the block
     * @return the temperature of the block. This is ADDED to the world temperature.
     */
    public abstract double getTemperature(World world, EntityLivingBase entity, BlockState state, BlockPos pos, double distance);

    public BlockTemp(Block... blocks)
    {   validBlocks = new HashSet<>(Arrays.asList(blocks));
    }

    public boolean hasBlock(Block block)
    {   return validBlocks.contains(block);
    }

    public Set<Block> getAffectedBlocks()
    {   return validBlocks;
    }

    /**
     * The maximum temperature this block can emit, no matter how many there are near the player <br>
     * @return a double representing the temperature, in Minecraft units
     */
    public double maxEffect() {
        return Double.MAX_VALUE;
    }

    /**
     * The minimum temperature this block can emit, no matter how many there are near the player <br>
     * (Useful for blocks with negative temperature) <br>
     * @return a double representing the temperature, in Minecraft units
     */
    public double minEffect() {
        return -Double.MAX_VALUE;
    }

    /**
     * The maximum world temperature for this BlockTemp to be effective<br>
     * @return a double representing the temperature, in Minecraft units
     */
    public double maxTemperature() {
        return Double.MAX_VALUE;
    }

    /**
     * The minimum world temperature for this BlockTemp to be effective<br>
     * @return a double representing the temperature, in Minecraft units
     */
    public double minTemperature() {
        return -Double.MAX_VALUE;
    }
}
