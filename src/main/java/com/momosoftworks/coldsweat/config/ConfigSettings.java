package com.momosoftworks.coldsweat.config;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Either;

import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.insulation.slot.ScalingFormula;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.spec.*;
import com.momosoftworks.coldsweat.config.type.CarriedItemTemperature;
import com.momosoftworks.coldsweat.config.type.InsulatingMount;
import com.momosoftworks.coldsweat.config.type.Insulator;
import com.momosoftworks.coldsweat.config.type.PredicateItem;
import com.momosoftworks.coldsweat.data.codec.configuration.DepthTempData;
import com.momosoftworks.coldsweat.data.codec.requirement.NbtRequirement;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.math.FastMap;
import com.momosoftworks.coldsweat.util.serialization.*;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.requirement.ItemRequirement;
import com.momosoftworks.coldsweat.data.codec.configuration.SpawnBiomeData;
import com.momosoftworks.coldsweat.util.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.Vec2i;
import com.momosoftworks.coldsweat.util.registries.ModEntities;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import oshi.util.tuples.Triplet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * Holds almost all configs for Cold Sweat in memory for easy access.
 * Handles syncing configs between the client/server.
 */
public class ConfigSettings
{
    public static final BiMap<String, DynamicHolder<?>> CONFIG_SETTINGS = HashBiMap.create();

    public static Difficulty DEFAULT_DIFFICULTY = Difficulty.NORMAL;

    // Settings visible in the config screen
    public static final DynamicHolder<Integer> DIFFICULTY;
    public static final DynamicHolder<Double> MAX_TEMP;
    public static final DynamicHolder<Double> MIN_TEMP;
    public static final DynamicHolder<Double> TEMP_RATE;
    public static final DynamicHolder<Double> TEMP_DAMAGE;
    public static final DynamicHolder<Boolean> FIRE_RESISTANCE_ENABLED;
    public static final DynamicHolder<Boolean> ICE_RESISTANCE_ENABLED;
    public static final DynamicHolder<Boolean> USE_PEACEFUL_MODE;
    public static final DynamicHolder<Boolean> REQUIRE_THERMOMETER;
    public static final DynamicHolder<Integer> GRACE_LENGTH;
    public static final DynamicHolder<Boolean> GRACE_ENABLED;

    // Other Difficulty Settings
    public static final DynamicHolder<Double> HEARTS_FREEZING_PERCENTAGE;
    public static final DynamicHolder<Double> COLD_MINING_IMPAIRMENT;
    public static final DynamicHolder<Double> COLD_MOVEMENT_SLOWDOWN;
    public static final DynamicHolder<Double> COLD_KNOCKBACK_REDUCTION;
    public static final DynamicHolder<Double> HEATSTROKE_FOG_DISTANCE;

    // World Settings
    public static final DynamicHolder<Map<Biome, Triplet<Double, Double, Temperature.Units>>> BIOME_TEMPS;
    public static final DynamicHolder<Map<Biome, Triplet<Double, Double, Temperature.Units>>> BIOME_OFFSETS;
    public static final DynamicHolder<Map<DimensionType, Pair<Double, Temperature.Units>>> DIMENSION_TEMPS;
    public static final DynamicHolder<Map<DimensionType, Pair<Double, Temperature.Units>>> DIMENSION_OFFSETS;
    public static final DynamicHolder<Map<StructureType<?>, Pair<Double, Temperature.Units>>> STRUCTURE_TEMPS;
    public static final DynamicHolder<Map<StructureType<?>, Pair<Double, Temperature.Units>>> STRUCTURE_OFFSETS;
    public static final DynamicHolder<Double> CAVE_INSULATION;
    public static final DynamicHolder<List<DepthTempData>> DEPTH_REGIONS;
    public static final DynamicHolder<Boolean> CHECK_SLEEP_CONDITIONS;
    public static final DynamicHolder<Double[]> SUMMER_TEMPS;
    public static final DynamicHolder<Double[]> AUTUMN_TEMPS;
    public static final DynamicHolder<Double[]> WINTER_TEMPS;
    public static final DynamicHolder<Double[]> SPRING_TEMPS;

    // Block settings
    public static final DynamicHolder<Integer> BLOCK_RANGE;
    public static final DynamicHolder<Boolean> COLD_SOUL_FIRE;
    public static final DynamicHolder<List<Block>> HEARTH_SPREAD_WHITELIST;
    public static final DynamicHolder<List<Block>> HEARTH_SPREAD_BLACKLIST;
    public static final DynamicHolder<Double> HEARTH_STRENGTH;
    public static final DynamicHolder<Boolean> SMART_HEARTH;
    public static final DynamicHolder<List<Block>> SLEEP_CHECK_IGNORE_BLOCKS;

    // Item settings
    public static final DynamicHolder<Map<Item, Insulator>> INSULATION_ITEMS;
    public static final DynamicHolder<Map<Item, Insulator>> INSULATING_ARMORS;
    public static final DynamicHolder<Map<Item, Insulator>> INSULATING_CURIOS;
    public static final DynamicHolder<ScalingFormula> INSULATION_SLOTS;
    public static final DynamicHolder<Double> INSULATION_STRENGTH;
    public static final DynamicHolder<List<Item>> INSULATION_BLACKLIST;

    public static final DynamicHolder<Map<Item, PredicateItem>> FOOD_TEMPERATURES;

    public static final DynamicHolder<Map<Item, CarriedItemTemperature>> CARRIED_ITEM_TEMPERATURES;

    public static final DynamicHolder<Integer> WATERSKIN_STRENGTH;

    public static final DynamicHolder<List<DimensionType>> LAMP_DIMENSIONS;

    public static final DynamicHolder<Map<Item, PredicateItem>> BOILER_FUEL;
    public static final DynamicHolder<Map<Item, PredicateItem>> ICEBOX_FUEL;
    public static final DynamicHolder<Map<Item, PredicateItem>> HEARTH_FUEL;
    public static final DynamicHolder<Map<Item, PredicateItem>> SOULSPRING_LAMP_FUEL;

    public static final DynamicHolder<Boolean> HEARTH_POTIONS_ENABLED;
    public static final DynamicHolder<List<MobEffect>> HEARTH_POTION_BLACKLIST;

    // Entity Settings
    public static final DynamicHolder<Triplet<Integer, Integer, Double>> FUR_TIMINGS;
    public static final DynamicHolder<Triplet<Integer, Integer, Double>> SHED_TIMINGS;
    public static final DynamicHolder<Multimap<Biome, SpawnBiomeData>> ENTITY_SPAWN_BIOMES;
    public static final DynamicHolder<Map<EntityType<?>, InsulatingMount>> INSULATED_ENTITIES;

