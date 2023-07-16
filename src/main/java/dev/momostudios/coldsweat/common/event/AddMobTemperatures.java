package dev.momostudios.coldsweat.common.event;

import dev.momostudios.coldsweat.api.event.common.EnableTemperatureEvent;
import dev.momostudios.coldsweat.common.entity.Chameleon;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class AddMobTemperatures
{
    @SubscribeEvent
    public static void onEnableTemperatureEvent(EnableTemperatureEvent event)
    {
        if (event.getEntity() instanceof Chameleon) event.setEnabled(true);
    }
}
