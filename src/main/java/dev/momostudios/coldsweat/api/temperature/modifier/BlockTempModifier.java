package dev.momostudios.coldsweat.api.temperature.modifier;

import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.api.registry.BlockTempRegistry;
import dev.momostudios.coldsweat.api.temperature.block_temp.BlockTemp;
import dev.momostudios.coldsweat.api.util.Temperature;
import dev.momostudios.coldsweat.config.WorldSettingsConfig;
import dev.momostudios.coldsweat.core.advancement.trigger.ModAdvancementTriggers;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class BlockTempModifier extends TempModifier
{
    public BlockTempModifier() {}

    public BlockTempModifier(int range)
    {   this.getNBT().putInt("RangeOverride", range);
    }

    @Override
    public Function<Double, Double> calculate(LivingEntity entity, Temperature.Type type)
    {
        Map<BlockTemp, Double> affectMap = new HashMap<>();
        Map<BlockPos, Pair<BlockTemp, Double>> triggers = new HashMap<>();
        Map<ChunkPos, ChunkAccess> chunks = new HashMap<>();

        Level level = entity.level;
        int range = this.getNBT().contains("RangeOverride", 3) ? this.getNBT().getInt("RangeOverride") : WorldSettingsConfig.getInstance().getBlockRange();

        int entX = entity.blockPosition().getX();
        int entY = entity.blockPosition().getY();
        int entZ = entity.blockPosition().getZ();
        BlockPos.MutableBlockPos blockpos = new BlockPos.MutableBlockPos();
        int minBuildHeight = level.getMinBuildHeight();

        for (int x = -range; x < range; x++)
        {
            for (int z = -range; z < range; z++)
            {
                ChunkPos chunkPos = new ChunkPos((entX + x) >> 4, (entZ + z) >> 4);
                ChunkAccess chunk = chunks.get(chunkPos);
                if (chunk == null) chunks.put(chunkPos, chunk = WorldHelper.getChunk(level, chunkPos));
                if (chunk == null) continue;
                LevelChunkSection[] sections = chunk.getSections();

                for (int y = -range; y < range; y++)
                {
                    try
                    {
                        blockpos.set(entX + x, entY + y, entZ + z);

                        LevelChunkSection section = sections[CSMath.clamp((blockpos.getY() >> 4) - (minBuildHeight >> 4), 0, sections.length - 1)];
                        BlockState state = section.getBlockState(blockpos.getX() & 15, blockpos.getY() & 15, blockpos.getZ() & 15);

                        if (state.getMaterial() == Material.AIR) continue;

                        // Get the BlockTemp associated with the block
                        List<BlockTemp> blockTemps = BlockTempRegistry.getBlockTempsFor(state);

                        if (blockTemps.isEmpty() || (blockTemps.size() == 1 && blockTemps.contains(BlockTempRegistry.DEFAULT_BLOCK_TEMP))) continue;

                        // Get the amount that this block has affected the player so far

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
                            Vec3 pos = Vec3.atCenterOf(blockpos);

                            // Gets the closest point in the player's BB to the block
                            double playerRadius = entity.getBbWidth() / 2;
                            Vec3 playerClosest = new Vec3(CSMath.clamp(pos.x, entity.getX() - playerRadius, entity.getX() + playerRadius),
                                                          CSMath.clamp(pos.y, entity.getY(), entity.getY() + entity.getBbHeight()),
                                                          CSMath.clamp(pos.z, entity.getZ() - playerRadius, entity.getZ() + playerRadius));

                            // Cast a ray between the player and the block
                            // Lessen the effect with each block between the player and the block
                            int[] blocks = new int[1];
                            Vec3 ray = pos.subtract(playerClosest);
                            Direction direction = Direction.getNearest(ray.x, ray.y, ray.z);

                            WorldHelper.forBlocksInRay(playerClosest, pos, level, chunk,
                            (rayChunk, rayState, bpos) ->
                            {   if (!bpos.equals(blockpos) && WorldHelper.isSpreadBlocked(level, rayState, bpos, direction, direction))
                                {   blocks[0]++;
                                }
                            }, 3);

                            // Get the temperature of the block given the player's distance
                            double distance = CSMath.getDistance(playerClosest, pos);

                            for (int i = 0; i < blockTemps.size(); i++)
                            {
                                BlockTemp blockTemp = blockTemps.get(i);
                                double tempToAdd = blockTemp.getTemperature(level, entity, state, blockpos, distance);

                                // Store this block type's total effect on the player
                                // Dampen the effect with each block between the player and the block
                                double blockTempTotal = affectMap.getOrDefault(blockTemp, 0d) + tempToAdd / (blocks[0] + 1);
                                affectMap.put(blockTemp, CSMath.clamp(blockTempTotal, blockTemp.minEffect(), blockTemp.maxEffect()));
                                // Used to trigger advancements
                                triggers.put(blockpos, Pair.of(blockTemp, distance));
                            }
                        }
                    }
                    catch (Exception ignored) {}
                }
            }
        }
        // Trigger advancements at every BlockPos with a BlockEffect attached to it
        if (entity instanceof ServerPlayer player)
        {
            for (Map.Entry<BlockPos, Pair<BlockTemp, Double>> trigger : triggers.entrySet())
            {   Pair<BlockTemp, Double> entry = trigger.getValue();
                ModAdvancementTriggers.BLOCK_AFFECTS_TEMP.trigger(player, trigger.getKey(), entry.getSecond(), affectMap.get(entry.getFirst()));
            }
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
        return "cold_sweat:blocks";
    }
}