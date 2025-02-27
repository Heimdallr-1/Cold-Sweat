package com.momosoftworks.coldsweat.api.event.core.init;

import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

/**
 * Called during startup to enable temperature for every entity type.<br>
 * This even is {@link Cancelable} and is called on the Mod event bus. <br>
 * Cancelling this event will prevent temperature from being enabled for the entity type if it is already enabled.
 */
@Cancelable
public class EnableTemperatureEvent extends Event implements IModBusEvent
{
    final EntityType<?> entityType;
    boolean enabled = false;

    public EnableTemperatureEvent(EntityType<?> entityType)
    {   this.entityType = entityType;
    }

    public EntityType<?> getEntityType()
    {   return entityType;
    }

    public void setEnabled(boolean enabled)
    {   this.enabled = enabled;
    }

    public boolean isEnabled()
    {   return enabled;
    }
}
