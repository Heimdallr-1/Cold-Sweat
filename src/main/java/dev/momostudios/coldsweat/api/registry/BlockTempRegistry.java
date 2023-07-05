package dev.momostudios.coldsweat.api.registry;

import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.temperature.block_temp.BlockTemp;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.LinkedList;

public class BlockTempRegistry
{
    public static final LinkedList<BlockTemp> BLOCK_TEMPS = new LinkedList<>();
    public static final HashMap<Block, BlockTemp> MAPPED_BLOCKS = new HashMap<>();
    public static final BlockTemp DEFAULT_BLOCK_EFFECT = new BlockTemp() {
        @Override
        public double getTemperature(Level level, LivingEntity entity, BlockState state, BlockPos pos, double distance)
        {   return 0;
        }
    };

    public static void register(BlockTemp blockTemp)
    {
        blockTemp.validBlocks.forEach(block ->
        {
            if (MAPPED_BLOCKS.put(block, blockTemp) != null)
            {   ColdSweat.LOGGER.error("Block \"{}\" already has a registered BlockTemp! The previous one will be overwritten.",
                                       ForgeRegistries.BLOCKS.getKey(block).toString());
            }
        });
        BLOCK_TEMPS.add(blockTemp);
    }

    public static void flush()
    {
        MAPPED_BLOCKS.clear();
    }

    public static BlockTemp getEntryFor(BlockState blockstate)
    {
        if (blockstate.isAir()) return DEFAULT_BLOCK_EFFECT;

        Block block = blockstate.getBlock();
        BlockTemp blockTemp = MAPPED_BLOCKS.get(block);
        if (blockTemp == null)
        {   blockTemp = BLOCK_TEMPS.stream().filter(bt -> bt.hasBlock(block)).findFirst().orElse(DEFAULT_BLOCK_EFFECT);
            MAPPED_BLOCKS.put(block, blockTemp);
            return blockTemp;
        }
        return blockTemp;
    }
}
