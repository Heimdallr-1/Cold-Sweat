package com.momosoftworks.coldsweat.api.event.core;

import com.momosoftworks.coldsweat.api.registry.TempModifierRegistry;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

/**
 * Builds the {@link TempModifierRegistry}. <br>
 * The event is fired during {@link net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent}. <br>
 * <br>
 * This event is NOT {@link net.minecraftforge.eventbus.api.Cancelable}. <br>
 * <br>
 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
 */
public class TempModifierRegisterEvent extends Event
{
    /**
     * Adds a new {@link TempModifier} to the registry.
     *
     * @param modifier the {@link TempModifier} to add.
     */
    public void register(ResourceLocation id, Supplier<TempModifier> modifier)
    {   TempModifierRegistry.register(id, modifier);
    }

    //TODO Remove soon
    /**
     * TempModifier IDs are no longer defined in the class. Register using a ResourceLocation instead.
     */
    @Deprecated(since = "2.3", forRemoval = true)
    public void register(Supplier<TempModifier> modifier)
    {
        if (SharedConstants.IS_RUNNING_IN_IDE)
        {   throw new RuntimeException("TempModifier IDs are no longer defined in the class. Register using a ResourceLocation instead.");
        }
        register(new ResourceLocation(modifier.get().getID()), modifier);
    }

    /**
     * A way of indirectly registering TempModifiers by class name.<br>
     * Useful for adding compat for other mods, where loading the TempModifier's class directly would cause an error.<br>
     * The class must have a no-arg constructor for this to work.
     */
    public void registerByClassName(ResourceLocation id, String className)
    {
        try
        {
            Constructor<?> clazz = Class.forName(className).getConstructor();
            this.register(id, () -> {
                try
                {
                    return (TempModifier) clazz.newInstance();
                } catch (Exception e)
                {   throw new RuntimeException(e);
                }
            });
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
