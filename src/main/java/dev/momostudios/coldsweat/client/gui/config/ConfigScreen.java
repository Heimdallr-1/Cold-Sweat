package dev.momostudios.coldsweat.client.gui.config;

import dev.momostudios.coldsweat.client.gui.config.pages.ConfigPageDifficulty;
import dev.momostudios.coldsweat.client.gui.config.pages.ConfigPageOne;
import dev.momostudios.coldsweat.client.gui.config.pages.ConfigPageTwo;
import dev.momostudios.coldsweat.core.network.message.SyncConfigSettingsMessage;
import dev.momostudios.coldsweat.config.ConfigSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ConfigScreen
{
    public static final int TITLE_HEIGHT = 16;
    public static final int BOTTOM_BUTTON_HEIGHT_OFFSET = 26;
    public static final int OPTION_SIZE = 25;
    public static final int BOTTOM_BUTTON_WIDTH = 150;

    public static Minecraft MC = Minecraft.getInstance();

    public static DecimalFormat TWO_PLACES = new DecimalFormat("#.##");

    public static boolean IS_MOUSE_DOWN = false;
    public static int MOUSE_X = 0;
    public static int MOUSE_Y = 0;

    static List<Class<? extends AbstractConfigPage>> PAGES = Arrays.asList(ConfigPageOne.class, ConfigPageTwo.class);
    public static int FIRST_PAGE = 0;
    public static int LAST_PAGE = PAGES.size() - 1;

    public static Screen getPage(int index, Screen parentScreen)
    {
        index = Math.max(FIRST_PAGE, Math.min(LAST_PAGE, index));
        try
        {
            return PAGES.get(index).getConstructor(Screen.class).newInstance(parentScreen);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveConfig()
    {
        if (Minecraft.getInstance().player != null)
        {
            if (Minecraft.getInstance().player.getPermissionLevel() >= 2)
            {
                if (!MC.isLocalServer())
                    ColdSweatPacketHandler.INSTANCE.sendToServer(new SyncConfigSettingsMessage());
                else
                    ConfigSettings.saveValues();
            }
        }
        else
            ConfigSettings.saveValues();
    }

    @SubscribeEvent
    public static void onClicked(ScreenEvent.MouseButtonPressed event)
    {
        if (event.getButton() == 0 && Minecraft.getInstance().screen instanceof ConfigPageDifficulty)
            IS_MOUSE_DOWN = true;
    }

    @SubscribeEvent
    public static void onReleased(ScreenEvent.MouseButtonReleased event)
    {
        if (event.getButton() == 0 && Minecraft.getInstance().screen instanceof ConfigPageDifficulty)
            IS_MOUSE_DOWN = false;
    }

    public static String difficultyName(int difficulty)
    {
        return  difficulty == 0 ? Component.translatable("cold_sweat.config.difficulty.super_easy.name").getString() :
                difficulty == 1 ? Component.translatable("cold_sweat.config.difficulty.easy.name").getString() :
                difficulty == 2 ? Component.translatable("cold_sweat.config.difficulty.normal.name").getString() :
                difficulty == 3 ? Component.translatable("cold_sweat.config.difficulty.hard.name").getString() :
                difficulty == 4 ? Component.translatable("cold_sweat.config.difficulty.custom.name").getString() : "";
    }

    public static int difficultyColor(int difficulty)
    {
        return  difficulty == 0 ? 16777215 :
                difficulty == 1 ? 16768882 :
                difficulty == 2 ? 16755024 :
                difficulty == 3 ? 16731202 :
                difficulty == 4 ? 10631158 : 16777215;
    }

    public static int textOptionColor()
    {
        return Minecraft.getInstance().player == null || Minecraft.getInstance().player.getPermissionLevel() > 2 ? 16777215 : 8421504;
    }
}
