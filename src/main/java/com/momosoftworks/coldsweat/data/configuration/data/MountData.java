package com.momosoftworks.coldsweat.data.configuration.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record MountData(List<Either<TagKey<EntityType<?>>, ResourceLocation>> entities, double coldInsulation, double heatInsulation,
                        EntityRequirement requirement, Optional<List<String>> requiredMods) implements IForgeRegistryEntry<MountData>
{
    public static Codec<MountData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.either(TagKey.codec(Registry.ENTITY_TYPE_REGISTRY), ResourceLocation.CODEC).listOf().fieldOf("entities").forGetter(MountData::entities),
            Codec.DOUBLE.fieldOf("cold_insulation").forGetter(MountData::coldInsulation),
            Codec.DOUBLE.fieldOf("heat_insulation").forGetter(MountData::heatInsulation),
            EntityRequirement.getCodec().fieldOf("requirement").forGetter(MountData::requirement),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(MountData::requiredMods)
    ).apply(instance, MountData::new));

    @Override
    public MountData setRegistryName(ResourceLocation name)
    {
        return null;
    }

    @Nullable
    @Override
    public ResourceLocation getRegistryName()
    {
        return null;
    }

    @Override
    public Class<MountData> getRegistryType()
    {
        return MountData.class;
    }
}
