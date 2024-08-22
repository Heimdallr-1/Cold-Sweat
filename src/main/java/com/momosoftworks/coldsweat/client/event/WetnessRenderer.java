package com.momosoftworks.coldsweat.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.api.temperature.modifier.WaterTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.util.math.CSMath;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector2f;
import org.joml.Vector2i;
import oshi.util.tuples.Triplet;

import java.util.*;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class WetnessRenderer
{
    private static final ResourceLocation WATER_DROP = new ResourceLocation(ColdSweat.MOD_ID, "textures/gui/overlay/droplet.png");
    private static final ResourceLocation WATER_DROP_TRAIL = new ResourceLocation(ColdSweat.MOD_ID, "textures/gui/overlay/droplet_trail.png");
    private static final List<Droplet> WATER_DROPS = new ArrayList<>();
    private static final List<Triplet<Vector2i, Float, Integer>> TRAILS = new ArrayList<>();
    private static boolean WAS_SUBMERGED = false;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiEvent.Pre event)
    {
        Minecraft mc = Minecraft.getInstance();
        float frametime = mc.getDeltaFrameTime();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        boolean paused = mc.isPaused();
        int uiScale = mc.options.guiScale().get();

        Player player = mc.player;
        if (player == null) return;

        float playerYVelocity = (float) (player.position().y - player.yOld);
        boolean isSubmerged = player.canSwimInFluidType(player.getEyeInFluidType());

        // Clear water drops when the player submerges
        if (isSubmerged && !paused)
        {
            TRAILS.clear();
            for (Droplet drop : WATER_DROPS)
            {
                drop.alpha -= 0.6f * frametime;
                float xMoveDir = drop.position.x < screenWidth / 2f ? -1 : 1;
                float yMoveDir = drop.position.y < screenHeight / 2f ? -1 : 1;
                drop.position.add(new Vector2f(xMoveDir, yMoveDir).mul(200 * -playerYVelocity * frametime));
            }
        }

        // Get the player's wetness level
        double wetness = Temperature.getModifier(mc.player, Temperature.Trait.WORLD, WaterTempModifier.class).map(mod -> mod.getNBT().getDouble("Strength")).orElse(0d);

        // Spawn a bunch of droplets when the player exits the water
        boolean justExitedWater = WAS_SUBMERGED && !isSubmerged;
        if (justExitedWater)
        {
            for (int i = 0; i < 20; i++)
            {
                Droplet newDrop = createDrop(screenWidth, screenHeight);
                newDrop.yMotion = getRandomVelocity(frametime) / 2 + 0.3f;
                newDrop.position.y = (float) Math.random() * screenHeight;
                WATER_DROPS.add(newDrop);
                int streakLength = (int) (Math.random() * 5) + 5;
                int x = (int)newDrop.position.x;
                int y = (int)newDrop.position.y;
                for (int j = 1; j < streakLength; j++)
                {
                    TRAILS.add(new Triplet<>(new Vector2i(x, y - j),
                                             CSMath.blend(newDrop.alpha * 0.8f, 0, j, 1, streakLength),
                                             newDrop.size / 2));
                }
            }
        }
        WAS_SUBMERGED = isSubmerged;

        // Spawn droplets randomly when the player is wet
        if (!paused && !isSubmerged && wetness > 0.01f && ((float) Math.random() * 1 * frametime) < 0.01f * wetness * frametime)
        {
            WATER_DROPS.add(createDrop(screenWidth, screenHeight));
        }

        // Handle rendering & movement of water drops
        RenderSystem.enableBlend();
        for (int i = 0; i < WATER_DROPS.size(); i++)
        {
            Droplet drop = WATER_DROPS.get(i);
            Vector2f pos = drop.position;
            float alpha = drop.alpha;
            int size = drop.size / uiScale * 3;

            if (alpha > 0)
            {
                // Render the water drop
                RenderSystem.setShaderColor(1, 1, 1, alpha);
                event.getGuiGraphics().blit(WATER_DROP, (int) CSMath.roundNearest(pos.x, 3f/uiScale), (int)pos.y, size, size, 0, 0, 8, 8, 8, 8);

                // Update the drop's position and alpha
                if (!paused)
                {
                    // Fade out
                    drop.alpha -= 0.001f * frametime;
                    // Recalculate the y velocity every so often
                    if (drop.yMotionUpdateCooldown <= 0)
                    {
                        drop.yMotionUpdateCooldown = (float) Math.random() * 16f + 8f;
                        drop.yMotion = getRandomVelocity(frametime);
                    }
                    else drop.yMotionUpdateCooldown -= frametime;

                    // Movement due to player motion
                    float dropFallFromPlayerVel = Math.max(-4f * frametime, playerYVelocity * frametime * 20);
                    float dropMoveFromPlayerLook = -(player.yHeadRot - player.yHeadRotO) / 20;
                    drop.xVelocity = (float) CSMath.maxAbs(dropMoveFromPlayerLook * (Math.random() * 0.2), drop.xVelocity);
                    drop.xVelocity /= 1 + 0.6f * frametime;

                    // Randomly change the x motion
                    if (drop.XMotionUpdateCooldown <= 0)
                    {
                        drop.XMotionUpdateCooldown = (float) Math.random() * 8f + 4f;
                        drop.xMotion = (float) Math.random() * 0.02f - 0.01f;
                    }
                    drop.XMotionUpdateCooldown -= frametime;

                    int oldY = (int)pos.y;
                    // Move the drop
                    if (!isSubmerged)
                    {   drop.position.add(new Vector2f(drop.xMotion * drop.yMotion * 20 + drop.xVelocity, drop.yMotion + dropFallFromPlayerVel).div(uiScale).mul(3));
                    }

                    // Add a trail behind the drop
                    for (int j = 0; j < Math.max(0, (int) (pos.y - oldY)); j++)
                    {
                        TRAILS.add(new Triplet<>(new Vector2i((int)pos.x, oldY + j), alpha, size));
                    }
                }

                // Wrap drops around the screen
                if (pos.x < -20)
                {   pos.x = screenWidth + 20;
                }
                else if (pos.x > screenWidth + 20)
                {   pos.x = -20;
                }
                // Remove drops that fall off the bottom of the screen
                if (pos.y > screenHeight)
                {   WATER_DROPS.remove(drop);
                    i--;
                }
            }
            // Remove drops that have faded out
            else
            {   WATER_DROPS.remove(drop);
                i--;
            }
        }

        // Render water drop trails
        for (int i = 0; i < TRAILS.size(); i++)
        {
            Triplet<Vector2i, Float, Integer> trail = TRAILS.get(i);
            Vector2i pos = trail.getA();
            float alpha = trail.getB();
            int size = trail.getC();

            if (alpha > 0)
            {
                RenderSystem.setShaderColor(1, 1, 1, alpha);
                event.getGuiGraphics().blit(WATER_DROP_TRAIL, (int) CSMath.roundNearest(pos.x, 3f/uiScale * 4), pos.y, size, 1, 0, 0, 8, 1, 8, 1);
                if (!paused)
                {   TRAILS.set(i, new Triplet<>(new Vector2i(pos.x, pos.y), alpha - 0.045f * frametime, size));
                }
            }
            else
            {   TRAILS.remove(trail);
                i--;
            }
        }
    }

    private static float getRandomVelocity(float frametime)
    {
        return (float) Math.min(0.7f * frametime * 20, (Math.pow(Math.random() * 3 + 0.1f, 4) * frametime) / 4f);
    }

    private static Droplet createDrop(int screenWidth, int screenHeight)
    {
        int size = new Random().nextInt(24, 32);
        return new Droplet(new Vector2f((int) (Math.random() * screenWidth), -size), 1f, size);
    }

    protected static class Droplet
    {
        public Vector2f position;
        public float alpha;
        public int size;
        public float yMotion = (float) Math.random() * 0.05f + 0.05f;
        public float xMotion = (float) Math.random() * 0.02f - 0.01f;
        public float xVelocity = 0;
        public float yMotionUpdateCooldown = (float) Math.random() * 16f + 8f;
        public float XMotionUpdateCooldown = 16;

        public Droplet(Vector2f position, float alpha, int size)
        {
            this.position = position;
            this.alpha = alpha;
            this.size = size;
        }
    }
}
