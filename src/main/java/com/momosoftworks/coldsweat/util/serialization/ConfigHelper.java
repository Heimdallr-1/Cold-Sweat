package com.momosoftworks.coldsweat.util.serialization;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.insulation.AdaptiveInsulation;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.insulation.StaticInsulation;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.type.Insulator;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import com.momosoftworks.coldsweat.data.codec.util.AttributeModifierMap;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.Property;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ConfigHelper
{
    private ConfigHelper() {}

    public static List<Block> getBlocks(String... ids)
    {
        List<Block> blocks = new ArrayList<>();
        for (String id : ids)
        {
            if (id.startsWith("#"))
            {
                final String tagID = id.replace("#", "");
                ITag<Block> blockTag = BlockTags.getAllTags().getTag(new ResourceLocation(tagID));
                if (blockTag != null)
                {   blocks.addAll(blockTag.getValues());
                }
            }
            else
            {
                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
                if (block != Blocks.AIR)
                    blocks.add(block);
            }
        }
        return blocks;
    }

    public static Map<Block, Number> getBlocksWithValues(List<? extends List<?>> source)
    {
        Map<Block, Number> map = new HashMap<>();
        for (List<?> entry : source)
        {
            String blockID = (String) entry.get(0);

            if (blockID.startsWith("#"))
            {
                final String tagID = blockID.replace("#", "");
                ITag<Block> blockTag = BlockTags.getAllTags().getTag(new ResourceLocation(tagID));
                if (blockTag != null)
                {
                    for (Block block : blockTag.getValues())
                    {   map.put(block, (Number) entry.get(1));
                    }
                }
            }
            else
            {   Block newBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockID));
                if (newBlock != null) map.put(newBlock, (Number) entry.get(1));
            }
        }
        return map;
    }

    public static List<Item> getItems(String... ids)
    {
        List<Item> items = new ArrayList<>();
        for (String itemId : ids)
        {
            if (itemId.startsWith("#"))
            {
                final String tagID = itemId.replace("#", "");
                ITag<Item> itemTag = ItemTags.getAllTags().getTag(new ResourceLocation(tagID));
                if (itemTag != null)
                {   items.addAll(itemTag.getValues());
                }
            }
            else
            {   Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
                if (item != null && item != Items.AIR)
                {   items.add(item);
                }
                else ColdSweat.LOGGER.error("Error parsing item config: item \"" + itemId + "\" does not exist");
            }
        }
        return items;
    }

    public static Map<ResourceLocation, Triplet<Double, Double, Temperature.Units>> getBiomesWithValues(List<? extends List<?>> source, boolean absolute)
    {
        Map<ResourceLocation, Triplet<Double, Double, Temperature.Units>> map = new HashMap<>();
        for (List<?> entry : source)
        {
            try
            {
                ResourceLocation biomeID = new ResourceLocation((String) entry.get(0));
                Biome biome = WorldHelper.getBiome(biomeID);

                double min;
                double max;
                Temperature.Units units;
                // The config defines a min and max value, with optional unit conversion
                if (entry.size() > 2)
                {   units = entry.size() == 4 ? Temperature.Units.valueOf(((String) entry.get(3)).toUpperCase()) : Temperature.Units.MC;
                    min = Temperature.convert(((Number) entry.get(1)).doubleValue(), units, Temperature.Units.MC, absolute);
                    max = Temperature.convert(((Number) entry.get(2)).doubleValue(), units, Temperature.Units.MC, absolute);
                }
                // The config only defines a mid-temperature
                else
                {   double mid = ((Number) entry.get(1)).doubleValue();
                    double variance = 1 / Math.max(1, 2 + biome.getDownfall() * 2);
                    min = mid - variance;
                    max = mid + variance;
                    units = Temperature.Units.MC;
                }

                // Maps the biome ID to the temperature (and variance if present)
                map.put(biomeID, new Triplet<>(min, max, units));
            }
            catch (Exception e)
            {   ColdSweat.LOGGER.error("Error parsing biome config: biome \"" + entry.toString() + "\" does not exist or is not loaded yet.");
            }
        }
        return map;
    }

    public static Map<ResourceLocation, Pair<Double, Temperature.Units>> getDimensionsWithValues(List<? extends List<?>> source, boolean absolute)
    {
        Map<ResourceLocation, Pair<Double, Temperature.Units>> map = new HashMap<>();
        for (List<?> entry : source)
        {
            try
            {
                ResourceLocation dimensionID = new ResourceLocation((String) entry.get(0));
                double temp = ((Number) entry.get(1)).doubleValue();
                Temperature.Units units = entry.size() == 3 ? Temperature.Units.valueOf(((String) entry.get(2)).toUpperCase()) : Temperature.Units.MC;
                map.put(dimensionID, Pair.of(Temperature.convert(temp, units, Temperature.Units.MC, absolute), units));
            }
            catch (Exception e)
            {   ColdSweat.LOGGER.error("Error parsing dimension config: dimension \"" + entry.toString() + "\" does not exist or is not loaded yet");
            }
        }
        return map;
    }

    public static Map<String, Predicate<BlockState>> getBlockStatePredicates(Block block, String predicates)
    {
        Map<String, Predicate<BlockState>> blockPredicates = new HashMap<>();
        // Separate comma-delineated predicates
        String[] predicateList = predicates.split(",");

        // Iterate predicates
        for (String predicate : predicateList)
        {
            // Split predicate into key-value pairs separated by "="
            String[] pair = predicate.split("=");
            String key = pair[0];
            String value = pair[1];

            // Get the property with the given name
            Property<?> property = block.getStateDefinition().getProperty(key);
            if (property != null)
            {
                // Parse the desired value for this property
                property.getValue(value).ifPresent(propertyValue ->
                {
                    // Add a new predicate to the list
                    blockPredicates.put(predicate, state ->
                    {   // If the value matches, this predicate returns true
                        return state.getValue(property).equals(propertyValue);
                    });
                });
            }
        }
        return blockPredicates;
    }

    public static List<Biome> getBiomes(List<? extends String> ids)
    {   return ids.stream().map(id -> WorldHelper.getBiome(new ResourceLocation(id))).collect(Collectors.toList());
    }

    public static CompoundNBT serializeNbtBool(boolean value, String key)
    {
        CompoundNBT tag = new CompoundNBT();
        tag.putBoolean(key, value);
        return tag;
    }

    public static CompoundNBT serializeNbtInt(int value, String key)
    {
        CompoundNBT tag = new CompoundNBT();
        tag.putInt(key, value);
        return tag;
    }

    public static CompoundNBT serializeNbtDouble(double value, String key)
    {
        CompoundNBT tag = new CompoundNBT();
        tag.putDouble(key, value);
        return tag;
    }

    public static CompoundNBT serializeNbtString(String value, String key)
    {
        CompoundNBT tag = new CompoundNBT();
        tag.putString(key, value);
        return tag;
    }

    public static CompoundNBT serializeDimensionTemps(Map<ResourceLocation, Pair<Double, Temperature.Units>> map, String key)
    {
        CompoundNBT tag = new CompoundNBT();
        CompoundNBT mapTag = new CompoundNBT();
        for (Map.Entry<ResourceLocation, Pair<Double, Temperature.Units>> entry : map.entrySet())
        {
            CompoundNBT biomeTag = new CompoundNBT();
            biomeTag.putDouble("Temp", entry.getValue().getFirst());
            biomeTag.putString("Units", entry.getValue().getSecond().toString());
            mapTag.put(entry.getKey().toString(), biomeTag);
        }
        tag.put(key, mapTag);
        return tag;
    }

    public static Map<ResourceLocation, Pair<Double, Temperature.Units>> deserializeDimensionTemps(CompoundNBT tag, String key)
    {
        Map<ResourceLocation, Pair<Double, Temperature.Units>> map = new HashMap<>();
        CompoundNBT mapTag = tag.getCompound(key);
        for (String biomeID : mapTag.getAllKeys())
        {
            CompoundNBT biomeTag = mapTag.getCompound(biomeID);
            map.put(new ResourceLocation(biomeID), Pair.of(biomeTag.getDouble("Temp"), Temperature.Units.valueOf(biomeTag.getString("Units"))));
        }
        return map;
    }

    public static CompoundNBT serializeBiomeTemps(Map<ResourceLocation, Triplet<Double, Double, Temperature.Units>> map, String key)
    {
        CompoundNBT tag = new CompoundNBT();
        CompoundNBT mapTag = new CompoundNBT();
        for (Map.Entry<ResourceLocation, Triplet<Double, Double, Temperature.Units>> entry : map.entrySet())
        {
            CompoundNBT biomeTag = new CompoundNBT();
            biomeTag.putDouble("Min", entry.getValue().getFirst());
            biomeTag.putDouble("Max", entry.getValue().getSecond());
            biomeTag.putString("Units", entry.getValue().getThird().toString());
            mapTag.put(entry.getKey().toString(), biomeTag);
        }
        tag.put(key, mapTag);
        return tag;
    }

    public static Map<ResourceLocation, Triplet<Double, Double, Temperature.Units>> deserializeBiomeTemps(CompoundNBT tag, String key)
    {
        Map<ResourceLocation, Triplet<Double, Double, Temperature.Units>> map = new HashMap<>();
        CompoundNBT mapTag = tag.getCompound(key);
        for (String biomeID : mapTag.getAllKeys())
        {
            CompoundNBT biomeTag = mapTag.getCompound(biomeID);
            map.put(new ResourceLocation(biomeID), new Triplet<>(biomeTag.getDouble("Min"), biomeTag.getDouble("Max"), Temperature.Units.valueOf(biomeTag.getString("Units"))));
        }
        return map;
    }

    public static <T> CompoundNBT serializeItemMap(Map<Item, T> map, String key, Function<T, CompoundNBT> serializer)
    {
        CompoundNBT tag = new CompoundNBT();
        CompoundNBT mapTag = new CompoundNBT();
        for (Map.Entry<Item, T> entry : map.entrySet())
        {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(entry.getKey());
            if (itemId == null) continue;
            mapTag.put(itemId.toString(), serializer.apply(entry.getValue()));
        }
        tag.put(key, mapTag);
        return tag;
    }

    public static <T> Map<Item, T> deserializeItemMap(CompoundNBT tag, String key, Function<CompoundNBT, T> deserializer)
    {
        Map<Item, T> map = new HashMap<>();
        CompoundNBT mapTag = tag.getCompound(key);
        for (String itemID : mapTag.getAllKeys())
        {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemID));
            map.put(item, deserializer.apply(mapTag.getCompound(itemID)));
        }
        return map;
    }

    public static <T> Map<Item, T> readItemMap(List<? extends List<?>> source, Function<List<?>, T> valueParser)
    {
        Map<Item, T> map = new HashMap<>();
        for (List<?> entry : source)
        {
            String itemId = (String) entry.get(0);
            for (Item item : getItems(itemId))
            {
                map.put(item, valueParser.apply(entry.subList(1, entry.size())));
            }
        }
        return map;
    }

    public static <T> void writeItemMap(Map<Item, T> map, Consumer<List<? extends List<?>>> saver, Function<T, List<?>> valueWriter)
    {
        List<List<?>> list = new ArrayList<>();
        for (Map.Entry<Item, T> entry : map.entrySet())
        {
            Item item = entry.getKey();
            T value = entry.getValue();
            List<Object> itemData = new ArrayList<>();
            itemData.add(ForgeRegistries.ITEMS.getKey(item).toString());
            itemData.addAll(valueWriter.apply(value));
            list.add(itemData);
        }
        saver.accept(list);
    }

    public static CompoundNBT serializeItemInsulations(Map<Item, Insulator> map, String key)
    {
        CompoundNBT tag = new CompoundNBT();
        CompoundNBT mapTag = new CompoundNBT();
        for (Map.Entry<Item, Insulator> entry : map.entrySet())
        {
            Item item = entry.getKey();
            Insulator insulator = entry.getValue();
            mapTag.put(ForgeRegistries.ITEMS.getKey(item).toString(), insulator.serialize());
        }
        tag.put(key, mapTag);

        return tag;
    }

    public static Map<Item, Insulator> deserializeItemInsulations(CompoundNBT tag, String key)
    {
        Map<Item, Insulator> map = new HashMap<>();
        CompoundNBT mapTag = tag.getCompound(key);
        for (String itemID : mapTag.getAllKeys())
        {
            CompoundNBT insulatorTag = mapTag.getCompound(itemID);
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemID));
            Insulator insulator = Insulator.deserialize(insulatorTag);
            map.put(item, insulator);
        }
        return map;
    }

    public static Map<Item, Insulator> readItemInsulations(List<? extends List<?>> items, Insulation.Slot slot)
    {
        return readItemMap(items, args ->
        {
            double value1 = ((Number) args.get(0)).doubleValue();
            double value2 = ((Number) args.get(1)).doubleValue();
            String type = args.size() > 2 ? (String) args.get(2) : "static";
            CompoundNBT nbt = args.size() > 3 ? NBTHelper.parseCompoundNbt(((String) args.get(3))) : new CompoundNBT();
            Insulation insulation = type.equals("static")
                                    ? new StaticInsulation(value1, value2)
                                    : new AdaptiveInsulation(value1, value2);
            ItemRequirement requirement = new ItemRequirement(Arrays.asList(), Optional.empty(),
                                                              Optional.empty(), Optional.empty(),
                                                              Optional.empty(), Optional.empty(), new NbtRequirement(nbt));
            return new Insulator(insulation, slot, requirement, EntityRequirement.NONE, new AttributeModifierMap());
        });
    }

    public static void writeItemInsulations(Map<Item, Insulator> items, Consumer<List<? extends List<?>>> saver)
    {
        writeItemMap(items, saver, insulator ->
        {
            if (insulator.predicate.equals(EntityRequirement.NONE))
            {   return Arrays.asList();
            }
            List<Object> itemData = new ArrayList<>();
            itemData.add(insulator.insulation instanceof StaticInsulation
                         ? insulator.insulation.getCold()
                         : ((AdaptiveInsulation) insulator.insulation).getInsulation());
            itemData.add(insulator.insulation instanceof StaticInsulation
                         ? insulator.insulation.getHeat()
                         : ((AdaptiveInsulation) insulator.insulation).getSpeed());
            itemData.add(insulator.insulation instanceof StaticInsulation
                         ? "static"
                         : "adaptive");
            itemData.add(insulator.data.nbt.serialize().toString());
            return itemData;
        });
    }
}
