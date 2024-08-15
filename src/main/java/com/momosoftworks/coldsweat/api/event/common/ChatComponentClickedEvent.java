package com.momosoftworks.coldsweat.api.event.common;

import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

import javax.annotation.Nullable;

public class ChatComponentClickedEvent extends Event
{
    private final Style style;
    private final Player player;

    public ChatComponentClickedEvent(@Nullable Style style, Player player)
    {   this.style = style != null ? style : Style.EMPTY;
        this.player = player;
    }

    public Style getStyle()
    {   return style;
    }

    public Player getPlayer()
    {   return player;
    }
}
