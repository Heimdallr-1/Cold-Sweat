package com.momosoftworks.coldsweat.data.codec.requirement.sub_type;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.requirement.EntityRequirement;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.entity.EntityHelper;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public record PlayerDataRequirement(IntegerBounds level, Optional<GameType> gameType, Optional<Map<StatRequirement, IntegerBounds>> stats,
                                    Optional<Map<ResourceLocation, Boolean>> recipes,
                                    Optional<Map<ResourceLocation, Either<AdvancementCompletionRequirement, AdvancementCriteriaRequirement>>> advancements,
                                    Optional<EntityRequirement> lookingAt) implements EntitySubRequirement
{
    public static MapCodec<PlayerDataRequirement> getCodec()
    {   return getCodec(EntityRequirement.getCodec());
    }

    public static MapCodec<PlayerDataRequirement> getCodec(Codec<EntityRequirement> lastCodec)
    {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                IntegerBounds.CODEC.optionalFieldOf("level", IntegerBounds.NONE).forGetter(requirement -> requirement.level),
                GameType.CODEC.optionalFieldOf("gamemode").forGetter(requirement -> requirement.gameType),
                Codec.unboundedMap(StatRequirement.CODEC, IntegerBounds.CODEC).optionalFieldOf("stats").forGetter(requirement -> requirement.stats),
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.BOOL).optionalFieldOf("recipes").forGetter(requirement -> requirement.recipes),
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.either(AdvancementCompletionRequirement.CODEC, AdvancementCriteriaRequirement.CODEC)).optionalFieldOf("advancements").forGetter(requirement -> requirement.advancements),
                lastCodec.optionalFieldOf("looking_at").forGetter(requirement -> requirement.lookingAt)
        ).apply(instance, PlayerDataRequirement::new));
    }

    public boolean test(Entity entity, Level level, Vec3 position)
    {
        if (!(entity instanceof Player player)) return false;
        ServerPlayer serverPlayer = EntityHelper.getServerPlayer(player);

        if (!this.level.test(serverPlayer.experienceLevel))
        {   return false;
        }
        if (gameType.isPresent() && serverPlayer.gameMode.getGameModeForPlayer() != gameType.get())
        {   return false;
        }
        if (stats.isPresent())
        {
            for (Map.Entry<StatRequirement, IntegerBounds> entry : stats.get().entrySet())
            {
                int value = serverPlayer.getStats().getValue(entry.getKey().stat());
                if (!entry.getKey().test(entry.getKey().stat(), value))
                {   return false;
                }
            }
        }
        if (recipes.isPresent())
        {
            for (Map.Entry<ResourceLocation, Boolean> entry : recipes.get().entrySet())
            {
                if (serverPlayer.getRecipeBook().contains(entry.getKey()) != entry.getValue())
                {   return false;
                }
            }
        }
        if (advancements.isPresent())
        {
            for (Map.Entry<ResourceLocation, Either<AdvancementCompletionRequirement, AdvancementCriteriaRequirement>> entry : advancements.get().entrySet())
            {
                AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(serverPlayer.getServer().getAdvancements().get(entry.getKey()));
                if (entry.getValue().map(complete -> complete.test(progress), criteria -> criteria.test(progress)))
                {   return false;
                }
            }
        }
        if (lookingAt.isPresent())
        {
            Vec3 vec3 = player.getEyePosition();
            Vec3 vec31 = player.getViewVector(1.0F);
            Vec3 vec32 = vec3.add(vec31.x * 100.0D, vec31.y * 100.0D, vec31.z * 100.0D);
            EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(player.level(), player, vec3, vec32, (new AABB(vec3, vec32)).inflate(1.0D), (ent) -> !ent.isSpectator(), 0.0F);
            if (entityhitresult == null || entityhitresult.getType() != HitResult.Type.ENTITY)
            {   return false;
            }

            Entity hitEntity = entityhitresult.getEntity();
            if (!this.lookingAt.get().test(hitEntity) || !player.hasLineOfSight(hitEntity))
            {   return false;
            }
        }
        return true;
    }

    public CompoundTag serialize()
    {   return (CompoundTag) getCodec().codec().encodeStart(NbtOps.INSTANCE, this).result().orElse(new CompoundTag());
    }

    public static PlayerDataRequirement deserialize(CompoundTag tag)
    {   return getCodec().codec().decode(NbtOps.INSTANCE, tag).result().orElseThrow(() -> new IllegalArgumentException("Could not deserialize PlayerDataRequirement")).getFirst();
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

        PlayerDataRequirement that = (PlayerDataRequirement) obj;

        if (!gameType.equals(that.gameType))
        {   return false;
        }
        if (!stats.equals(that.stats))
        {   return false;
        }
        if (!recipes.equals(that.recipes))
        {   return false;
        }
        if (!advancements.equals(that.advancements))
        {   return false;
        }
        return lookingAt.equals(that.lookingAt);
    }

    public static class StatRequirement
    {
        private final StatType<?> type;
        private ResourceLocation statId;
        private final Stat<?> stat;
        private final IntegerBounds value;

        public static final Codec<StatRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.STAT_TYPE.byNameCodec().fieldOf("type").forGetter(stat -> stat.type),
                ResourceLocation.CODEC.fieldOf("stat").forGetter(stat -> stat.statId),
                IntegerBounds.CODEC.fieldOf("value").forGetter(stat -> stat.value)
        ).apply(instance, StatRequirement::new));

        public StatRequirement(StatType<?> type, ResourceLocation statId, IntegerBounds value)
        {   this(type, (Stat<?>) type.getRegistry().get(statId), value);
        }

        public StatRequirement(StatType<?> type, Stat<?> stat, IntegerBounds value)
        {   this.type = type;
            this.stat = stat;
            this.value = value;
        }

        public StatType<?> type()
        {   return this.type;
        }
        public Stat<?> stat()
        {   return this.stat;
        }
        public IntegerBounds value()
        {   return this.value;
        }

        public boolean test(Stat<?> stat, int value)
        {   return this.stat.equals(stat) && this.value.test(value);
        }

        public CompoundTag serialize()
        {   CompoundTag tag = new CompoundTag();
            tag.putString("type", BuiltInRegistries.STAT_TYPE.getKey(type).toString());
            tag.putString("stat", serializeStat(stat));
            tag.put("value", this.value.serialize());
            return tag;
        }

        public static StatRequirement deserialize(CompoundTag tag)
        {   StatType<?> type = BuiltInRegistries.STAT_TYPE.get(ResourceLocation.parse(tag.getString("type")));
            Stat<?> stat = deserializeStat(type, tag);
            IntegerBounds value = IntegerBounds.deserialize(tag.getCompound("value"));
            return new StatRequirement(type, stat, value);
        }

        private static <T> String serializeStat(Stat<T> stat)
        {   return stat.getType().getRegistry().getKey(stat.getValue()).toString();
        }

        private static <T> Stat<T> deserializeStat(StatType<T> type, CompoundTag tag)
        {   return type.get(type.getRegistry().get(ResourceLocation.parse(tag.getString("stat"))));
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

            StatRequirement that = (StatRequirement) obj;

            if (!type.equals(that.type))
            {   return false;
            }
            if (!statId.equals(that.statId))
            {   return false;
            }
            if (!stat.equals(that.stat))
            {   return false;
            }
            return value.equals(that.value);
        }

        @Override
        public String toString()
        {
            return "Stat{" +
                    "type=" + type +
                    ", statId=" + statId +
                    ", stat=" + stat +
                    ", value=" + value +
                    '}';
        }
    }

    public record AdvancementCompletionRequirement(Boolean complete)
    {
        public static final Codec<AdvancementCompletionRequirement> CODEC = Codec.BOOL.xmap(AdvancementCompletionRequirement::new, AdvancementCompletionRequirement::complete);

        public boolean test(AdvancementProgress progress)
        {   return progress.isDone() == this.complete;
        }

        public CompoundTag serialize()
        {   CompoundTag tag = new CompoundTag();
            tag.putBoolean("completion", complete);
            return tag;
        }

        public static AdvancementCompletionRequirement deserialize(CompoundTag tag)
        {   return new AdvancementCompletionRequirement(tag.getBoolean("completion"));
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

            AdvancementCompletionRequirement that = (AdvancementCompletionRequirement) obj;

            return complete.equals(that.complete);

        }

        @Override
        public String toString()
        {
            return "Completion{" +
                    "complete=" + complete +
                    '}';
        }
    }
    public record AdvancementCriteriaRequirement(Map<String, Boolean> criteria)
    {
        public static final Codec<AdvancementCriteriaRequirement> CODEC = Codec.unboundedMap(Codec.STRING, Codec.BOOL).xmap(AdvancementCriteriaRequirement::new, AdvancementCriteriaRequirement::criteria);

        public boolean test(AdvancementProgress progress)
        {
            for (Map.Entry<String, Boolean> entry : this.criteria.entrySet())
            {
                CriterionProgress criterionprogress = progress.getCriterion(entry.getKey());
                if (criterionprogress == null || criterionprogress.isDone() != entry.getValue())
                {   return false;
                }
            }
            return true;
        }

        public CompoundTag serialize()
        {   CompoundTag tag = new CompoundTag();
            for (Map.Entry<String, Boolean> entry : this.criteria.entrySet())
            {   tag.putBoolean(entry.getKey(), entry.getValue());
            }
            return tag;
        }

        public static AdvancementCriteriaRequirement deserialize(CompoundTag tag)
        {   return new AdvancementCriteriaRequirement(tag.getAllKeys().stream().collect(Collectors.toMap(key -> key, key -> tag.getBoolean(key))));
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

            AdvancementCriteriaRequirement that = (AdvancementCriteriaRequirement) obj;

            return criteria.equals(that.criteria);
        }

        @Override
        public String toString()
        {
            return "Criteria{" +
                    "criteria=" + criteria +
                    '}';
        }
    }

    @Override
    public String toString()
    {
        return "PlayerData{" +
                "gameType=" + gameType +
                ", stats=" + stats +
                ", recipes=" + recipes +
                ", advancements=" + advancements +
                ", lookingAt=" + lookingAt +
                '}';
    }
}