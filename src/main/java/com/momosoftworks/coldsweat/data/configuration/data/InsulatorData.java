package com.momosoftworks.coldsweat.data.configuration.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.api.insulation.AdaptiveInsulation;
import com.momosoftworks.coldsweat.api.insulation.Insulation;
import com.momosoftworks.coldsweat.api.insulation.StaticInsulation;
import com.momosoftworks.coldsweat.api.util.InsulationType;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.List;
import java.util.Optional;

public class InsulatorData implements IForgeRegistryEntry<InsulatorData>
{
    List<Either<ITag<Item>, Item>> items;
    InsulationType type;
    Either<StaticInsulation, AdaptiveInsulation> insulation;
    Optional<CompoundNBT> nbt;
    Optional<EntityRequirement> predicate;
    Optional<List<String>> requiredMods;

    public InsulatorData(List<Either<ITag<Item>, Item>> items, InsulationType type, Either<StaticInsulation, AdaptiveInsulation> insulation, Optional<CompoundNBT> nbt,
                         Optional<EntityRequirement> predicate, Optional<List<String>> requiredMods)
    {   this.items = items;
        this.type = type;
        this.insulation = insulation;
        this.nbt = nbt;
    }

    public static final Codec<InsulatorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.either(ITag.codec(ItemTags::getAllTags), Registry.ITEM).listOf().fieldOf("item").forGetter(insulator -> insulator.items),
            InsulationType.CODEC.fieldOf("type").forGetter(insulator -> insulator.type),
            Codec.either(StaticInsulation.CODEC, AdaptiveInsulation.CODEC).fieldOf("insulation").forGetter(insulator -> insulator.insulation),
            CompoundNBT.CODEC.optionalFieldOf("nbt").forGetter(insulator -> insulator.nbt),
            EntityRequirement.getCodec().optionalFieldOf("predicate").forGetter(data -> data.predicate),
            Codec.STRING.listOf().optionalFieldOf("required_mods").forGetter(data -> data.requiredMods)
    ).apply(instance, InsulatorData::new));

    public Insulation getInsulation()
    {
        if (insulation.left().isPresent())
        {   return insulation.left().get();
        }
        else if (insulation.right().isPresent())
        {   return insulation.right().get();
        }
        throw new IllegalArgumentException(String.format("Insulation %s is not defined!", insulation));
    }

    @Override
    public InsulatorData setRegistryName(ResourceLocation name)
    {
        return this;
    }

    @Override
    public ResourceLocation getRegistryName()
    {
        return new ResourceLocation("coldsweat", "insulators");
    }

    @Override
    public Class<InsulatorData> getRegistryType()
    {
        return InsulatorData.class;
    }
}