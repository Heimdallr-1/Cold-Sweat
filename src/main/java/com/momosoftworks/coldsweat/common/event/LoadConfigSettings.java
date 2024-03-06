package com.momosoftworks.coldsweat.common.event;

import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.registry.BlockTempRegistry;
import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp;
import com.momosoftworks.coldsweat.api.util.InsulationType;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.config.util.ItemData;
import com.momosoftworks.coldsweat.data.ModRegistries;
import com.momosoftworks.coldsweat.data.configuration.BiomeTempData;
import com.momosoftworks.coldsweat.data.configuration.BlockTempData;
import com.momosoftworks.coldsweat.data.configuration.DimensionTempData;
import com.momosoftworks.coldsweat.data.configuration.InsulatorData;
import com.momosoftworks.coldsweat.util.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import oshi.util.tuples.Triplet;

import java.util.List;
import java.util.Optional;

import java.util.concurrent.atomic.AtomicBoolean;

@Mod.EventBusSubscriber
public class LoadConfigSettings
{
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event)
    {
        ConfigSettings.load();

        // Load JSON data-driven insulators
        RegistryAccess registries = event.getServer().registryAccess();
        registries.registryOrThrow(ModRegistries.INSULATOR_DATA)
        .holders()
        .forEach(holder ->
        {
            InsulatorData insulatorData = holder.get();
            com.momosoftworks.coldsweat.api.insulation.Insulation insulation = insulatorData.getInsulation();
            CompoundTag nbt = insulatorData.nbt().orElse(new CompoundTag());
            // If the item is defined, add it to the appropriate map
            insulatorData.item().ifPresent(itemOrList ->
            {
                // If the item is single, write the insulation value
                itemOrList.ifLeft(item -> addItemConfig(item, insulation, insulatorData.type(), nbt));
                // If the item is a list, write the insulation value for each item
                itemOrList.ifRight(list ->
                {
                    for (Item item : list)
                    {   addItemConfig(item, insulation, insulatorData.type(), nbt);
                    }
                });
            });
            // If the tag is defined, add all items in the tag to the appropriate map
            insulatorData.tag().ifPresent(tag ->
            {
                ForgeRegistries.ITEMS.tags().getTag(tag).stream().forEach(item ->
                {   addItemConfig(item, insulation, insulatorData.type(), nbt);
                });
            });
        });

        // Load JSON data-driven block temperatures
        registries.registryOrThrow(ModRegistries.BLOCK_TEMP_DATA)
        .holders()
        .forEach(holder ->
        {
            BlockTempData blockTempData = holder.get();
            BlockTemp blockTemp = new BlockTemp(blockTempData.blocks().toArray(new Block[0]))
            {
                final double temperature = blockTempData.temperature();
                final double maxEffect = blockTempData.maxEffect();
                final boolean fade = blockTempData.fade();
                final List<BlockPredicate> conditions = blockTempData.conditions();
                final Optional<CompoundTag> tag = blockTempData.tag();

                @Override
                public double getTemperature(Level level, LivingEntity entity, BlockState state, BlockPos pos, double distance)
                {
                    if (level instanceof ServerLevel serverLevel)
                    {
                        for (int i = 0; i < conditions.size(); i++)
                        {
                            if (!conditions.get(i).test(serverLevel, pos))
                            {   return 0;
                            }
                        }
                    }
                    if (tag.isPresent())
                    {
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity != null)
                        {
                            CompoundTag blockTag = blockEntity.saveWithFullMetadata();
                            for (String key : tag.get().getAllKeys())
                            {
                                if (!tag.get().get(key).equals(blockTag.get(key)))
                                {   return 0;
                                }
                            }
                        }
                    }
                    return fade
                         ? CSMath.blend(temperature, 0, distance, 0, ConfigSettings.BLOCK_RANGE.get())
                         : temperature;
                }

                @Override
                public double maxEffect()
                {   return temperature > 0 ? maxEffect : super.maxEffect();
                }

                @Override
                public double minEffect()
                {   return temperature < 0 ? -maxEffect : super.minEffect();
                }
            };
            BlockTempRegistry.register(blockTemp);
        });

        // Load JSON data-driven dimension temperatures
        registries.registryOrThrow(ModRegistries.BIOME_TEMP_DATA)
        .holders()
        .forEach(holder ->
        {
            // If the dimension is a tag, add the temperature to all biomes in the tag
            BiomeTempData biomeTempData = holder.get();
            biomeTempData.biome().ifLeft(tag ->
            {
                ForgeRegistries.BIOMES.tags().getTag(tag).stream().map(ForgeRegistries.BIOMES::getKey).forEach(biome ->
                {    addBiomeTempConfig(biome, biomeTempData);
                });
            });
            // If the dimension is a single dimension, add the temperature to the dimension
            biomeTempData.biome().ifRight(location ->
            {   addBiomeTempConfig(location, biomeTempData);
            });
        });

        // Load JSON data-driven dimension temperatures
        registries.registryOrThrow(ModRegistries.DIMENSION_TEMP_DATA)
        .holders()
        .forEach(holder ->
        {
            // If the dimension is a tag, add the temperature to all biomes in the tag
            DimensionTempData dimensionTempData = holder.get();
            dimensionTempData.dimension().ifLeft(tag ->
            {
                Registry<DimensionType> dimensions = registries.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);

                dimensions.getTag(tag).orElseThrow().stream().map(dim -> dimensions.getKey(dim.get())).forEach(location ->
                {   addDimensionTempConfig(location, dimensionTempData);
                });
            });
            // If the dimension is a single dimension, add the temperature to the dimension
            dimensionTempData.dimension().ifRight(location ->
            {   addDimensionTempConfig(location, dimensionTempData);
            });
        });
    }

    private static void addItemConfig(Item item, Insulation insulation, InsulationType type, CompoundTag nbt)
    {
        switch (type)
        {
            case ITEM -> ConfigSettings.INSULATION_ITEMS.get().put(new ItemData(item, nbt), insulation);
            case ARMOR -> ConfigSettings.INSULATING_ARMORS.get().put(new ItemData(item, nbt), insulation);
            case CURIO ->
            {
                if (CompatManager.isCuriosLoaded())
                {   ConfigSettings.INSULATING_CURIOS.get().put(new ItemData(item, nbt), insulation);
                }
            }
        }
    }

    private static void addBiomeTempConfig(ResourceLocation biome, BiomeTempData biomeTempData)
    {
        Temperature.Units units = biomeTempData.units();
        if (biomeTempData.isOffset())
        {   ConfigSettings.BIOME_OFFSETS.get().put(biome, new Triplet<>(Temperature.convertUnits(biomeTempData.min(), units, Temperature.Units.MC, true),
                                                                      Temperature.convertUnits(biomeTempData.max(), units, Temperature.Units.MC, true),
                                                                      biomeTempData.units()));
        }
        else
        {
            ConfigSettings.BIOME_TEMPS.get().put(biome, new Triplet<>(Temperature.convertUnits(biomeTempData.min(), units, Temperature.Units.MC, true),
                                                                      Temperature.convertUnits(biomeTempData.max(), units, Temperature.Units.MC, true),
                                                                      biomeTempData.units()));
        }
    }

    private static void addDimensionTempConfig(ResourceLocation dimension, DimensionTempData dimensionTempData)
    {
        Temperature.Units units = dimensionTempData.units();
        if (dimensionTempData.isOffset())
        {   ConfigSettings.DIMENSION_OFFSETS.get().put(dimension, Pair.of(Temperature.convertUnits(dimensionTempData.temperature(), units, Temperature.Units.MC, true),
                                                                         dimensionTempData.units()));
        }
        else
        {   ConfigSettings.DIMENSION_TEMPS.get().put(dimension, Pair.of(Temperature.convertUnits(dimensionTempData.temperature(), units, Temperature.Units.MC, true),
                                                                        dimensionTempData.units()));
        }
    }
}
