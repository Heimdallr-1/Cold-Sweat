package com.momosoftworks.coldsweat.data.tag;

import com.momosoftworks.coldsweat.ColdSweat;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;

@EventBusSubscriber
public class ModDimensionTags
{
    public static final TagKey<DimensionType> SOUL_LAMP_VALID = createTag("soulspring_lamp_valid");

    public static final TagKey<DimensionType> HAS_CEILING = createForgeTag("has_ceiling");
    public static final TagKey<DimensionType> HAS_SKY = createForgeTag("has_sky");
    public static final TagKey<DimensionType> NATURAL = createForgeTag("natural");
    public static final TagKey<DimensionType> UNNATURAL = createForgeTag("unnatural");
    public static final TagKey<DimensionType> ULTRAWARM = createForgeTag("ultrawarm");
    public static final TagKey<DimensionType> BED_WORKS = createForgeTag("bed_works");
    public static final TagKey<DimensionType> RESPAWN_ANCHOR_WORKS = createForgeTag("respawn_anchor_works");
    public static final TagKey<DimensionType> PIGLIN_SAFE = createForgeTag("piglin_safe");
    public static final TagKey<DimensionType> HAS_SKYLIGHT = createForgeTag("has_skylight");
    public static final TagKey<DimensionType> HAS_RAIDS = createForgeTag("has_raids");
    public static final TagKey<DimensionType> OVERWORLD_LIKE = createForgeTag("overworld_like");

    private static TagKey<DimensionType> createTag(String name)
    {   return TagKey.create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath(ColdSweat.MOD_ID, name));
    }

    private static TagKey<DimensionType> createForgeTag(String name)
    {   return TagKey.create(Registries.DIMENSION_TYPE, ResourceLocation.fromNamespaceAndPath("c", name));
    }

    private static final Field CONTENTS = ObfuscationReflectionHelper.findField(HolderSet.Named.class, "contents");
    private static final Method BIND = ObfuscationReflectionHelper.findMethod(HolderSet.Named.class, "bind", List.class);
    static
    {
        CONTENTS.setAccessible(true);
        BIND.setAccessible(true);
    }

    /**
     * Scans through the registry for dimensions and assigns them to the appropriate vanilla tags.
     */
    public static void initDynamicTags(RegistryAccess registryAccess)
    {
        fillTag(HAS_CEILING, DimensionType::hasCeiling, registryAccess);
        fillTag(HAS_SKY, dimensionType -> !dimensionType.hasCeiling(), registryAccess);
        fillTag(NATURAL, DimensionType::natural, registryAccess);
        fillTag(UNNATURAL, dimensionType -> !dimensionType.natural(), registryAccess);
        fillTag(ULTRAWARM, DimensionType::ultraWarm, registryAccess);
        fillTag(BED_WORKS, DimensionType::bedWorks, registryAccess);
        fillTag(RESPAWN_ANCHOR_WORKS, DimensionType::respawnAnchorWorks, registryAccess);
        fillTag(PIGLIN_SAFE, DimensionType::piglinSafe, registryAccess);
        fillTag(HAS_SKYLIGHT, DimensionType::hasSkyLight, registryAccess);
        fillTag(HAS_RAIDS, DimensionType::hasRaids, registryAccess);
        fillTag(OVERWORLD_LIKE, dimensionType -> !dimensionType.hasCeiling()
                                              && dimensionType.natural()
                                              && dimensionType.hasSkyLight()
                                              && !dimensionType.ultraWarm(), registryAccess);
    }

    private static void fillTag(TagKey<DimensionType> tag, Predicate<DimensionType> predicate, RegistryAccess registryAccess)
    {
        Registry<DimensionType> dimensionRegistry = registryAccess.registryOrThrow(Registries.DIMENSION_TYPE);
        HolderSet.Named<DimensionType> holderSet = dimensionRegistry.getTag(tag).get();
        Set<Holder<DimensionType>> entries;
        try
        {
            entries = new HashSet<>((List<Holder<DimensionType>>) CONTENTS.get(holderSet));
            dimensionRegistry.holders().forEach(dimensionType ->
            {
                if (predicate.test(dimensionType.value()))
                {   entries.add(dimensionType);
                }
            });
            BIND.invoke(holderSet, new ArrayList<>(entries));
        }
        catch (Exception e)
        {   throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public static void onServerStart(ServerAboutToStartEvent event)
    {
        // Initialize custom vanilla tags for dimension types
        ModDimensionTags.initDynamicTags(event.getServer().registryAccess());
    }
}
