package dev.momostudios.coldsweat.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.momostudios.coldsweat.api.util.Temperature;
import dev.momostudios.coldsweat.common.capability.ModCapabilities;
import dev.momostudios.coldsweat.common.capability.PlayerTempCap;
import dev.momostudios.coldsweat.config.ClientSettingsConfig;
import dev.momostudios.coldsweat.config.ConfigSettings;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class Overlays
{
    static ClientSettingsConfig CLIENT_CONFIG = ClientSettingsConfig.getInstance();

    // Stuff for world temperature
    public static double WORLD_TEMP = 0;
    static boolean ADVANCED_WORLD_TEMP = false;
    static double PREV_WORLD_TEMP = 0;
    static double MAX_OFFSET = 0;
    static double MIN_OFFSET = 0;

    // Stuff for body temperature
    public static double BODY_TEMP = 0;
    static double PREV_BODY_TEMP = 0;
    static int BLEND_BODY_TEMP = 0;
    static int ICON_BOB = 0;
    static int BODY_ICON = 0;
    static int PREV_BODY_ICON = 0;
    static int BODY_TRANSITION_PROGRESS = 0;
    static int BODY_BLEND_TIME = 10;
    static int BODY_TEMP_SEVERITY = 0;

    public static void registerOverlays()
    {
        OverlayRegistry.registerOverlayBelow(ForgeIngameGui.CHAT_PANEL_ELEMENT, "world_temp", (gui, poseStack, partialTick, width, height) ->
        {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && ADVANCED_WORLD_TEMP && Minecraft.getInstance().gameMode.getPlayerMode() != GameType.SPECTATOR && !Minecraft.getInstance().options.hideGui)
            {
                gui.setupOverlayRenderState(true, false);

                double min = ConfigSettings.MIN_TEMP.get();
                double max = ConfigSettings.MAX_TEMP.get();

                // Get player world temperature
                double temp = CSMath.convertTemp(WORLD_TEMP, CLIENT_CONFIG.isCelsius() ? Temperature.Units.C : Temperature.Units.F, Temperature.Units.MC, true);

                // Get the temperature severity
                int severity = getWorldSeverity(temp, min, max, MIN_OFFSET, MAX_OFFSET);

                // Set text color
                int color = switch (severity)
                {   case  2, 3 -> 16297781;
                    case  4    -> 16728089;
                    case -2,-3 -> 8443135;
                    case -4    -> 4236031;
                    default -> 14737376;
                };


                /* Render gauge */

                poseStack.pushPose();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.setShader(GameRenderer::getPositionTexShader);

                // Set gauge texture
                RenderSystem.setShaderTexture(0, new ResourceLocation("cold_sweat:textures/gui/overlay/world_temp_gauge.png"));

                // Render frame
                GuiComponent.blit(poseStack, (width / 2) + 92 + CLIENT_CONFIG.getWorldGaugeX(), height - 19 + CLIENT_CONFIG.getWorldGaugeY(), 0, 64 - severity * 16, 25, 16, 25, 144);

                RenderSystem.disableBlend();

                // Sets the text bobbing offset (or none if disabled)
                int bob = CLIENT_CONFIG.isIconBobbingEnabled() && !CSMath.withinRange(temp, min + MIN_OFFSET, max + MAX_OFFSET) && player.tickCount % 2 == 0 ? 1 : 0;

                // Render text
                int blendedTemp = (int) CSMath.blend(PREV_WORLD_TEMP, WORLD_TEMP, Minecraft.getInstance().getFrameTime(), 0, 1);

                Minecraft.getInstance().font.draw(poseStack, (blendedTemp + CLIENT_CONFIG.getTempOffset())+"",
                        /* X */ width / 2f + 105 + (Integer.toString(blendedTemp + CLIENT_CONFIG.getTempOffset()).length() * -3) + CLIENT_CONFIG.getWorldGaugeX(),
                        /* Y */ height - 15 - bob + CLIENT_CONFIG.getWorldGaugeY(), color);
                poseStack.popPose();
            }
        });

        OverlayRegistry.registerOverlayBelow(ForgeIngameGui.CHAT_PANEL_ELEMENT, "body_temp", (gui, poseStack, partialTick, width, height) ->
        {
            gui.setupOverlayRenderState(true, false);
            Minecraft mc = Minecraft.getInstance();

            if (gui.shouldDrawSurvivalElements() && !Minecraft.getInstance().options.hideGui)
            {
                // Blend body temp (per frame)
                BLEND_BODY_TEMP = (int) CSMath.blend(PREV_BODY_TEMP, BODY_TEMP, Minecraft.getInstance().getFrameTime(), 0, 1);

                // Get text color
                int color = switch (BODY_TEMP_SEVERITY)
                {   case  7, -7 -> 16777215;
                    case  6 -> 16777132;
                    case  5 -> 16767856;
                    case  4 -> 16759634;
                    case  3 -> 16751174;
                    case -3 -> 6078975;
                    case -4 -> 7528447;
                    case -5 -> 8713471;
                    case -6 -> 11599871;
                    default -> BLEND_BODY_TEMP > 0 ? 16744509
                             : BLEND_BODY_TEMP < 0 ? 4233468
                             : 11513775;
                };

                // Get the outer border color when readout is > 100
                int colorBG = BLEND_BODY_TEMP < 0 ? 1122643
                            : BLEND_BODY_TEMP > 0 ? 5376516
                            : 0;

                int bobLevel = Math.min(Math.abs(BODY_TEMP_SEVERITY), 3);
                int threatOffset = !CLIENT_CONFIG.isIconBobbingEnabled() ? 0
                                 : bobLevel == 2 ? ICON_BOB
                                 : bobLevel == 3 ? Minecraft.getInstance().cameraEntity.tickCount % 2
                                 : 0;

                RenderSystem.defaultBlendFunc();

                // Render old icon (if blending)
                RenderSystem.setShaderTexture(0, new ResourceLocation("cold_sweat:textures/gui/overlay/body_temp_gauge.png"));
                if (BODY_TRANSITION_PROGRESS < BODY_BLEND_TIME)
                {
                    GuiComponent.blit(poseStack, (width / 2) - 5 + CLIENT_CONFIG.getBodyIconX(), height - 53 - threatOffset + CLIENT_CONFIG.getBodyIconY(), 0, 30 - PREV_BODY_ICON * 10, 10, 10, 10, 70);
                    RenderSystem.enableBlend();
                    RenderSystem.setShaderColor(1, 1, 1, (mc.getFrameTime() + BODY_TRANSITION_PROGRESS) / BODY_BLEND_TIME);
                }
                // Render new icon on top of old icon (if blending)
                // Otherwise this is just the regular icon
                GuiComponent.blit(poseStack, (width / 2) - 5 + CLIENT_CONFIG.getBodyIconX(), height - 53 - threatOffset + CLIENT_CONFIG.getBodyIconY(), 0, 30 - BODY_ICON * 10, 10, 10, 10, 70);
                RenderSystem.setShaderColor(1, 1, 1, 1);

                // Render Readout
                Font font = mc.font;
                int scaledWidth = mc.getWindow().getGuiScaledWidth();
                int scaledHeight = mc.getWindow().getGuiScaledHeight();

                String s = "" + Math.min(Math.abs(BLEND_BODY_TEMP), 100);
                float x = (scaledWidth - font.width(s)) / 2f + CLIENT_CONFIG.getBodyReadoutX();
                float y = scaledHeight - 31f - 10f + CLIENT_CONFIG.getBodyReadoutY();

                // Draw the outline
                font.draw(poseStack, s, x + 1, y, colorBG);
                font.draw(poseStack, s, x - 1, y, colorBG);
                font.draw(poseStack, s, x, y + 1, colorBG);
                font.draw(poseStack, s, x, y - 1, colorBG);

                // Draw the readout
                font.draw(poseStack, s, x, y, color);
            }
        });

        OverlayRegistry.registerOverlayBelow(ForgeIngameGui.CHAT_PANEL_ELEMENT, "vague_temp", (gui, poseStack, partialTick, width, height) ->
        {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player != null && !ADVANCED_WORLD_TEMP && mc.gameMode.getPlayerMode() != GameType.SPECTATOR && !mc.options.hideGui)
            {
                gui.setupOverlayRenderState(true, false);

                double min = ConfigSettings.MIN_TEMP.get();
                double max = ConfigSettings.MAX_TEMP.get();

                // Get player world temperature
                double temp = CSMath.convertTemp(WORLD_TEMP, CLIENT_CONFIG.isCelsius() ? Temperature.Units.C : Temperature.Units.F, Temperature.Units.MC, true);
                // Get the temperature severity
                int severity = getWorldSeverity(temp, min, max, MIN_OFFSET, MAX_OFFSET);
                int renderOffset = CSMath.clamp(severity, -1, 1) * 3;

                poseStack.pushPose();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.setShader(GameRenderer::getPositionTexShader);

                // Set gauge texture
                RenderSystem.setShaderTexture(0, new ResourceLocation("cold_sweat:textures/gui/overlay/vague_temp_gauge.png"));

                // Render frame
                GuiComponent.blit(poseStack, (width / 2) + 96 + CLIENT_CONFIG.getWorldGaugeX(), height - 19 + CLIENT_CONFIG.getWorldGaugeY() - renderOffset, 0, 64 - severity * 16, 16, 16, 16, 144);

                RenderSystem.disableBlend();
                poseStack.popPose();
            }
        });
    }

    // Handle temperature blending and transitions
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        Player player = Minecraft.getInstance().player;
        if (event.phase == TickEvent.Phase.START && player != null)
        {
            player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(icap ->
            {
                if (!(icap instanceof PlayerTempCap cap)) return;

                    cap.calculateVisibility(player);
                    ADVANCED_WORLD_TEMP = cap.showAdvancedWorldTemp();


                /* World Temp */

                // Get temperature in actual degrees
                boolean celsius = CLIENT_CONFIG.isCelsius();
                double worldTemp = cap.getTemp(Temperature.Type.WORLD);
                double realTemp = CSMath.convertTemp(worldTemp, Temperature.Units.MC, celsius ? Temperature.Units.C : Temperature.Units.F, true);
                // Calculate the blended world temp for this tick
                double diff = realTemp - WORLD_TEMP;
                PREV_WORLD_TEMP = WORLD_TEMP;
                WORLD_TEMP += Math.abs(diff) <= 0.5 ? diff : diff / 4d;

                // Update max/min offset
                MAX_OFFSET = cap.getTemp(Temperature.Type.FREEZING_POINT);
                MIN_OFFSET = cap.getTemp(Temperature.Type.BURNING_POINT);


                /* Body Temp */

                // Blend body temp (per tick)
                PREV_BODY_TEMP = BODY_TEMP;
                double currentTemp = cap.getTemp(Temperature.Type.BODY);
                BODY_TEMP = Math.abs(currentTemp - BODY_TEMP) < 0.1 ? currentTemp : BODY_TEMP + (cap.getTemp(Temperature.Type.BODY) - BODY_TEMP) / 5;

                // Handle effects for the icon (bobbing, stage, transition)
                // Get icon bob
                ICON_BOB = player.tickCount % 3 == 0 && Math.random() < 0.3 ? 1 : 0;

                // Get the severity of the player's body temperature
                BODY_TEMP_SEVERITY = getBodySeverity(BLEND_BODY_TEMP);

                // Get the icon to be displayed
                int neededIcon = CSMath.clamp(BODY_TEMP_SEVERITY, -3, 3);

                // Start transition
                if (BODY_ICON != neededIcon)
                {   BODY_ICON = neededIcon;
                    BODY_TRANSITION_PROGRESS = 0;
                }

                // Tick the transition progress
                if (PREV_BODY_ICON != BODY_ICON && BODY_TRANSITION_PROGRESS++ >= BODY_BLEND_TIME)
                {   PREV_BODY_ICON = BODY_ICON;
                }
            });
        }
    }

    public static int getWorldSeverity(double temp, double min, double max, double offsMin, double offsMax)
    {   return (int) CSMath.blend(-4, 4, temp, min + offsMin, max + offsMax);
    }

    static int getBodySeverity(int temp)
    {
        int sign = CSMath.getSign(temp);
        int absTemp = Math.abs(temp);

        return absTemp < 100 ? (int) Math.floor(CSMath.blend(0, 3, absTemp, 0, 100)) * sign
                             : (int) CSMath.blend(3, 7, absTemp, 100, 150) * sign;
    }

    public static void setBodyTemp(double temp)
    {   BODY_TEMP = temp;
        PREV_BODY_TEMP = temp;
        BLEND_BODY_TEMP = (int) temp;
    }
}
