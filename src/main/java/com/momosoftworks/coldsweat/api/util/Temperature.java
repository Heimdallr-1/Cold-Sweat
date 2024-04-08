package com.momosoftworks.coldsweat.api.util;

import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.event.common.GatherDefaultTempModifiersEvent;
import com.momosoftworks.coldsweat.api.event.common.TempModifierEvent;
import com.momosoftworks.coldsweat.api.registry.TempModifierRegistry;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.common.event.capability.EntityTempManager;
import com.momosoftworks.coldsweat.common.capability.temperature.ITemperatureCap;
import com.momosoftworks.coldsweat.common.capability.temperature.PlayerTempCap;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.core.network.message.TempModifiersSyncMessage;
import com.momosoftworks.coldsweat.core.network.message.TemperatureSyncMessage;
import com.momosoftworks.coldsweat.util.entity.DummyPlayer;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.InterruptableStreamer;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * General helper class for temperature-related actions. (Previously TempHelper)<br>
 * Allows for manipulating player temperature and TempModifiers.
 */
public class Temperature
{
    private Temperature() {}

    /**
     * Converts a double temperature to a different unit. If {@code from} and {@code to} are the same, returns {@code value}.<br>
     * @param value The temperature to convert.
     * @param from The unit to convert from.
     * @param to The unit to convert to.
     * @param absolute Used when dealing with world temperatures with Minecraft units.
     * @return The converted temperature.
     */
    public static double convertUnits(double value, Units from, Units to, boolean absolute)
    {
        return switch (from)
        {
            case C -> switch (to)
            {
                case C -> value;
                case F -> value * 1.8 + 32d;
                case MC -> value / 23.333333333d;
            };
            case F -> switch (to)
            {
                case C -> (value - 32) / 1.8;
                case F -> value;
                case MC -> (value - (absolute ? 32d : 0d)) / 42d;
            };
            case MC -> switch (to)
            {
                case C -> value * 23.333333333d;
                case F -> value * 42d + (absolute ? 32d : 0d);
                case MC -> value;
            };
        };
    }

    /**
     * Returns the player's temperature of the specified type.
     */
    public static double get(LivingEntity entity, Type type)
    {   return EntityTempManager.getTemperatureCap(entity).map(cap -> cap.getTemp(type)).orElse(0.0);
    }

    public static double get(LivingEntity entity, Ability type)
    {   return EntityTempManager.getTemperatureCap(entity).map(cap -> cap.getAbility(type)).orElse(0.0);
    }

    public static void set(LivingEntity entity, Type type, double value)
    {   EntityTempManager.getTemperatureCap(entity).orElse(new PlayerTempCap()).setTemp(type, value);
    }

    public static void set(LivingEntity entity, Ability type, double value)
    {   EntityTempManager.getTemperatureCap(entity).orElse(new PlayerTempCap()).setAbility(type, value);
    }

