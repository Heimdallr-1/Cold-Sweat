package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public record EffectsRequirement(Map<Holder<MobEffect>, Instance> effects)
{
    public static final Codec<EffectsRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ConfigHelper.tagOrBuiltinHolderCodec(Registries.MOB_EFFECT, BuiltInRegistries.MOB_EFFECT), Instance.CODEC)
            .xmap(map ->
                  {
                        Map<Holder<MobEffect>, Instance> effects = new HashMap<>();
                        for (Map.Entry<Either<TagKey<MobEffect>, Holder<MobEffect>>, Instance> entry : map.entrySet())
                        {
                            entry.getKey().map(
                            tag ->
                            {   BuiltInRegistries.MOB_EFFECT.getTag(tag).get().stream().forEach(effect -> effects.put(effect, entry.getValue()));
                                return null;
                            },
                            effect -> effects.put(effect, entry.getValue()));
                        }
                        return effects;
                  },
                  effects ->
                  {
                      Map<Either<TagKey<MobEffect>, Holder<MobEffect>>, Instance> map = new HashMap<>();
                      for (Map.Entry<Holder<MobEffect>, Instance> entry : effects.entrySet())
                      {
                          map.put(Either.right(entry.getKey()), entry.getValue());
                      }
                      return map;
                  }).fieldOf("effects").forGetter(predicate -> predicate.effects)
    ).apply(instance, EffectsRequirement::new));

    public boolean test(Entity entity)
    {   return !(entity instanceof LivingEntity living) || test(living.getActiveEffects());
    }

    public boolean test(Collection<MobEffectInstance> effects)
    {
        for (Map.Entry<Holder<MobEffect>, Instance> entry : this.effects.entrySet())
        {
            MobEffect effect = entry.getKey().value();
            Instance instance = entry.getValue();
            int amplifier = 0;
            int duration = 0;
            boolean ambient = true;
            boolean visible = true;
            for (MobEffectInstance effectInstance : effects)
            {
                if (effectInstance.getEffect() == effect)
                {
                    amplifier = effectInstance.getAmplifier();
                    duration = effectInstance.getDuration();
                    ambient = effectInstance.isAmbient();
                    visible = effectInstance.isVisible();
                    break;
                }
            }
            if (!instance.amplifier().test(amplifier) || !instance.duration().test(duration))
            {   return false;
            }
            if (instance.ambient().isPresent() && instance.ambient().get() != ambient)
            {   return false;
            }
            if (instance.visible().isPresent() && instance.visible().get() != visible)
            {   return false;
            }
        }
        return true;
    }

    public boolean test(MobEffectInstance effect)
    {
        Instance instance = effects.get(effect.getEffect());
        if (instance == null)
        {   return true;
        }

        return instance.amplifier().test(effect.getAmplifier()) && instance.duration().test(effect.getDuration())
            && (instance.ambient().isEmpty() || instance.ambient().get() == effect.isAmbient())
            && (instance.visible().isEmpty() || instance.visible().get() == effect.isVisible());
    }

    public CompoundTag serialize()
    {   return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
    }

    public static EffectsRequirement deserialize(CompoundTag tag)
    {   return CODEC.decode(NbtOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalArgumentException("Could not deserialize EffectsRequirement")).getFirst();
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

        EffectsRequirement that = (EffectsRequirement) obj;

        return effects.equals(that.effects);
    }

    public record Instance(IntegerBounds amplifier, IntegerBounds duration, Optional<Boolean> ambient, Optional<Boolean> visible)
    {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                IntegerBounds.CODEC.fieldOf("amplifier").forGetter(effect -> effect.amplifier),
                IntegerBounds.CODEC.fieldOf("duration").forGetter(effect -> effect.duration),
                Codec.BOOL.optionalFieldOf("ambient").forGetter(effect -> effect.ambient),
                Codec.BOOL.optionalFieldOf("visible").forGetter(effect -> effect.visible)
        ).apply(instance, Instance::new));

        public CompoundTag serialize()
        {   return (CompoundTag) CODEC.encodeStart(NbtOps.INSTANCE, this).result().orElseGet(CompoundTag::new);
        }

        public static Instance deserialize(CompoundTag tag)
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

            Instance instance = (Instance) obj;

            if (!amplifier.equals(instance.amplifier))
            {   return false;
            }
            if (!duration.equals(instance.duration))
            {   return false;
            }
            if (!ambient.equals(instance.ambient))
            {   return false;
            }
            return visible.equals(instance.visible);
        }

        /*@Override
        public String toString()
        {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
        }*/
    }

    /*@Override
    public String toString()
    {   return CODEC.encodeStart(JsonOps.INSTANCE, this).result().map(Object::toString).orElse("serialize_failed");
    }*/
}
