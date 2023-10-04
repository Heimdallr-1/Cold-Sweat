package com.momosoftworks.coldsweat.api.temperature.modifier;

import com.momosoftworks.coldsweat.api.registry.BlockTempRegistry;
import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.Direction;
import com.momosoftworks.coldsweat.util.math.Triplet;
import com.momosoftworks.coldsweat.util.world.BlockPos;
import com.momosoftworks.coldsweat.util.world.BlockState;
import com.momosoftworks.coldsweat.util.world.ChunkPos;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class BlockTempModifier extends TempModifier
{
    public BlockTempModifier() {}

    public BlockTempModifier(int range)
    {   this.getNBT().setInteger("RangeOverride", range);
    }

    Map<ChunkPos, Chunk> chunks = new HashMap<>(16);

    @Override
    public Function<Double, Double> calculate(EntityLivingBase entity, Temperature.Type type)
    {
        Map<BlockTemp, Double> affectMap = new HashMap<>(128);
        Map<BlockPos, BlockState> stateCache = new HashMap<>(4096);
        List<Triplet<BlockPos, BlockTemp, Double>> triggers = new ArrayList<>(128);

        World world = entity.worldObj;
        int range = this.getNBT().hasKey("RangeOverride", 3) ? this.getNBT().getInteger("RangeOverride") : ConfigSettings.BLOCK_RANGE.get();

        BlockPos entPos = new BlockPos(entity.getPosition(0));
        int entX = entPos.getX();
        int entY = entPos.getY();
        int entZ = entPos.getZ();
        BlockPos.Mutable blockpos = new BlockPos.Mutable();

        for (int x = -range; x < range; x++)
        {
            for (int z = -range; z < range; z++)
            {
                ChunkPos chunkPos = new ChunkPos((entX + x) >> 4, (entZ + z) >> 4);
                Chunk chunk = chunks.get(chunkPos);
                if (chunk == null) chunks.put(chunkPos, chunk = WorldHelper.getChunk(world, chunkPos));
                if (chunk == null) continue;
                ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();

                for (int y = -range; y < range; y++)
                {
                    try
                    {
                        blockpos.set(entX + x, entY + y, entZ + z);

                        BlockState state = stateCache.get(blockpos);
                        if (state == null)
                        {   stateCache.put(blockpos, state = BlockState.of(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z)));
                        }
                        Block block = state.getBlock();

                        if (block.getMaterial() == Material.air) continue;

                        // Get the BlockTemp associated with the block
                        List<BlockTemp> blockTemps = BlockTempRegistry.getBlockTempsFor(state);

                        if (blockTemps.isEmpty() || (blockTemps.size() == 1 && blockTemps.contains(BlockTempRegistry.DEFAULT_BLOCK_TEMP))) continue;

                        // Is totalTemp within the bounds of any BlockTemp's min/max range?
                        boolean isInTempRange = affectMap.isEmpty();
                        if (!isInTempRange)
                        {   for (Map.Entry<BlockTemp, Double> entry : affectMap.entrySet())
                        {   BlockTemp key = entry.getKey();
                            Double value = entry.getValue();

                            if (!blockTemps.contains(key) || CSMath.withinRange(value, key.minEffect(), key.maxEffect()))
                            {   isInTempRange = true;
                                break;
                            }
                        }
                        }
                        if (isInTempRange)
                        {
                            // Get Vector positions of the centers of the source block and player
                            Vec3 pos = CSMath.atCenterOf(blockpos);

                            // Gets the closest point in the player's BB to the block
                            double playerRadius = entity.width / 2;
                            Vec3 playerClosest = Vec3.createVectorHelper(CSMath.clamp(pos.xCoord, entity.posX - playerRadius, entity.posX + playerRadius),
                                                                  CSMath.clamp(pos.yCoord, entity.posY, entity.posY + entity.height),
                                                                  CSMath.clamp(pos.zCoord, entity.posZ - playerRadius, entity.posZ + playerRadius));

                            // Cast a ray between the player and the block
                            // Lessen the effect with each block between the player and the block
                            int[] blocks = new int[1];
                            Vec3 ray = pos.subtract(playerClosest);
                            Direction direction = Direction.getNearest(ray.xCoord, ray.yCoord, ray.zCoord);

                            WorldHelper.forBlocksInRay(playerClosest, pos, world, chunk, stateCache,
                            (rayState, bpos) ->
                            {   if (!bpos.equals(blockpos) && WorldHelper.isSpreadBlocked(world, rayState, bpos, direction, direction))
                                {   blocks[0]++;
                                }
                            }, 3);

                            // Get the temperature of the block given the player's distance
                            double distance = CSMath.getDistance(playerClosest, pos);

                            for (int i = 0; i < blockTemps.size(); i++)
                            {
                                BlockTemp blockTemp = blockTemps.get(i);
                                double tempToAdd = blockTemp.getTemperature(world, entity, state, blockpos, distance);

                                // Store this block type's total effect on the player
                                // Dampen the effect with each block between the player and the block
                                double blockTempTotal = affectMap.getOrDefault(blockTemp, 0d) + tempToAdd / (blocks[0] + 1);
                                affectMap.put(blockTemp, CSMath.clamp(blockTempTotal, blockTemp.minEffect(), blockTemp.maxEffect()));
                                // TODO: 9/24/23 Add this back if achievements are added
                                // Used to trigger advancements
                                /*if (shouldTickAdvancements)
                                {   triggers.add(new Triplet<>(blockpos, blockTemp, distance));
                                }*/
                            }
                        }
                    }
                    catch (Exception ignored) {}
                }
            }
        }


        // TODO: 9/24/23 Add this back if achievements are added
        // Trigger advancements at every BlockPos with a BlockEffect attached to it
        /*if (entity instanceof ServerPlayerEntity && shouldTickAdvancements)
        {
            for (Triplet<BlockPos, BlockTemp, Double> trigger : triggers)
            {   ModAdvancementTriggers.BLOCK_AFFECTS_TEMP.trigger(((ServerPlayerEntity) entity), trigger.getFirst(), trigger.getThird(), affectMap.get(trigger.getSecond()));
            }
        }*/

        // Remove old chunks from the cache
        while (chunks.size() >= 16)
        {   chunks.remove(chunks.keySet().iterator().next());
        }

        // Add the effects of all the blocks together and return the result
        return temp ->
        {
            for (Map.Entry<BlockTemp, Double> effect : affectMap.entrySet())
            {
                BlockTemp be = effect.getKey();
                double min = be.minTemperature();
                double max = be.maxTemperature();
                if (!CSMath.withinRange(temp, min, max)) continue;
                temp = CSMath.clamp(temp + effect.getValue(), min, max);
            }
            return temp;
        };
    }

    public String getID()
    {
        return "cold_sweat:nearby_blocks";
    }
}