    public static void add(LivingEntity entity, double value, Type type)
    {
        EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
        {   cap.setTemp(type, value + cap.getTemp(type));
        });
    }

    /**
     * @return  a double representing what the Temperature would be after a TempModifier is applied.
     * @param entity the entity this modifier should use
     * @param modifiers the modifier(s) being applied to the {@code Temperature}
     */
    public static double apply(double temp, LivingEntity entity, Type type, TempModifier... modifiers)
    {
        double temp2 = temp;
        for (TempModifier modifier : modifiers)
        {
            if (modifier == null) continue;

            double newTemp = entity.tickCount % modifier.getTickRate() == 0 || modifier.getTicksExisted() == 0
                    ? modifier.update(temp2, entity, type)
                    : modifier.getResult(temp2);
            if (!Double.isNaN(newTemp))
            {   temp2 = newTemp;
            }
        }
        return temp2;
    }

    /**
     * @return a double representing what the temperature would be after a collection of TempModifier(s) are applied.
     * @param entity the entity this list of modifiers should use
     * @param modifiers the list of modifiers being applied to the player's temperature
     */
    public static double apply(double temp, LivingEntity entity, Type type, Collection<TempModifier> modifiers)
    {
        return apply(temp, entity, type, modifiers.toArray(new TempModifier[0]));
    }

    static Map<ResourceLocation, DummyPlayer> DUMMIES = new HashMap<>();
    public static double getTemperatureAt(BlockPos pos, Level level)
    {
        ResourceLocation dimension = level.dimension().location();
        // There is one "dummy" entity per world, which TempModifiers are applied to
        DummyPlayer dummy = DUMMIES.get(dimension);
        // If the dummy for this dimension is invalid, make a new one
        if (dummy == null || dummy.level != level)
        {   DUMMIES.put(dimension, dummy = new DummyPlayer(level));
            // Use default player modifiers to determine the temperature
            GatherDefaultTempModifiersEvent event = new GatherDefaultTempModifiersEvent(dummy, Type.WORLD);
            MinecraftForge.EVENT_BUS.post(event);
            addModifiers(dummy, event.getModifiers(), Type.WORLD, true);
        }
        // Move the dummy to the position being tested
        dummy.setPos(CSMath.getCenterPos(pos));
        return apply(0, dummy, Type.WORLD, getModifiers(dummy, Type.WORLD));
    }

    /**
     * @param modClass The class of the TempModifier to check for
     * @param type The type of TempModifier to check for
     * @return true if the player has a TempModifier that extends the given class
     */
    public static boolean hasModifier(LivingEntity entity, Type type, Class<? extends TempModifier> modClass)
    {
        return EntityTempManager.getTemperatureCap(entity).map(cap -> cap.hasModifier(type, modClass)).orElse(false);
    }

    /**
     * @return The first modifier of the given class that is applied to the player.
     */
    public static <T extends TempModifier> Optional<T> getModifier(LivingEntity entity, Type type, Class<T> modClass)
    {
        return getModifier(EntityTempManager.getTemperatureCap(entity).orElse(new PlayerTempCap()), type, modClass);
    }

    public static <T extends TempModifier> Optional<T> getModifier(ITemperatureCap cap, Type type, Class<T> modClass)
    {
        return (Optional<T>) cap.getModifiers(type).stream().filter(modClass::isInstance).findFirst();
    }

    /**
     * @return The first modifier applied to the player that fits the predicate.
     */
    @Nullable
    public static TempModifier getModifier(LivingEntity entity, Type type, Predicate<TempModifier> condition)
    {
        for (TempModifier modifier : EntityTempManager.getTemperatureCap(entity).orElse(new PlayerTempCap()).getModifiers(type))
        {
            if (condition.test(modifier))
            {
                return modifier;
            }
        }
        return null;
    }

    /**
     * Invokes addModifier() in a way that replaces the first occurrence of the modifier, if it exists.<br>
     * Otherwise, it will add the modifier.<br>
     * @param player The player to apply the modifier to
     * @param modifier The modifier to apply
     * @param type The type of temperature to apply the modifier to
     */
    public static void addOrReplaceModifier(Player player, TempModifier modifier, Type type)
    {
        addModifier(player, modifier, type, false, Addition.of(Addition.Mode.REPLACE_OR_ADD, Addition.Order.FIRST, mod -> modifier.getID().equals(mod.getID())));
    }

    /**
     * Invokes addModifier() in a way that replaces the first occurrence of the modifier, if it exists.<br>
     * It will not add the modifier if an existing instance of the same TempModifier class is not found.<br>
     * @param player The player to apply the modifier to
     * @param modifier The modifier to apply
     * @param type The type of temperature to apply the modifier to
     */
    public static void replaceModifier(Player player, TempModifier modifier, Type type)
    {
        addModifier(player, modifier, type, false, Addition.of(Addition.Mode.REPLACE, Addition.Order.FIRST, mod -> modifier.getID().equals(mod.getID())));
    }

    /**
     * Adds the given modifier to the player.<br>
     * If duplicates are disabled and the modifier already exists, this action will fail.
     * @param allowDupes allows or disallows duplicate TempModifiers to be applied
     * (You might use this for things that have stacking effects, for example)
     */
    public static void addModifier(LivingEntity entity, TempModifier modifier, Type type, boolean allowDupes)
    {
        addModifier(entity, modifier, type, allowDupes, Addition.AFTER_LAST);
    }

    public static void addModifier(LivingEntity entity, TempModifier modifier, Type type, boolean allowDupes, Addition params)
    {
        TempModifierEvent.Add event = new TempModifierEvent.Add(modifier, entity, type);
        MinecraftForge.EVENT_BUS.post(event);
        if (!event.isCanceled())
        {
            TempModifier newModifier = event.getModifier();
            if (TempModifierRegistry.getEntries().containsKey(newModifier.getID()))
            {
                EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
                {
                    List<TempModifier> modifiers = cap.getModifiers(event.type);
                    boolean changed = false;
                    try
                    {
                        Predicate<TempModifier> predicate = params.predicate();
                        if (predicate == null) predicate = mod -> true;

                        boolean replace = params.mode  == Addition.Mode.REPLACE || params.mode == Addition.Mode.REPLACE_OR_ADD;
                        boolean after   = params.mode  == Addition.Mode.AFTER;
                        boolean forward = params.order == Addition.Order.FIRST;

                        TempModifier newMod = event.getModifier();

                        if (!allowDupes && modifiers.stream().anyMatch(mod -> mod.getID().equals(newMod.getID())) && !replace)
                        {   return;
                        }
                        // Get the start of the iterator & which direction it's going
                        int start = forward ? 0 : (modifiers.size() - 1);
                        // Iterate through the list (backwards if "forward" is false)
                        for (int i = start; forward ? i < modifiers.size() : i >= 0; i += forward ? 1 : -1)
                        {
                            TempModifier mod = modifiers.get(i);
                            // If the predicate is true, inject the modifier at this position (or after it if "after" is true)
                            if (predicate.test(mod))
                            {
                                if (replace)
                                {   modifiers.set(i, newMod);
                                }
                                else
                                {   modifiers.add(i + (after ? 1 : 0), newMod);
                                }
                                changed = true;
                                return;
                            }
                        }
                        // Add the modifier if the insertion check fails
                        if (params.mode != Addition.Mode.REPLACE)
                        {   modifiers.add(newMod);
                            changed = true;
                        }
                    }
                    finally
                    {   if (changed) updateModifiers(entity, cap);
                    }
                });
            }
            else
            {   ColdSweat.LOGGER.error("Tried to reference invalid TempModifier with ID \"" + modifier.getID() + "\"! Is it not registered?");
            }
        }
    }

    public static void addModifiers(LivingEntity entity, List<TempModifier> modifiers, Type type, boolean duplicates)
    {
        EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
        {
            List<TempModifier> list = cap.getModifiers(type);
            for (TempModifier modifier : modifiers)
            {
                if (duplicates || list.stream().noneMatch(mod -> mod.getID().equals(modifier.getID())))
                    cap.getModifiers(type).add(modifier);
            }
            updateModifiers(entity, cap);
        });
    }

    /**
     * Removes the specified number of TempModifiers of the specified type from the player
     * @param entity The entity being sampled
     * @param type Determines which TempModifier list to pull from
     * @param count The number of modifiers of the given type to be removed (can be higher than the number of modifiers on the player)
     * @param condition The predicate to determine which TempModifiers to remove
     */
    public static void removeModifiers(LivingEntity entity, Type type, int count, Predicate<TempModifier> condition)
    {
        AtomicInteger removed = new AtomicInteger(0);

        EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
        {
            cap.getModifiers(type).removeIf(modifier ->
            {
                if (removed.get() < count)
                {
                    TempModifierEvent.Remove event = new TempModifierEvent.Remove(entity, type, count, condition);
                    MinecraftForge.EVENT_BUS.post(event);
                    if (!event.isCanceled())
                    {
                        if (event.getCondition().test(modifier))
                        {
                            removed.incrementAndGet();
                            return true;
                        }
                    }
                    return false;
                }
                return false;
            });

            // Update modifiers if anything actually changed
            if (removed.get() > 0)
                updateModifiers(entity, cap);
        });
    }

    public static void removeModifiers(LivingEntity entity, Type type, Predicate<TempModifier> condition)
    {
        removeModifiers(entity, type, Integer.MAX_VALUE, condition);
    }

    /**
     * Gets all TempModifiers of the specified type on the player
     * @param entity is the entity being sampled
     * @param type determines which TempModifier list to pull from
     * @return a NEW list of all TempModifiers of the specified type
     */
    public static List<TempModifier> getModifiers(LivingEntity entity, Type type)
    {
        return EntityTempManager.getTemperatureCap(entity).map(cap -> cap.getModifiers(type)).orElse(List.of());
    }

    /**
     * Iterates through all TempModifiers of the specified type on the player
     * @param type determines which TempModifier list to pull from
     * @param action the action(s) to perform on each TempModifier
     */
    public static void forEachModifier(LivingEntity entity, Type type, Consumer<TempModifier> action)
    {
        EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
        {
            if (cap.getModifiers(type) != null)
            {
                cap.getModifiers(type).forEach(action);
            }
        });
    }

    public static void forEachModifier(LivingEntity entity, Type type, BiConsumer<TempModifier, InterruptableStreamer<TempModifier>> action)
    {
        EntityTempManager.getTemperatureCap(entity).ifPresent(cap ->
        {
            if (cap.getModifiers(type) != null)
            {
                CSMath.breakableForEach(cap.getModifiers(type), action);
            }
        });
    }

    public static void updateTemperature(LivingEntity entity, ITemperatureCap cap, boolean instant)
    {
        if (!entity.level.isClientSide)
        {
            ColdSweatPacketHandler.INSTANCE.send(entity instanceof ServerPlayer player
                            ? PacketDistributor.PLAYER.with(() -> player)
                            : PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
            new TemperatureSyncMessage(entity, cap.serializeTemps().merge(cap.serializeAbilities()), instant));
        }
    }

    public static void updateModifiers(LivingEntity entity, ITemperatureCap cap)
    {
        if (!entity.level.isClientSide)
        {
            ColdSweatPacketHandler.INSTANCE.send(entity instanceof ServerPlayer player
                            ? PacketDistributor.PLAYER.with(() -> player)
                            : PacketDistributor.TRACKING_ENTITY.with(() -> entity),
            new TempModifiersSyncMessage(entity, cap.serializeModifiers()));
        }
    }

    public static Map<Type, Double> getTemperatures(LivingEntity entity)
    {   return EntityTempManager.getTemperatureCap(entity).map(ITemperatureCap::getTemperatures).orElse(new EnumMap<>(Type.class));
    }

    /**
     * Defines all temperature stats in Cold Sweat. <br>
     * These are used to get temperature stored on the player and/or to apply modifiers to it. <br>
     * <br>
     * {@link #WORLD}: The temperature of the area around the player. Should ONLY be changed by TempModifiers. <br>
     * <br>
     * {@link #CORE}: The core temperature of the player (This is what "body" temperature typically refers to). <br>
     * {@link #BASE}: A static offset applied to the player's core temperature. <br>
     * {@link #BODY}: The sum of the player's core and base temperatures. (CANNOT be set) <br>
     * {@link #RATE}: Only used by TempModifiers. Affects the rate at which the player's body temperature changes. <br>
     */
    public enum Type implements StringRepresentable
    {
        WORLD("world"),
        CORE("core"),
        BASE("base"),
        BODY("body"),
        RATE("rate");

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values, Type::fromID);

        private final String id;

        Type(String id)
        {   this.id = id;
        }

        public static Type fromID(String id)
        {
            for (Type type : values())
            {
                if (type.getSerializedName().equals(id))
                    return type;
            }
            return null;
        }

        @Override
        public String getSerializedName()
        {   return id;
        }
    }

    /**
     * Defines all extra temperature-related abilities in Cold Sweat. <br>
     * These are different from {@link Temperature.Type} because they do not get TempModifiers. These are mainly used for third-party stuff.
     * These are used to get temperature stored on the player and/or to apply modifiers to it. <br>
     * <br>
     * {@link #FREEZING_POINT}: An offset to the max temperature threshold, after which a player's body temperature starts rising. <br>
     * {@link #BURNING_POINT}: An offset to the min temperature threshold, after which a player's body temperature starts falling. <br>
     * {@link #COLD_RESISTANCE}: Resistance to cold temperature-related damage. <br>
     * {@link #HEAT_RESISTANCE}: Resistance to heat temperature-related damage. <br>
     * {@link #COLD_DAMPENING}: Changes the rate of body temperature increase. <br>
     * {@link #HEAT_DAMPENING}: Changes the rate of body temperature decrease. <br>
     */
    public enum Ability implements StringRepresentable
    {
        FREEZING_POINT("freezing_point"),
        BURNING_POINT("burning_point"),
        COLD_RESISTANCE("cold_resistance"),
        HEAT_RESISTANCE("heat_resistance"),
        COLD_DAMPENING("cold_dampening"),
        HEAT_DAMPENING("heat_dampening");

        public static final Codec<Ability> CODEC = StringRepresentable.fromEnum(Ability::values, Ability::fromID);

        private final String id;

        Ability(String id)
        {   this.id = id;
        }

        public static Ability fromID(String id)
        {
            for (Ability ability : values())
            {
                if (ability.getSerializedName().equals(id))
                    return ability;
            }
            return null;
        }

        @Override
        public String getSerializedName()
        {   return id;
        }
    }

    /**
     * Units of measurement used by Cold Sweat.<br>
     * Most calculations are done in MC units, then converted to C or F when they are displayed.<br>
     */
    public enum Units implements StringRepresentable
    {
        F("°F", "f"),
        C("°C", "c"),
        MC("MC", "mc");

        public static final Codec<Units> CODEC = StringRepresentable.fromEnum(Units::values, Units::fromID);

        private final String name;
        private final String id;

        Units(String name, String id)
        {   this.name = name;
            this.id = id;
        }

        public static Units fromID(String id)
        {
            for (Units unit : values())
            {
                if (unit.getSerializedName().equals(id))
                    return unit;
            }
            return null;
        }

        public String getFormattedName()
        {   return name;
        }

        @Override
        public String getSerializedName()
        {   return id;
        }
    }

    public record Addition(Mode mode, Order order, Predicate<TempModifier> predicate)
    {
        public static final Addition AFTER_LAST = Addition.of(Mode.AFTER, Order.LAST, mod -> true);
        public static final Addition BEFORE_FIRST = Addition.of(Mode.BEFORE, Order.FIRST, mod -> true);

        public static Addition of(Mode mode, Order order, Predicate<TempModifier> predicate)
        {   return new Addition(mode, order, predicate);
        }

        public enum Mode
        {
            // Inserts the new modifier before the targeted modifier's position
            BEFORE,
            // Inserts the new modifier after the targeted modifier's position
            AFTER,
            // Replace the desired instance of the modifier (fails if no modifiers pass the predicate)
            REPLACE,
            // Replace the desired instance of the modifier if it exists, otherwise add it to the end
            REPLACE_OR_ADD
        }

        public enum Order
        {
            // Targets the first modifier that passes the predicate
            FIRST,
            // Targets the last modifier that passes the predicate
            LAST
        }
    }
}
