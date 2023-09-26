package dev.momostudios.coldsweat.api.event.core;

import dev.momostudios.coldsweat.api.registry.BlockTempRegistry;
import dev.momostudios.coldsweat.api.temperature.block_temp.BlockTemp;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when the {@link BlockTemp} registry is being built ({@link BlockTempRegistry}). <br>
 * The event is fired during {@link net.minecraftforge.event.world.WorldEvent.Load}. <br>
 * <br>
 * Use {@code BlockTempRegistry.flush()} if calling manually to prevent duplicates. <br>
 * (You probably shouldn't ever do that anyway) <br>
 * <br>
 * This event is not {@link net.minecraftforge.eventbus.api.Cancelable}. <br>
 * <br>
 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
 */
public class BlockTempRegisterEvent extends Event
{
    /**
     * Adds a new {@link BlockTemp} to the registry.
     *
     * @param blockTemp The BlockTemp to add.
     */
    public void register(BlockTemp blockTemp)
    {
        BlockTempRegistry.register(blockTemp);
    }
}