    // Client Settings
    /* NULL ON THE SERVER */
    public static final DynamicHolder<Boolean> CELSIUS;
    public static final DynamicHolder<Integer> TEMP_OFFSET;
    public static final DynamicHolder<Double> TEMP_SMOOTHING;

    public static final DynamicHolder<Vec2i> BODY_ICON_POS;
    public static final DynamicHolder<Boolean> BODY_ICON_ENABLED;
    public static final DynamicHolder<Boolean> MOVE_BODY_ICON_WHEN_ADVANCED;

    public static final DynamicHolder<Vec2i> BODY_READOUT_POS;
    public static final DynamicHolder<Boolean> BODY_READOUT_ENABLED;

    public static final DynamicHolder<Vec2i> WORLD_GAUGE_POS;
    public static final DynamicHolder<Boolean> WORLD_GAUGE_ENABLED;

    public static final DynamicHolder<Boolean> CUSTOM_HOTBAR_LAYOUT;
    public static final DynamicHolder<Boolean> ICON_BOBBING;

    public static final DynamicHolder<Boolean> HEARTH_DEBUG;

    public static final DynamicHolder<Boolean> SHOW_CONFIG_BUTTON;
    public static final DynamicHolder<Vec2i> CONFIG_BUTTON_POS;

    public static final DynamicHolder<Boolean> DISTORTION_EFFECTS;

    public static final DynamicHolder<Boolean> HIGH_CONTRAST;

    public static final DynamicHolder<Boolean> SHOW_CREATIVE_WARNING;
    public static final DynamicHolder<Boolean> HIDE_TOOLTIPS;


    // Makes the settings instantiation collapsible & easier to read
    static
    {
        DIFFICULTY = addSyncedSetting("difficulty", () -> MainSettingsConfig.getInstance().getDifficulty(),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "Difficulty"),
        (decoder) -> decoder.getInt("Difficulty"),
        (saver) -> MainSettingsConfig.getInstance().setDifficulty(saver));

