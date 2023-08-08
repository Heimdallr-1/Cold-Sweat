package dev.momostudios.coldsweat.client.event;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.api.util.Temperature;
import dev.momostudios.coldsweat.client.gui.tooltip.*;
import dev.momostudios.coldsweat.common.capability.ItemInsulationCap;
import dev.momostudios.coldsweat.common.capability.ItemInsulationCap.Insulation;
import dev.momostudios.coldsweat.common.capability.ItemInsulationCap.InsulationPair;
import dev.momostudios.coldsweat.common.capability.ModCapabilities;
import dev.momostudios.coldsweat.common.item.SoulspringLampItem;
import dev.momostudios.coldsweat.config.ClientSettingsConfig;
import dev.momostudios.coldsweat.config.ConfigSettings;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.enchantment.IArmorVanishable;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class TooltipHandler
{
    @SubscribeEvent
    public static void addSimpleTooltips(ItemTooltipEvent event)
    {
        ItemStack stack = event.getItemStack();
        if (stack.getItem() == ModItems.FILLED_WATERSKIN && event.getPlayer() != null)
        {
            boolean celsius = ClientSettingsConfig.getInstance().isCelsius();
            double temp = stack.getOrCreateTag().getDouble("temperature");
            String color = temp == 0 ? "7" : (temp < 0 ? "9" : "c");
            String tempUnits = celsius ? "C" : "F";
            temp = temp / 2 + 95;
            if (celsius) temp = CSMath.convertTemp(temp, Temperature.Units.F, Temperature.Units.C, true);
            temp += ClientSettingsConfig.getInstance().getTempOffset() / 2.0;

            event.getToolTip().add(1, new StringTextComponent("§7" + new TranslationTextComponent(
                "item.cold_sweat.waterskin.filled").getString() + " (§" + color + (int) temp + " °" + tempUnits + "§7)§r"));
        }
        else if (stack.getItem() == ModItems.SOULSPRING_LAMP)
        {
            if (!Screen.hasShiftDown())
            {   event.getToolTip().add(1, new StringTextComponent("§9? §8'Shift'"));
            }
            if (event.getFlags().isAdvanced())
            {   event.getToolTip().add(Math.max(event.getToolTip().size() - 2, 1), new StringTextComponent("§fFuel: " + (int) event.getItemStack().getOrCreateTag().getDouble("fuel") + " / " + 64));
            }
        }
        else if (ConfigSettings.INSULATION_ITEMS.get().getOrDefault(stack.getItem(), ConfigSettings.ADAPTIVE_INSULATION_ITEMS.get().get(stack.getItem())) != null)
        {
            event.getToolTip().add(1, new StringTextComponent(" "));
        }
    }

    @SubscribeEvent
    public static void renderCustomTooltips(GuiScreenEvent.DrawScreenEvent.Post event)
    {
        if (event.getGui() instanceof ContainerScreen)
        {
            ContainerScreen<?> screen = (ContainerScreen<?>) event.getGui();
            Slot slot = screen.getSlotUnderMouse();
            if (slot == null) return;
            ItemStack stack = screen.getSlotUnderMouse().getItem();
            if (stack.isEmpty()) return;

            Tooltip tooltip = null;

            Pair<Double, Double> itemInsul = null;
            // Add the armor insulation tooltip if the armor has insulation
            if (stack.getItem() instanceof SoulspringLampItem)
            {   tooltip = new SoulspringTooltip(stack.getOrCreateTag().getDouble("fuel"));
            }
            // If the item is an insulation ingredient, add the tooltip
            else if ((itemInsul = ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem())) != null)
            {   tooltip = new InsulatorTooltip(ConfigSettings.INSULATION_ITEMS.get().get(stack.getItem()), false);
            }
            else if ((itemInsul = ConfigSettings.ADAPTIVE_INSULATION_ITEMS.get().get(stack.getItem())) != null)
            {   tooltip = new InsulatorTooltip(ConfigSettings.ADAPTIVE_INSULATION_ITEMS.get().get(stack.getItem()), true);
            }

            // If the item is insulated armor
            if (stack.getItem() instanceof IArmorVanishable && (itemInsul == null || !ConfigSettings.INSULATING_ARMORS.get().get(stack.getItem()).equals(itemInsul)))
            {
                // Create the list of insulation pairs from NBT
                List<InsulationPair> insulation = stack.getCapability(ModCapabilities.ITEM_INSULATION)
                .map(c ->
                {
                    if (c instanceof ItemInsulationCap)
                    {
                        return ((ItemInsulationCap) c);
                    }
                    return new ItemInsulationCap();
                }).map(cap -> cap.deserializeSimple(stack)).orElse(new ArrayList<>());

                // If the armor has intrinsic insulation due to configs, add it to the list
                ConfigSettings.INSULATING_ARMORS.get().computeIfPresent(stack.getItem(), (item, pair) ->
                {
                    double cold = pair.getFirst();
                    double hot = pair.getSecond();
                    double neutral = cold > 0 == hot > 0 ? CSMath.minAbs(cold, hot) : 0;
                    if (cold == neutral) cold = 0;
                    if (hot == neutral) hot = 0;
                    // Cold insulation
                    for (int i = 0; i < CSMath.ceil(Math.abs(cold)) / 2; i++)
                    {
                        double coldInsul = CSMath.minAbs(CSMath.shrink(cold, i * 2), 2);
                        insulation.add(new ItemInsulationCap.Insulation(coldInsul, 0d));
                    }

                    // Neutral insulation
                    for (int i = 0; i < CSMath.ceil(Math.abs(neutral)); i++)
                    {
                        double neutralInsul = CSMath.minAbs(CSMath.shrink(neutral, i), 1);
                        insulation.add(new Insulation(neutralInsul, neutralInsul));
                    }

                    // Hot insulation
                    for (int i = 0; i < CSMath.ceil(Math.abs(hot)) / 2; i++)
                    {
                        double hotInsul = CSMath.minAbs(CSMath.shrink(hot, i * 2), 2);
                        insulation.add(new Insulation(0d, hotInsul));
                    }
                    return pair;
                });

                // Sort the insulation values from cold to hot
                ItemInsulationCap.sortInsulationList(insulation);

                // Calculate the number of slots and render the insulation bar
                if (insulation.size() > 0)
                {   tooltip = new InsulationTooltip(insulation, stack);
                }
            }

            if (tooltip != null)
            {
                tooltip.renderImage(Minecraft.getInstance().font, event.getMouseX(), event.getMouseY(), event.getMatrixStack(), Minecraft.getInstance().getItemRenderer(), 0);
                tooltip.renderText(Minecraft.getInstance().font, event.getMouseX(), event.getMouseY(), event.getMatrixStack(), Minecraft.getInstance().getItemRenderer(), 0);
            }
        }
    }
}
