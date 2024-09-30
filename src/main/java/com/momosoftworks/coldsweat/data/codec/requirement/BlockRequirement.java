package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record BlockRequirement(Optional<List<Either<TagKey<Block>, Block>>> blocks, Optional<TagKey<Block>> tag, Optional<StateRequirement> state, Optional<NbtRequirement> nbt)
{
    public static final Codec<BlockRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrBuiltinCodec(Registries.BLOCK, BuiltInRegistries.BLOCK).listOf().optionalFieldOf("blocks").forGetter(predicate -> predicate.blocks),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("tag").forGetter(predicate -> predicate.tag),
            StateRequirement.CODEC.optionalFieldOf("state").forGetter(predicate -> predicate.state),
            NbtRequirement.CODEC.optionalFieldOf("nbt").forGetter(predicate -> predicate.nbt)
    ).apply(instance, BlockRequirement::new));

    public boolean test(Level level, BlockPos pos)
    {
        if (!level.isLoaded(pos))
        {   return false;
        }
        else
        {
            BlockState blockstate = level.getBlockState(pos);
            if (this.tag.isPresent() && !blockstate.is(this.tag.get()))
            {   return false;
            }
            else if (this.blocks.isPresent() && !this.blocks.get().contains(Holder.direct(blockstate.getBlock())))
            {   return false;
            }
            else if (this.state.isPresent() && !this.state.get().matches(blockstate))
            {   return false;
            }
            else
            {   if (this.nbt.isPresent())
                {   BlockEntity blockentity = level.getBlockEntity(pos);
                    return blockentity != null && this.nbt.get().test(blockentity.saveWithFullMetadata(level.registryAccess()));
                }

                return true;
            }
        }
    }

    public CompoundTag serialize()
    {   return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
    }

    public static BlockRequirement deserialize(CompoundTag tag)
    {   return CODEC.decode(NbtOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalArgumentException("Could not deserialize BlockRequirement")).getFirst();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {   return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {   return false;
        }

        BlockRequirement that = (BlockRequirement) obj;

        if (!blocks.equals(that.blocks))
        {   return false;
        }
        if (!tag.equals(that.tag))
        {   return false;
        }
        if (!state.equals(that.state))
        {   return false;
        }
        return nbt.equals(that.nbt);
    }

    public record StateRequirement(List<Either<StateProperty, RangedProperty>> properties)
    {
        public static final Codec<StateRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.either(StateProperty.CODEC, RangedProperty.CODEC).listOf().fieldOf("properties").forGetter(StateRequirement::properties)
        ).apply(instance, StateRequirement::new));

        public CompoundTag serialize()
        {   return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        }

        public static StateRequirement deserialize(CompoundTag tag)
        {   return CODEC.decode(NbtOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalArgumentException("Could not deserialize StateRequirement")).getFirst();
        }

        public boolean matches(BlockState pState)
        {   return this.matches(pState.getBlock().getStateDefinition(), pState);
        }

        public boolean matches(FluidState pState)
        {   return this.matches(pState.getType().getStateDefinition(), pState);
        }

        public <S extends StateHolder<?, S>> boolean matches(StateDefinition<?, S> pProperties, S pTargetProperty)
        {
            for(Either<StateProperty, RangedProperty> property : this.properties)
            {
                if (!property.map(
                    stateProperty -> stateProperty.match(pProperties, pTargetProperty),
                    rangedProperty -> rangedProperty.match(pProperties, pTargetProperty)))
                {   return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {   return true;
            }
            if (obj == null || getClass() != obj.getClass())
            {   return false;
            }

            StateRequirement that = (StateRequirement) obj;

            return properties.equals(that.properties);
        }
    }

    public record StateProperty(String name, String value)
    {
        public static final Codec<StateProperty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(property -> property.name),
                Codec.STRING.fieldOf("value").forGetter(property -> property.value)
        ).apply(instance, StateProperty::new));

        public CompoundTag serialize()
        {   return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        }

        public static StateProperty deserialize(CompoundTag tag)
        {   return CODEC.decode(NbtOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalArgumentException("Could not deserialize StateProperty")).getFirst();
        }

        public <S extends StateHolder<?, S>> boolean match(StateDefinition<?, S> pProperties, S pPropertyToMatch)
        {   Property<?> property = pProperties.getProperty(this.name);
            return property != null && this.match(pPropertyToMatch, property);
        }

        private <V extends Comparable<V>> boolean match(StateHolder<?, ?> properties, Property<V> property)
        {   V stateValue = properties.getValue(property);
            Optional<V> propValue = property.getValue(this.value);
            return propValue.isPresent() && stateValue.compareTo(propValue.get()) == 0;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {   return true;
            }
            if (obj == null || getClass() != obj.getClass())
            {   return false;
            }

            StateProperty that = (StateProperty) obj;

            if (!name.equals(that.name))
            {   return false;
            }
            return value.equals(that.value);
        }
    }

    public record RangedProperty(String name, String min, String max)
    {
        public static final Codec<RangedProperty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(property -> property.name),
                Codec.STRING.fieldOf("min").forGetter(property -> property.min),
                Codec.STRING.fieldOf("max").forGetter(property -> property.max)
        ).apply(instance, RangedProperty::new));

        public CompoundTag serialize()
        {   return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        }

        public static RangedProperty deserialize(CompoundTag tag)
        {   return CODEC.decode(NbtOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalArgumentException("Could not deserialize RangedProperty")).getFirst();
        }

        public <S extends StateHolder<?, S>> boolean match(StateDefinition<?, S> pProperties, S pTargetProperty)
        {   Property<?> property = pProperties.getProperty(this.name);
            return property != null && this.match(pTargetProperty, property);
        }

        private <T extends Comparable<T>> boolean match(StateHolder<?, ?> pProperties, Property<T> pPropertyTarget)
        {
            T t = pProperties.getValue(pPropertyTarget);

            if (this.min != null)
            {   Optional<T> optional = pPropertyTarget.getValue(this.min);
                if (optional.isEmpty() || t.compareTo(optional.get()) < 0)
                {   return false;
                }
            }
            if (this.max != null)
            {   Optional<T> optional1 = pPropertyTarget.getValue(this.max);
                if (optional1.isEmpty() || t.compareTo(optional1.get()) > 0)
                {   return false;
                }
            }

            return true;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {   return true;
            }
            if (obj == null || getClass() != obj.getClass())
            {   return false;
            }

            RangedProperty that = (RangedProperty) obj;

            if (!name.equals(that.name))
            {   return false;
            }
            if (!min.equals(that.min))
            {   return false;
            }
            return max.equals(that.max);
        }
    }

    /*@Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("BlockRequirement{");
        blocks.ifPresent(blocks -> builder.append("blocks=").append(blocks));
        tag.ifPresent(tag -> builder.append("tag=").append(tag));
        state.ifPresent(state -> builder.append("state=").append(state));
        nbt.ifPresent(nbt -> builder.append("nbt=").append(nbt));
        builder.append("}");

        return builder.toString();
    }*/
}
