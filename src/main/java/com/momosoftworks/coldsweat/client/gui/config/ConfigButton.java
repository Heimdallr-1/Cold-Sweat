package com.momosoftworks.coldsweat.client.gui.config;

import com.momosoftworks.coldsweat.client.gui.config.pages.ConfigPageOne;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class ConfigButton extends Button
{
    public ConfigButton(int x, int y, int width, int height, IFormattableTextComponent title, Button.IPressable pressedAction)
    {
        super(x, y, width, height, title, pressedAction);
    }

    public boolean setsCustomDifficulty() {
        return true;
    }

    @Override
    public void onPress()
    {
        if (setsCustomDifficulty())
        {
            ConfigSettings.DIFFICULTY.set(ConfigSettings.Difficulty.CUSTOM);

            if (Minecraft.getInstance().screen instanceof ConfigPageOne)
            {
                ConfigPageOne page = (ConfigPageOne) Minecraft.getInstance().screen;
                ((Button) page.getWidgetBatch("difficulty").get(0)).setMessage(
                        new StringTextComponent(new TranslationTextComponent("cold_sweat.config.difficulty.name").getString() +
                                " (" + ConfigSettings.Difficulty.getFormattedName(ConfigSettings.DIFFICULTY.get()).getString() + ")..."));
            }
        }

        super.onPress();
    }
}
