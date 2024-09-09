package com.momosoftworks.coldsweat.client.event;

import com.momosoftworks.coldsweat.client.gui.config.ConfigScreen;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.core.event.TaskScheduler;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.core.network.message.ClientConfigAskMessage;
import com.momosoftworks.coldsweat.util.ClientOnlyHelper;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.math.Vec2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.OptionsScreen;
import net.minecraft.client.gui.widget.button.AbstractButton;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.gui.widget.Slider;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class DrawConfigButton
{
    public static boolean EDIT_MODE = false;

    @SubscribeEvent
    public static void eventHandler(GuiScreenEvent.InitGuiEvent event)
    {
        if (event.getGui() instanceof OptionsScreen && ConfigSettings.SHOW_CONFIG_BUTTON.get())
        {
            // The offset from the config
            Supplier<Vec2i> buttonPos = () -> ConfigSettings.CONFIG_BUTTON_POS.get();
            AtomicInteger xOffset = new AtomicInteger(buttonPos.get().x());
            AtomicInteger yOffset = new AtomicInteger(buttonPos.get().y());
            int buttonX = event.getGui().width / 2 - 183;
            int buttonY = event.getGui().height / 6 + 110;
            int screenWidth = event.getGui().width;
            int screenHeight = event.getGui().height;

            if (xOffset.get() + buttonX < -1 || yOffset.get() + buttonY < -1)
            {   xOffset.set(0);
                yOffset.set(0);
                ConfigSettings.CONFIG_BUTTON_POS.set(new Vec2i(0, 0));
            }

            // Main config button
            ImageButton mainButton = new ImageButton(buttonX + xOffset.get(), buttonY + yOffset.get(),
                                     24, 24, 40, 40, 24,
                                     new ResourceLocation("cold_sweat:textures/gui/screen/config_gui.png"),
                                     button ->
                                     {
                                         ConfigScreen.CURRENT_PAGE = 0;
                                         if (Minecraft.getInstance().player != null)
                                         {   ColdSweatPacketHandler.INSTANCE.sendToServer(new ClientConfigAskMessage(Minecraft.getInstance().player.getUUID()));
                                         }
                                     });

            if (Minecraft.getInstance().level == null)
            {
                Field onTooltip = ObfuscationReflectionHelper.findField(Button.class, "field_238487_u_");
                onTooltip.setAccessible(true);
                mainButton.active = false;
                try
                {
                    onTooltip.set(mainButton, (Button.ITooltip) (button, poseStack, mouseX, mouseY) ->
                    {   event.getGui().renderTooltip(poseStack, new TranslationTextComponent("tooltip.cold_sweat.config.must_be_in_game").withStyle(TextFormatting.RED), mouseX, mouseY);
                    });
                }
                catch (Exception ignored) {}
            }

            // Add main button
            event.addWidget(mainButton);

            // Reconfigure the options screen with controls for the position of the config button
            if (EDIT_MODE)
            {
                int buttonStartX = event.getGui().width / 2 - 183;
                int buttonStartY = event.getGui().height / 6 + 110;
                Runnable saveAndClamp = () ->
                {   xOffset.set(CSMath.clamp(xOffset.get(), -buttonStartX, screenWidth - mainButton.getWidth() - buttonStartX));
                    yOffset.set(CSMath.clamp(yOffset.get(), -buttonStartY, screenHeight - mainButton.getHeight() - buttonStartY));
                    mainButton.setPosition(buttonStartX + xOffset.get(), buttonStartY + yOffset.get());
                    ConfigSettings.CONFIG_BUTTON_POS.set(new Vec2i(xOffset.get(), yOffset.get()));
                };

                AtomicReference<AbstractButton> doneButtonAtomic = new AtomicReference<>(null);
                // Disable all other buttons
                event.getGui().children().forEach(child ->
                {
                    // Don't disable "Done" button
                    if (child instanceof AbstractButton)
                    {
                        AbstractButton button = (AbstractButton) child;
                        boolean isDoneButton = button.getMessage().getString().equals(DialogTexts.GUI_DONE.getString());
                        if (!isDoneButton)
                        {   button.active = false;
                        }
                        else
                        {   doneButtonAtomic.set(button);
                            button.setWidth(button.getWidth() - 72);
                        }
                    }
                    if (child instanceof Slider)
                    {   ((Slider) child).active = false;
                    }
                });

                if (doneButtonAtomic.get() == null) return;
                AbstractButton doneButton = doneButtonAtomic.get();

                // Create "left" button
                ImageButton leftButton = new ImageButton(doneButton.x + doneButton.getWidth() + 2, doneButton.y, 14, 20, 0, 0, 20,
                                     new ResourceLocation("cold_sweat:textures/gui/screen/config_gui.png"),
                                     button ->
                                     {   xOffset.set(xOffset.get() - ConfigScreen.SHIFT_AMOUNT.get());
                                         saveAndClamp.run();
                                     });
                // Add left button
                event.addWidget(leftButton);

                // Create "up" button
                ImageButton upButton = new ImageButton(leftButton.x + leftButton.getWidth(), leftButton.y, 20, 10, 14, 0, 20,
                                     new ResourceLocation("cold_sweat:textures/gui/screen/config_gui.png"),
                                     button ->
                                     {   yOffset.set(yOffset.get() - ConfigScreen.SHIFT_AMOUNT.get());
                                         saveAndClamp.run();
                                     });
                // Add up button
                event.addWidget(upButton);

                // Create "down" button
                ImageButton downButton = new ImageButton(upButton.x, upButton.y + upButton.getHeight(), 20, 10, 14, 10, 20,
                                     new ResourceLocation("cold_sweat:textures/gui/screen/config_gui.png"),
                                     button ->
                                     {   yOffset.set(yOffset.get() + ConfigScreen.SHIFT_AMOUNT.get());
                                         saveAndClamp.run();
                                     });
                // Add down button
                event.addWidget(downButton);

                // Create "right" button
                ImageButton rightButton = new ImageButton(upButton.x + upButton.getWidth(), upButton.y, 14, 20, 34, 0, 20,
                                     new ResourceLocation("cold_sweat:textures/gui/screen/config_gui.png"),
                                     button ->
                                     {   xOffset.set(xOffset.get() + ConfigScreen.SHIFT_AMOUNT.get());
                                         saveAndClamp.run();
                                     });
                // Add right button
                event.addWidget(rightButton);

                // Create "reset" button
                ImageButton resetButton = new ImageButton(rightButton.x + rightButton.getWidth() + 2, rightButton.y, 20, 20, 48, 0, 20,
                                     new ResourceLocation("cold_sweat:textures/gui/screen/config_gui.png"),
                                     button ->
                                     {   xOffset.set(0);
                                         yOffset.set(0);
                                         saveAndClamp.run();
                                     });
                // Add reset button
                event.addWidget(resetButton);

                TaskScheduler.scheduleClient(() -> EDIT_MODE = false, 1);
            }
        }
    }
}