        MAX_TEMP = addSyncedSetting("max_temp", () -> MainSettingsConfig.getInstance().getMaxTempHabitable(),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "MaxTemp"),
        (decoder) -> decoder.getDouble("MaxTemp"),
        (saver) -> MainSettingsConfig.getInstance().setMaxHabitable(saver));

        MIN_TEMP = addSyncedSetting("min_temp", () -> MainSettingsConfig.getInstance().getMinTempHabitable(),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "MinTemp"),
        (decoder) -> decoder.getDouble("MinTemp"),
        (saver) -> MainSettingsConfig.getInstance().setMinHabitable(saver));

        TEMP_RATE = addSyncedSetting("temp_rate", () -> MainSettingsConfig.getInstance().getRateMultiplier(),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "TempRate"),
        (decoder) -> decoder.getDouble("TempRate"),
        (saver) -> MainSettingsConfig.getInstance().setRateMultiplier(saver));

        TEMP_DAMAGE = addSyncedSetting("temp_damage", () -> MainSettingsConfig.getInstance().getTempDamage(),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "TempDamage"),
        (decoder) -> decoder.getDouble("TempDamage"),
        (saver) -> MainSettingsConfig.getInstance().setTempDamage(saver));

        FIRE_RESISTANCE_ENABLED = addSyncedSetting("fire_resistance_enabled", () -> MainSettingsConfig.getInstance().isFireResistanceEnabled(),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "FireResistanceEnabled"),
        (decoder) -> decoder.getBoolean("FireResistanceEnabled"),
        (saver) -> MainSettingsConfig.getInstance().setFireResistanceEnabled(saver));

        ICE_RESISTANCE_ENABLED = addSyncedSetting("ice_resistance_enabled", () -> MainSettingsConfig.getInstance().isIceResistanceEnabled(),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "IceResistanceEnabled"),
        (decoder) -> decoder.getBoolean("IceResistanceEnabled"),
        (saver) -> MainSettingsConfig.getInstance().setIceResistanceEnabled(saver));

        USE_PEACEFUL_MODE = addSyncedSetting("use_peaceful", () -> MainSettingsConfig.getInstance().doDamageScaling(),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "UsePeaceful"),
        (decoder) -> decoder.getBoolean("UsePeaceful"),
        (saver) -> MainSettingsConfig.getInstance().setDamageScaling(saver));

        REQUIRE_THERMOMETER = addSyncedSetting("require_thermometer", () -> MainSettingsConfig.getInstance().thermometerRequired(),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "RequireThermometer"),
        (decoder) -> decoder.getBoolean("RequireThermometer"),
        (saver) -> MainSettingsConfig.getInstance().setRequireThermometer(saver));

        GRACE_LENGTH = addSyncedSetting("grace_length", () -> MainSettingsConfig.getInstance().getGracePeriodLength(),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "GraceLength"),
        (decoder) -> decoder.getInt("GraceLength"),
        (saver) -> MainSettingsConfig.getInstance().setGracePeriodLength(saver));

        GRACE_ENABLED = addSyncedSetting("grace_enabled", () -> MainSettingsConfig.getInstance().isGracePeriodEnabled(),
        (encoder) -> ConfigHelper.serializeNbtBool(encoder, "GraceEnabled"),
        (decoder) -> decoder.getBoolean("GraceEnabled"),
        (saver) -> MainSettingsConfig.getInstance().setGracePeriodEnabled(saver));


        HEARTS_FREEZING_PERCENTAGE = addSyncedSetting("hearts_freezing_percentage", () -> MainSettingsConfig.getInstance().getHeartsFreezingPercentage(),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "HeartsFreezingPercentage"),
        (decoder) -> decoder.getDouble("HeartsFreezingPercentage"),
        (saver) -> MainSettingsConfig.getInstance().setHeartsFreezingPercentage(saver));

        COLD_MINING_IMPAIRMENT = addSyncedSetting("cold_mining_slowdown", () -> MainSettingsConfig.getInstance().getColdMiningImpairment(),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "ColdMiningImpairment"),
        (decoder) -> decoder.getDouble("ColdMiningImpairment"),
        (saver) -> MainSettingsConfig.getInstance().setColdMiningImpairment(saver));

        COLD_MOVEMENT_SLOWDOWN = addSyncedSetting("cold_movement_slowdown", () -> MainSettingsConfig.getInstance().getColdMovementSlowdown(),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "ColdMovementSlowdown"),
        (decoder) -> decoder.getDouble("ColdMovementSlowdown"),
        (saver) -> MainSettingsConfig.getInstance().setColdMovementSlowdown(saver));

        COLD_KNOCKBACK_REDUCTION = addSyncedSetting("cold_knockback_reduction", () -> MainSettingsConfig.getInstance().getColdKnockbackReduction(),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "ColdKnockbackReduction"),
        (decoder) -> decoder.getDouble("ColdKnockbackReduction"),
        (saver) -> MainSettingsConfig.getInstance().setColdKnockbackReduction(saver));

        HEATSTROKE_FOG_DISTANCE = addSyncedSetting("heatstroke_fog_distance", () -> MainSettingsConfig.getInstance().getHeatstrokeFogDistance(),
        (encoder) -> ConfigHelper.serializeNbtDouble(encoder, "HeatstrokeFogDistance"),
        (decoder) -> decoder.getDouble("HeatstrokeFogDistance"),
        (saver) -> MainSettingsConfig.getInstance().setHeatstrokeFogDistance(saver));


        BIOME_TEMPS = addSyncedSettingWithRegistries("biome_temps", (registryAccess) -> ConfigHelper.getBiomesWithValues(WorldSettingsConfig.getInstance().getBiomeTemperatures(), true, registryAccess),
        (encoder, registryAccess) -> ConfigHelper.serializeBiomeTemps(encoder, "BiomeTemps", registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeBiomeTemps(decoder, "BiomeTemps", registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.getInstance().setBiomeTemperatures(saver.entrySet().stream()
                                                            .map(entry ->
                                                            {
                                                                ResourceLocation biome = RegistryHelper.getBiomeId(entry.getKey(), registryAccess);
                                                                if (biome == null) return null;

                                                                Temperature.Units units = entry.getValue().getC();
                                                                double min = Temperature.convert(entry.getValue().getA(), Temperature.Units.MC, units, true);
                                                                double max = Temperature.convert(entry.getValue().getB(), Temperature.Units.MC, units, true);

                                                                return Arrays.asList(biome.toString(), min, max, units.toString());
                                                            })
                                                            .filter(Objects::nonNull)
                                                            .collect(Collectors.toList())));

        BIOME_OFFSETS = addSyncedSettingWithRegistries("biome_offsets", (registryAccess) -> ConfigHelper.getBiomesWithValues(WorldSettingsConfig.getInstance().getBiomeTempOffsets(), false, registryAccess),
        (encoder, registryAccess) -> ConfigHelper.serializeBiomeTemps(encoder, "BiomeOffsets", registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeBiomeTemps(decoder, "BiomeOffsets", registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.getInstance().setBiomeTempOffsets(saver.entrySet().stream()
                                                            .map(entry ->
                                                            {
                                                                ResourceLocation biome = RegistryHelper.getBiomeId(entry.getKey(), registryAccess);
                                                                if (biome == null) return null;

                                                                Temperature.Units units = entry.getValue().getC();
                                                                double min = Temperature.convert(entry.getValue().getA(), Temperature.Units.MC, units, false);
                                                                double max = Temperature.convert(entry.getValue().getB(), Temperature.Units.MC, units, false);

                                                                return Arrays.asList(biome.toString(), min, max, units.toString());
                                                            })
                                                            .filter(Objects::nonNull)
                                                            .collect(Collectors.toList())));

        DIMENSION_TEMPS = addSyncedSettingWithRegistries("dimension_temps", (registryAccess) -> ConfigHelper.getDimensionsWithValues(WorldSettingsConfig.getInstance().getDimensionTemperatures(), true, registryAccess),
        (encoder, registryAccess) -> ConfigHelper.serializeDimensionTemps(encoder, "DimensionTemps", registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeDimensionTemps(decoder, "DimensionTemps", registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.getInstance().setDimensionTemperatures(saver.entrySet().stream()
                                                     .map(entry ->
                                                     {
                                                         ResourceLocation dim = RegistryHelper.getDimensionId(entry.getKey(), registryAccess);
                                                         if (dim == null) return null;

                                                         Temperature.Units units = entry.getValue().getSecond();
                                                         double temp = Temperature.convert(entry.getValue().getFirst(), Temperature.Units.MC, units, true);

                                                         return Arrays.asList(dim.toString(), temp, units.toString());
                                                     })
                                                     .filter(Objects::nonNull)
                                                     .collect(Collectors.toList())));

        DIMENSION_OFFSETS = addSyncedSettingWithRegistries("dimension_offsets", (registryAccess) -> ConfigHelper.getDimensionsWithValues(WorldSettingsConfig.getInstance().getDimensionTempOffsets(), false, registryAccess),
        (encoder, registryAccess) -> ConfigHelper.serializeDimensionTemps(encoder, "DimensionOffsets", registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeDimensionTemps(decoder, "DimensionOffsets", registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.getInstance().setDimensionTempOffsets(saver.entrySet().stream()
                                                     .map(entry ->
                                                     {
                                                         ResourceLocation dim = RegistryHelper.getDimensionId(entry.getKey(), registryAccess);
                                                         if (dim == null) return null;

                                                         Temperature.Units units = entry.getValue().getSecond();
                                                         double temp = Temperature.convert(entry.getValue().getFirst(), Temperature.Units.MC, units, false);

                                                         return Arrays.asList(dim.toString(), temp, units.toString());
                                                     })
                                                     .filter(Objects::nonNull)
                                                     .collect(Collectors.toList())));

        STRUCTURE_TEMPS = addSyncedSettingWithRegistries("structure_temperatures", (registryAccess) -> ConfigHelper.getStructuresWithValues(WorldSettingsConfig.getInstance().getStructureTemperatures(), true, registryAccess),
        (encoder, registryAccess) -> ConfigHelper.serializeStructureTemps(encoder, "StructureTemperatures", registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeStructureTemps(decoder, "StructureTemperatures", registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.getInstance().setStructureTemperatures(saver.entrySet().stream()
                                                     .map(entry ->
                                                     {
                                                         ResourceLocation struct = RegistryHelper.getStructureId(entry.getKey(), registryAccess);
                                                         if (struct == null) return null;

                                                         Temperature.Units units = entry.getValue().getSecond();
                                                         double temp = Temperature.convert(entry.getValue().getFirst(), Temperature.Units.MC, units, true);

                                                         return Arrays.asList(struct.toString(), temp, units.toString());
                                                     })
                                                     .filter(Objects::nonNull)
                                                     .collect(Collectors.toList())));

        STRUCTURE_OFFSETS = addSyncedSettingWithRegistries("structure_offsets", (registryAccess) -> ConfigHelper.getStructuresWithValues(WorldSettingsConfig.getInstance().getStructureTempOffsets(), false, registryAccess),
        (encoder, registryAccess) -> ConfigHelper.serializeStructureTemps(encoder, "StructureOffsets", registryAccess),
        (decoder, registryAccess) -> ConfigHelper.deserializeStructureTemps(decoder, "StructureOffsets", registryAccess),
        (saver, registryAccess) -> WorldSettingsConfig.getInstance().setStructureTempOffsets(saver.entrySet().stream()
                                                     .map(entry ->
                                                     {
                                                         ResourceLocation struct = RegistryHelper.getStructureId(entry.getKey(), registryAccess);
                                                         if (struct == null) return null;

                                                         Temperature.Units units = entry.getValue().getSecond();
                                                         double temp = Temperature.convert(entry.getValue().getFirst(), Temperature.Units.MC, units, false);

                                                         return Arrays.asList(struct.toString(), temp, units.toString());
                                                     })
                                                     .filter(Objects::nonNull)
                                                     .collect(Collectors.toList())));

        CAVE_INSULATION = addSetting("cave_insulation", () -> WorldSettingsConfig.getInstance().getCaveInsulation());

        DEPTH_REGIONS = addSetting("depth_regions", () -> new ArrayList<>());

        BiFunction<Item, List<?>, PredicateItem> fuelMapper = (item, args) ->
        {
            double fuel = ((Number) args.get(0)).doubleValue();
            NbtRequirement nbtRequirement;
            if (args.size() > 2)
            {   nbtRequirement = new NbtRequirement(NBTHelper.parseCompoundNbt((String) args.get(2)));
            }
            else nbtRequirement = new NbtRequirement(new CompoundTag());
            return new PredicateItem(fuel, new ItemRequirement(Optional.of(List.of(Either.right(item))),
                                                               Optional.empty(), Optional.empty(),
                                                               Optional.empty(), Optional.empty(),
                                                               Optional.empty(), Optional.empty(),
                                                               nbtRequirement),
                                     EntityRequirement.NONE);
        };
        BOILER_FUEL = addSetting("boiler_fuel_items", () -> ConfigHelper.readItemMap(ItemSettingsConfig.getInstance().getBoilerFuelItems(), fuelMapper));
        ICEBOX_FUEL = addSetting("icebox_fuel_items", () -> ConfigHelper.readItemMap(ItemSettingsConfig.getInstance().getIceboxFuelItems(), fuelMapper));
        HEARTH_FUEL = addSetting("hearth_fuel_items", () -> ConfigHelper.readItemMap(ItemSettingsConfig.getInstance().getHearthFuelItems(), fuelMapper));

        SOULSPRING_LAMP_FUEL = addSyncedSetting("lamp_fuel_items", () -> ConfigHelper.readItemMap(ItemSettingsConfig.getInstance().getSoulLampFuelItems(), fuelMapper),
        (encoder) -> ConfigHelper.serializeItemMap(encoder, "LampFuelItems", fuel -> fuel.serialize()),
        (decoder) -> ConfigHelper.deserializeItemMap(decoder, "LampFuelItems", nbt -> PredicateItem.deserialize(nbt)),
        (saver) -> ConfigHelper.writeItemMap(saver,
                                           list -> ItemSettingsConfig.getInstance().setSoulLampFuelItems(list),
                                           fuel -> List.of(fuel.value(), fuel.data().nbt().tag().toString())));

        HEARTH_POTIONS_ENABLED = addSetting("hearth_potions_enabled", () -> ItemSettingsConfig.getInstance().arePotionsEnabled());
        HEARTH_POTION_BLACKLIST = addSetting("hearth_potion_blacklist", () -> ItemSettingsConfig.getInstance().getPotionBlacklist()
                                                                          .stream()
                                                                          .map(entry -> ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(entry)))
                                                                          .collect(ArrayList::new, List::add, List::addAll));

        INSULATION_ITEMS = addSyncedSetting("insulation_items", () -> ConfigHelper.readItemInsulations(ItemSettingsConfig.getInstance().getInsulationItems(), Insulation.Slot.ITEM),
        (encoder) -> ConfigHelper.serializeItemInsulations(encoder, "InsulationItems"),
        (decoder) -> ConfigHelper.deserializeItemInsulations(decoder, "InsulationItems"),
        (saver) -> ConfigHelper.writeItemInsulations(saver, list -> ItemSettingsConfig.getInstance().setInsulationItems(list)));

        INSULATING_ARMORS = addSyncedSetting("insulating_armors", () -> ConfigHelper.readItemInsulations(ItemSettingsConfig.getInstance().getInsulatingArmorItems(), Insulation.Slot.ARMOR),
        (encoder) -> ConfigHelper.serializeItemInsulations(encoder, "InsulatingArmors"),
        (decoder) -> ConfigHelper.deserializeItemInsulations(decoder, "InsulatingArmors"),
        (saver) -> ConfigHelper.writeItemInsulations(saver, list -> ItemSettingsConfig.getInstance().setInsulatingArmorItems(list)));

        INSULATING_CURIOS = addSyncedSetting("insulating_curios", () ->
        {   if (!CompatManager.isCuriosLoaded()) return new HashMap<>();
            return ConfigHelper.readItemInsulations(ItemSettingsConfig.getInstance().getInsulatingCurios(), Insulation.Slot.CURIO);
        },
        (encoder) -> ConfigHelper.serializeItemInsulations(encoder, "InsulatingCurios"),
        (decoder) -> ConfigHelper.deserializeItemInsulations(decoder, "InsulatingCurios"),
        (saver) ->
        {   if (CompatManager.isCuriosLoaded())
            {   ConfigHelper.writeItemInsulations(saver, list -> ItemSettingsConfig.getInstance().setInsulatingCurios(list));
            }
        });

        INSULATION_SLOTS = addSyncedSetting("insulation_slots", () ->
        {
            List<?> list = ItemSettingsConfig.getInstance().getArmorInsulationSlots();
            // Handle legacy insulation notation
            if (list.size() == 4 && list.stream().allMatch(el -> el instanceof Integer))
            {   list = List.of("static", list.get(0), list.get(1), list.get(2), list.get(3));
            }
            String mode = ((String) list.get(0));

            ScalingFormula.Type scalingType = ScalingFormula.Type.byName(mode);
            List<? extends Number> values = list.subList(1, list.size()).stream().map(o -> (Number) o).toList();

            return scalingType == ScalingFormula.Type.STATIC
                                  ? new ScalingFormula.Static(values.get(0).intValue(),
                                                              values.get(1).intValue(),
                                                              values.get(2).intValue(),
                                                              values.get(3).intValue())
                                  : new ScalingFormula.Dynamic(scalingType,
                                                               values.get(0).doubleValue(),
                                                               values.size() > 2 ? values.get(2).doubleValue() : Double.MAX_VALUE);
        },
        (encoder) ->
        {
            CompoundTag tag = new CompoundTag();
            tag.putString("Mode", encoder.getType().getSerializedName());

            ListTag values = new ListTag();
            encoder.getValues().forEach(value -> values.add(DoubleTag.valueOf(value.doubleValue())));
            tag.put("Values", values);

            return tag;
        },
        (decoder) ->
        {
            ScalingFormula.Type scalingType = ScalingFormula.Type.byName(decoder.getString("Mode"));
            List<? extends Number> values = decoder.getList("Values", 6)
                    .stream()
                    .map(tag -> ((DoubleTag) tag).getAsNumber()).toList();

            return scalingType == ScalingFormula.Type.STATIC
                                  ? new ScalingFormula.Static(values.get(0).intValue(),
                                                              values.get(1).intValue(),
                                                              values.get(2).intValue(),
                                                              values.get(3).intValue())
                                  : new ScalingFormula.Dynamic(scalingType,
                                                               values.get(0).doubleValue(),
                                                               values.size() > 2 ? values.get(2).doubleValue() : Double.MAX_VALUE);
        },
        (saver) ->
        {
            List list = new ArrayList<>();
            list.add(saver.getType().getSerializedName());
            list.addAll(saver.getValues());
            ItemSettingsConfig.getInstance().setArmorInsulationSlots(list);
        });

        INSULATION_STRENGTH = addSetting("insulation_strength", () -> ItemSettingsConfig.getInstance().getInsulationStrength());

        INSULATION_BLACKLIST = addSetting("insulation_blacklist", () -> ItemSettingsConfig.getInstance().getInsulationBlacklist()
                                                                        .stream()
                                                                        .map(entry -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(entry)))
                                                                        .collect(ArrayList::new, List::add, List::addAll));

        CHECK_SLEEP_CONDITIONS = addSetting("check_sleep_conditions", () -> WorldSettingsConfig.getInstance().isSleepChecked());

        SLEEP_CHECK_IGNORE_BLOCKS = addSetting("sleep_check_override_blocks", () -> ConfigHelper.getBlocks(WorldSettingsConfig.getInstance().getSleepOverrideBlocks().toArray(new String[0])));

        FOOD_TEMPERATURES = addSyncedSetting("food_temperatures", () -> ConfigHelper.readItemMap(ItemSettingsConfig.getInstance().getFoodTemperatures(), (item, args) ->
        {
            double value = ((Number) args.get(0)).doubleValue();
            NbtRequirement nbtRequirement = args.size() > 1
                                            ? new NbtRequirement(NBTHelper.parseCompoundNbt((String) args.get(1)))
                                            : new NbtRequirement(new CompoundTag());
            Integer duration = args.size() > 2 ? ((Number) args.get(2)).intValue() : null;
            ItemRequirement itemRequirement = new ItemRequirement(Optional.of(List.of(Either.right(item))),
                                                                  Optional.empty(), Optional.empty(),
                                                                  Optional.empty(), Optional.empty(),
                                                                  Optional.empty(), Optional.empty(),
                                                                  nbtRequirement);
            CompoundTag tag = new CompoundTag();
            if (duration != null)
            {   tag.putInt("duration", duration);
            }
            return new PredicateItem(value, itemRequirement, EntityRequirement.NONE, tag);
        }),
        (encoder) -> ConfigHelper.serializeItemMap(encoder, "FoodTemperatures", food -> food.serialize()),
        (decoder) -> ConfigHelper.deserializeItemMap(decoder, "FoodTemperatures", nbt -> PredicateItem.deserialize(nbt)),
        (saver) -> ConfigHelper.writeItemMap(saver,
                                             list -> ItemSettingsConfig.getInstance().setFoodTemperatures(list),
                                             food ->
                                             {
                                                 List<Object> foodData = new ArrayList<>(List.of(food.value(), food.data().nbt().tag().toString()));
                                                 if (food.extraData().contains("duration"))
                                                 {   foodData.add(food.extraData().getInt("duration"));
                                                 }
                                                 return foodData;
                                             }));

        CARRIED_ITEM_TEMPERATURES = addSyncedSetting("carried_item_temps", () ->
        {
            List<?> list = ItemSettingsConfig.getInstance().getCarriedTemps();
            Map<Item, CarriedItemTemperature> map = new FastMap<>();

            for (Object entry : list)
            {
                List<?> entryList = (List<?>) entry;
                // item ID
                String itemID = (String) entryList.get(0);
                Either<TagKey<Item>, Item> item = itemID.startsWith("#")
                                                  ? Either.left(TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(itemID.substring(1))))
                                                  : Either.right(ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemID)));
                //temp
                double temp = ((Number) entryList.get(1)).doubleValue();
                // slots
                List<Either<IntegerBounds, EquipmentSlot>> slots = switch ((String) entryList.get(2))
                {
                    case "inventory" -> List.of(Either.left(IntegerBounds.NONE));
                    case "hotbar"    -> List.of(Either.left(new IntegerBounds(36, 44)));
                    case "hand" -> List.of(Either.right(EquipmentSlot.MAINHAND), Either.right(EquipmentSlot.OFFHAND));
                    default -> List.of(Either.left(new IntegerBounds(-1, -1)));
                };
                // trait
                Temperature.Trait trait = Temperature.Trait.fromID((String) entryList.get(3));
                // nbt
                NbtRequirement nbtRequirement = entryList.size() > 4
                                                ? new NbtRequirement(NBTHelper.parseCompoundNbt((String) entryList.get(4)))
                                                : new NbtRequirement(new CompoundTag());
                // compile item requirement
                ItemRequirement itemRequirement = new ItemRequirement(Optional.of(List.of(item)),
                                                                      Optional.empty(), Optional.empty(),
                                                                      Optional.empty(), Optional.empty(),
                                                                      Optional.empty(), Optional.empty(),
                                                                      nbtRequirement);
                // final carried temp
                CarriedItemTemperature carriedTemp = new CarriedItemTemperature(itemRequirement, slots, temp, trait, IntegerBounds.NONE, EntityRequirement.NONE);

                // add carried temp to map
                if (item.left().isPresent())
                {   ForgeRegistries.ITEMS.tags().getTag(item.left().get()).stream().forEach(i -> map.put(i, carriedTemp));
                }
                else
                {   map.put(item.right().get(), carriedTemp);
                }
            }
            return map;
        },
        (encoder) ->
        {
            CompoundTag tag = new CompoundTag();
            ListTag list = new ListTag();
            encoder.forEach((item, temp) ->
            {
                CompoundTag entry = new CompoundTag();
                entry.putString("Id", ForgeRegistries.ITEMS.getKey(item).toString());
                entry.put("Value", temp.serialize());
                list.add(entry);
            });
            tag.put("CarriedItemTemps", list);
            return tag;
        },
        (decoder) ->
        {
            List<?> list = decoder.getList("CarriedItemTemps", 10);
            Map<Item, CarriedItemTemperature> map = new FastMap<>();
            list.forEach(entry ->
            {
                CompoundTag entryTag = (CompoundTag) entry;
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(entryTag.getString("Id")));
                CarriedItemTemperature temp = CarriedItemTemperature.deserialize(entryTag.getCompound("Value"));
                map.put(item, temp);
            });
            return map;
        },
        (saver) ->
        {
            ConfigHelper.writeItemMap(saver,
            list -> ItemSettingsConfig.getInstance().setCarriedTemps(list),
            temp ->
            {
                List<Object> entry = new ArrayList<>();
                // Temperature
                entry.add(temp.temperature());
                // Slot types
                String strictType = temp.getSlotRangeName();
                if (strictType.isEmpty()) return null;
                entry.add(strictType);
                // Trait
                entry.add(temp.trait().getSerializedName());
                // NBT data
                if (!temp.item().nbt().tag().isEmpty())
                {   entry.add(temp.item().nbt().tag().toString());
                }
                return entry;
            });
        });

        WATERSKIN_STRENGTH = addSetting("waterskin_strength", () -> ItemSettingsConfig.getInstance().getWaterskinStrength());

        LAMP_DIMENSIONS = addSettingWithRegistries("valid_lamp_dimensions", (registryAccess) -> new ArrayList<>(ItemSettingsConfig.getInstance().getValidSoulLampDimensions()
                                                                                    .stream()
                                                                                    .map(entry -> registryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY).get(new ResourceLocation(entry)))
                                                                                    .collect(ArrayList::new, List::add, List::addAll)));

        FUR_TIMINGS = addSyncedSetting("fur_timings", () ->
        {   List<?> entry = EntitySettingsConfig.getInstance().getGoatFurStats();
            return new Triplet<>(((Number) entry.get(0)).intValue(), ((Number) entry.get(1)).intValue(), ((Number) entry.get(2)).doubleValue());
        },
        (encoder) ->
        {   CompoundTag tag = new CompoundTag();
            tag.put("Interval", IntTag.valueOf(encoder.getA()));
            tag.put("Cooldown", IntTag.valueOf(encoder.getB()));
            tag.put("Chance", DoubleTag.valueOf(encoder.getC()));
            return tag;
        },
        (decoder) ->
        {   int interval = decoder.getInt("Interval");
            int cooldown = decoder.getInt("Cooldown");
            double chance = decoder.getDouble("Chance");
            return new Triplet<>(interval, cooldown, chance);
        },
        (saver) ->
        {   List<Number> list = new ArrayList<>();
            list.add(saver.getA());
            list.add(saver.getB());
            list.add(saver.getC());
            EntitySettingsConfig.getInstance().setGoatFurStats(list);
        });

        SHED_TIMINGS = addSyncedSetting("shed_timings", () ->
        {   List<?> entry = EntitySettingsConfig.getInstance().getChameleonShedStats();
            return new Triplet<>(((Number) entry.get(0)).intValue(), ((Number) entry.get(1)).intValue(), ((Number) entry.get(2)).doubleValue());
        },
        (encoder) ->
        {   CompoundTag tag = new CompoundTag();
            tag.put("Interval", IntTag.valueOf(encoder.getA()));
            tag.put("Cooldown", IntTag.valueOf(encoder.getB()));
            tag.put("Chance", DoubleTag.valueOf(encoder.getC()));
            return tag;
        },
        (decoder) ->
        {   int interval = decoder.getInt("Interval");
            int cooldown = decoder.getInt("Cooldown");
            double chance = decoder.getDouble("Chance");
            return new Triplet<>(interval, cooldown, chance);
        },
        (saver) ->
        {   List<Number> list = new ArrayList<>();
            list.add(saver.getA());
            list.add(saver.getB());
            list.add(saver.getC());
            EntitySettingsConfig.getInstance().setChameleonShedStats(list);
        });

        ENTITY_SPAWN_BIOMES = addSettingWithRegistries("entity_spawn_biomes", (registryAccess) ->
        {
            Multimap<Biome, SpawnBiomeData> map = HashMultimap.create();
            // Function to read biomes from configs and put them in the config settings
            BiConsumer<List<? extends List<?>>, EntityType<?>> configReader = (configBiomes, entityType) ->
            {
                for (List<?> entry : configBiomes)
                {
                    String biomeId = ((String) entry.get(0));
                    List<Biome> biomes = ConfigHelper.parseRegistryItems(Registry.BIOME_REGISTRY, registryAccess, biomeId);
                    Either<TagKey<Biome>, Biome> biomeEither;
                    if (biomeId.charAt(0) == '#')
                    {   biomeEither = Either.left(TagKey.create(Registry.BIOME_REGISTRY, new ResourceLocation(biomeId.substring(1))));
                    }
                    else
                    {   Biome biome = RegistryHelper.getBiome(new ResourceLocation(biomeId), registryAccess);
                        if (biome != null)
                            biomeEither = Either.right(biome);
                        else
                            continue;
                    }
                    for (Biome biome : biomes)
                    {
                        SpawnBiomeData spawnData = new SpawnBiomeData(List.of(biomeEither), MobCategory.CREATURE, ((Number) entry.get(1)).intValue(),
                                                                      List.of(Either.right(entityType)),
                                                                      Optional.empty());
                        map.put(biome, spawnData);
                    }
                }
            };

            // Parse goat and chameleon biomes
            configReader.accept(EntitySettingsConfig.getInstance().getChameleonSpawnBiomes(), ModEntities.CHAMELEON);
            configReader.accept(EntitySettingsConfig.getInstance().getGoatSpawnBiomes(), EntityType.GOAT);

            return map;
        });

        INSULATED_ENTITIES = addSetting("insulated_entities", () ->
        EntitySettingsConfig.getInstance().getInsulatedEntities().stream().map(entry ->
        {
            List<Map.Entry<EntityType<?>, InsulatingMount>> entries = new ArrayList<>();
            String entityID = (String) entry.get(0);
            double coldInsul = ((Number) entry.get(1)).doubleValue();
            double hotInsul = entry.size() < 3
                              ? coldInsul
                              : ((Number) entry.get(2)).doubleValue();

            for (EntityType<?> entityType : ConfigHelper.getEntityTypes(entityID))
            {   entries.add(Map.entry(entityType, new InsulatingMount(entityType, coldInsul, hotInsul, EntityRequirement.NONE)));
            }

            return entries;
        })
        .flatMap(List::stream)
        .distinct()
        .filter(entry -> entry.getKey() != null)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> {
            ColdSweat.LOGGER.warn("Duplicate entity entry for \"{}\" found in config. Using the first entry.", ForgeRegistries.ENTITY_TYPES.getKey(a.entityType()).toString());
            return a;
        })));

        BLOCK_RANGE = addSyncedSetting("block_range", () -> WorldSettingsConfig.getInstance().getBlockRange(),
        (encoder) -> ConfigHelper.serializeNbtInt(encoder, "BlockRange"),
        (decoder) -> decoder.getInt("BlockRange"),
        (saver) -> WorldSettingsConfig.getInstance().setBlockRange(saver));

        COLD_SOUL_FIRE = addSetting("cold_soul_fire", () -> WorldSettingsConfig.getInstance().isSoulFireCold());

        HEARTH_SPREAD_WHITELIST = addSyncedSetting("hearth_spread_whitelist", () -> ConfigHelper.getBlocks(WorldSettingsConfig.getInstance().getHearthSpreadWhitelist().toArray(new String[0])),
        (encoder) ->
        {
            CompoundTag tag = new CompoundTag();
            ListTag list = new ListTag();
            for (Block entry : encoder)
            {   list.add(StringTag.valueOf(ForgeRegistries.BLOCKS.getKey(entry).toString()));
            }
            tag.put("HearthWhitelist", list);
            return tag;
        },
        (decoder) ->
        {
            List<Block> list = new ArrayList<>();
            for (Tag entry : decoder.getList("HearthWhitelist", 8))
            {   list.add(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.getAsString())));
            }
            return list;
        },
        saver -> WorldSettingsConfig.getInstance().setHearthSpreadWhitelist(saver.stream().map(ForgeRegistries.BLOCKS::getKey).toList()));

        HEARTH_SPREAD_BLACKLIST = addSyncedSetting("hearth_spread_blacklist", () -> ConfigHelper.getBlocks(WorldSettingsConfig.getInstance().getHearthSpreadBlacklist().toArray(new String[0])),
        (encoder) ->
        {
            CompoundTag tag = new CompoundTag();
            ListTag list = new ListTag();
            for (Block entry : encoder)
            {   list.add(StringTag.valueOf(ForgeRegistries.BLOCKS.getKey(entry).toString()));
            }
            tag.put("HearthBlacklist", list);
            return tag;
        },
        (decoder) ->
        {
            List<Block> list = new ArrayList<>();
            for (Tag entry : decoder.getList("HearthBlacklist", 8))
            {   list.add(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.getAsString())));
            }
            return list;
        },
        saver -> WorldSettingsConfig.getInstance().setHearthSpreadBlacklist(saver.stream().map(ForgeRegistries.BLOCKS::getKey).toList()));

        HEARTH_STRENGTH = addSetting("hearth_effect", () -> WorldSettingsConfig.getInstance().getHearthStrength());

        SMART_HEARTH = addSetting("smart_hearth", () -> WorldSettingsConfig.getInstance().isSmartHearth());

        CELSIUS = addClientSetting("celsius", () -> ClientSettingsConfig.getInstance().isCelsius());

        TEMP_OFFSET = addClientSetting("temp_offset", () -> ClientSettingsConfig.getInstance().getTempOffset());

        TEMP_SMOOTHING = addClientSetting("temp_smoothing", () -> ClientSettingsConfig.getInstance().getTempSmoothing());

        BODY_ICON_POS = addClientSetting("body_icon_pos", () -> new Vec2i(ClientSettingsConfig.getInstance().getBodyIconX(),
                                                                  ClientSettingsConfig.getInstance().getBodyIconY()));
        BODY_ICON_ENABLED = addClientSetting("body_icon_enabled", () -> ClientSettingsConfig.getInstance().isBodyIconEnabled());

        MOVE_BODY_ICON_WHEN_ADVANCED = addClientSetting("move_body_icon_for_advanced", () -> ClientSettingsConfig.getInstance().moveBodyIconWhenAdvanced());

        BODY_READOUT_POS = addClientSetting("body_readout_pos", () -> new Vec2i(ClientSettingsConfig.getInstance().getBodyReadoutX(),
                                                                                   ClientSettingsConfig.getInstance().getBodyReadoutY()));
        BODY_READOUT_ENABLED = addClientSetting("body_readout_enabled", () -> ClientSettingsConfig.getInstance().isBodyReadoutEnabled());

        WORLD_GAUGE_POS = addClientSetting("world_gauge_pos", () -> new Vec2i(ClientSettingsConfig.getInstance().getWorldGaugeX(),
                                                                    ClientSettingsConfig.getInstance().getWorldGaugeY()));
        WORLD_GAUGE_ENABLED = addClientSetting("world_gauge_enabled", () -> ClientSettingsConfig.getInstance().isWorldGaugeEnabled());

        CUSTOM_HOTBAR_LAYOUT = addClientSetting("custom_hotbar_layout", () -> ClientSettingsConfig.getInstance().isCustomHotbarLayout());
        ICON_BOBBING = addClientSetting("icon_bobbing", () -> ClientSettingsConfig.getInstance().isIconBobbing());

        HEARTH_DEBUG = addClientSetting("hearth_debug", () -> ClientSettingsConfig.getInstance().isHearthDebug());

        SHOW_CONFIG_BUTTON = addClientSetting("show_config_button", () -> ClientSettingsConfig.getInstance().showConfigButton());
        CONFIG_BUTTON_POS = addClientSetting("config_button_pos", () -> new Vec2i(ClientSettingsConfig.getInstance().getConfigButtonPos().get(0),
                                                                          ClientSettingsConfig.getInstance().getConfigButtonPos().get(1)));

        DISTORTION_EFFECTS = addClientSetting("distortion_effects", () -> ClientSettingsConfig.getInstance().areDistortionsEnabled());

        HIGH_CONTRAST = addClientSetting("high_contrast", () -> ClientSettingsConfig.getInstance().isHighContrast());

        SHOW_CREATIVE_WARNING = addClientSetting("show_creative_warning", () -> ClientSettingsConfig.getInstance().showCreativeWarning());

        HIDE_TOOLTIPS = addClientSetting("hide_tooltips", () -> ClientSettingsConfig.getInstance().hideTooltips());

        boolean ssLoaded = CompatManager.isSereneSeasonsLoaded();
        SUMMER_TEMPS = addSetting("summer_temps", ssLoaded ? () -> WorldSettingsConfig.getInstance().getSummerTemps() : () -> new Double[3]);
        AUTUMN_TEMPS = addSetting("autumn_temps", ssLoaded ? () -> WorldSettingsConfig.getInstance().getAutumnTemps() : () -> new Double[3]);
        WINTER_TEMPS = addSetting("winter_temps", ssLoaded ? () -> WorldSettingsConfig.getInstance().getWinterTemps() : () -> new Double[3]);
        SPRING_TEMPS = addSetting("spring_temps", ssLoaded ? () -> WorldSettingsConfig.getInstance().getSpringTemps() : () -> new Double[3]);
    }

    public static String getKey(DynamicHolder<?> setting)
    {   return CONFIG_SETTINGS.inverse().get(setting);
    }

    public enum Difficulty
    {
        SUPER_EASY(() -> Map.of(
            getKey(MIN_TEMP), () -> Temperature.convert(40, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(MAX_TEMP), () -> Temperature.convert(120, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(TEMP_RATE), () -> 0.5,
            getKey(REQUIRE_THERMOMETER), () -> false,
            getKey(FIRE_RESISTANCE_ENABLED), () -> true,
            getKey(ICE_RESISTANCE_ENABLED), () -> true
        )),

        EASY(() -> Map.of(
            getKey(MIN_TEMP), () -> Temperature.convert(45, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(MAX_TEMP), () -> Temperature.convert(110, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(TEMP_RATE), () -> 0.75,
            getKey(REQUIRE_THERMOMETER), () -> false,
            getKey(FIRE_RESISTANCE_ENABLED), () -> true,
            getKey(ICE_RESISTANCE_ENABLED), () -> true
        )),

        NORMAL(() -> Map.of(
            getKey(MIN_TEMP), () -> Temperature.convert(50, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(MAX_TEMP), () -> Temperature.convert(100, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(TEMP_RATE), () -> 1.0,
            getKey(REQUIRE_THERMOMETER), () -> true,
            getKey(FIRE_RESISTANCE_ENABLED), () -> true,
            getKey(ICE_RESISTANCE_ENABLED), () -> true
        )),

        HARD(() -> Map.of(
            getKey(MIN_TEMP), () -> Temperature.convert(55, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(MAX_TEMP), () -> Temperature.convert(90, Temperature.Units.F, Temperature.Units.MC, true),
            getKey(TEMP_RATE), () -> 1.25,
            getKey(REQUIRE_THERMOMETER), () -> true,
            getKey(FIRE_RESISTANCE_ENABLED), () -> false,
            getKey(ICE_RESISTANCE_ENABLED), () -> false
        )),

        CUSTOM(() -> Map.of());

        private final Supplier<Map<String, Supplier<?>>> settingsSupplier;
        private Map<String, Supplier<?>> settings;

        Difficulty(Supplier<Map<String, Supplier<?>>> settings)
        {   this.settingsSupplier = settings;
        }

        private void ensureSettingsGenerated()
        {   if (settings == null) settings = settingsSupplier.get();
        }

        public <T> T getSetting(String id)
        {
            this.ensureSettingsGenerated();
            return (T) settings.get(id).get();
        }

        public <T> T getOrDefault(String id, T defaultValue)
        {
            this.ensureSettingsGenerated();
            return (T) settings.getOrDefault(id, () -> defaultValue).get();
        }

        public void load()
        {
            this.ensureSettingsGenerated();
            settings.forEach((id, loader) -> CONFIG_SETTINGS.computeIfPresent(id, (s, holder) ->
            {
                holder.set(loader.get());
                return holder;
            }));
        }
    }

    public static <T> DynamicHolder<T> addSetting(String id, Supplier<T> supplier)
    {   DynamicHolder<T> loader = DynamicHolder.create(supplier);
        CONFIG_SETTINGS.put(id, loader);
        return loader;
    }

    public static <T> DynamicHolder<T> addSettingWithRegistries(String id, DynamicHolder.Getter<T> supplier)
    {   DynamicHolder<T> loader = DynamicHolder.createWithRegistries(supplier);
        CONFIG_SETTINGS.put(id, loader);
        return loader;
    }

    public static <T> DynamicHolder<T> addSyncedSetting(String id, Supplier<T> supplier, Function<T, CompoundTag> writer, Function<CompoundTag, T> reader, Consumer<T> saver)
    {   DynamicHolder<T> loader = DynamicHolder.createSynced(supplier, writer, reader, saver);
        CONFIG_SETTINGS.put(id, loader);
        return loader;
    }

    public static <T> DynamicHolder<T> addSyncedSettingWithRegistries(String id, DynamicHolder.Getter<T> supplier, DynamicHolder.Writer<T> writer, DynamicHolder.Reader<T> reader, DynamicHolder.Saver<T> saver)
    {   DynamicHolder<T> loader = DynamicHolder.createSyncedWithRegistries(supplier, writer, reader, saver);
        CONFIG_SETTINGS.put(id, loader);
        return loader;
    }

    public static <T> DynamicHolder<T> addClientSetting(String id, Supplier<T> supplier)
    {
        return FMLEnvironment.dist == Dist.CLIENT
             ? addSetting(id, supplier)
             : DynamicHolder.create(() -> null);
    }

    public static Map<String, CompoundTag> encode(RegistryAccess registryAccess)
    {
        Map<String, CompoundTag> map = new HashMap<>();
        CONFIG_SETTINGS.forEach((key, value) ->
        {   if (value.isSynced())
            {   map.put(key, value.encode(registryAccess));
            }
        });
        return map;
    }

    public static void decode(String key, CompoundTag tag, RegistryAccess registryAccess)
    {
        CONFIG_SETTINGS.computeIfPresent(key, (k, value) ->
        {   value.decode(tag, registryAccess);
            return value;
        });
    }

    public static void saveValues(RegistryAccess registryAccess)
    {
        CONFIG_SETTINGS.values().forEach(value ->
        {   if (value.isSynced())
            {   value.save(registryAccess);
            }
        });
    }

    public static void load(RegistryAccess registryAccess)
    {
        if (registryAccess != null)
        {
            CONFIG_SETTINGS.values().forEach(dynamicHolder -> dynamicHolder.load(registryAccess));
            ConfigRegistryHandler.collectConfigRegistries(registryAccess);
        }
        else
        {
            ColdSweat.LOGGER.warn("Loading Cold Sweat config settings without registry access. This is normal during startup.");
            CONFIG_SETTINGS.values().forEach(dynamicHolder ->
            {
                if (!dynamicHolder.requiresRegistries())
                {   dynamicHolder.load();
                }
            });
        }
    }
}
