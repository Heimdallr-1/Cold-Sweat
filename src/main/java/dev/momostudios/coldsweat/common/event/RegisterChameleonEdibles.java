package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.api.event.core.EdiblesRegisterEvent;
import dev.momostudios.coldsweat.common.entity.data.edible.*;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class RegisterChameleonEdibles
{
    @SubscribeEvent
    public static void onWorldLoaded(LevelEvent.Load event)
    {
        if (event.getLevel().isClientSide()) return;

        EdiblesRegisterEvent edibleEvent = new EdiblesRegisterEvent();
        MinecraftForge.EVENT_BUS.post(edibleEvent);
    }

    @SubscribeEvent
    public static void onEdiblesRegister(EdiblesRegisterEvent event)
    {
        event.registerEdible(new HotBiomeEdible());
        event.registerEdible(new ColdBiomeEdible());
        event.registerEdible(new HumidBiomeEdible());
        event.registerEdible(new HealingEdible());
        event.registerEdible(new HealingEdible());
    }
}
