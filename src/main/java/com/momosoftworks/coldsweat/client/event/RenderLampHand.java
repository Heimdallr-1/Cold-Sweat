package com.momosoftworks.coldsweat.client.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.momosoftworks.coldsweat.util.entity.EntityHelper;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.registries.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.FirstPersonRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.PlayerRenderer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.Math;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class RenderLampHand
{
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHandRender(RenderHandEvent event)
    {
        if (event.getItemStack().getItem() == ModItems.SOULSPRING_LAMP)
        {
            event.setCanceled(true);

            MatrixStack ms = event.getMatrixStack();
            ClientPlayerEntity player = Minecraft.getInstance().player;
            if (player == null) return;

            boolean isRightHand = EntityHelper.getArmFromHand(event.getHand(), player) == HandSide.RIGHT;
            PlayerRenderer playerRenderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
            FirstPersonRenderer handRenderer = Minecraft.getInstance().gameRenderer.itemInHandRenderer;

            ms.pushPose();

            // Idle position on the screen
            ms.translate(0, 0.2, -0.3);

            // Swing animation
            ms.pushPose();
            float swingProgress = event.getSwingProgress();
            float equipProgress = event.getEquipProgress();
            float handX = isRightHand ? 1.0F : -1.0F;
            // Position hand on screen
            ms.translate(isRightHand ? 0 : -0.698,
                         0.6,
                         isRightHand ? 0.7 : 0.76);
            float sqrtSwing = MathHelper.sqrt(swingProgress);
            float handSwingX = -0.3F * MathHelper.sin(sqrtSwing * (float)Math.PI);
            float handSwingY = 0.4F * MathHelper.sin(sqrtSwing * ((float)Math.PI * 2F));
            float handSwingZ = -0.4F * MathHelper.sin(swingProgress * (float)Math.PI);
            float swingSize = isRightHand
                              ? (equipProgress != 0 && equipProgress < 1 ? 2 : 4)
                              : (equipProgress != 0 && equipProgress < 1 ? 1.5f : 7);
            ms.translate(handX * (handSwingX + 0.64000005F), handSwingY/swingSize + -0.6F + equipProgress * -0.6F, handSwingZ/(swingSize*6) + -0.71999997F);
            ms.mulPose(Vector3f.YP.rotationDegrees(handX * 45.0F));
            float handFlailZ = MathHelper.sin(swingProgress * swingProgress * (float)Math.PI);
            float handFlailX = MathHelper.sin(sqrtSwing * (float)Math.PI);
            ms.mulPose(Vector3f.YP.rotationDegrees(handX * handFlailX * (isRightHand ? 1f : 20.0F)));
            ms.mulPose(Vector3f.XP.rotationDegrees(handX * handFlailX * (isRightHand ? -10.0F : 1f)));
            ms.mulPose(Vector3f.ZP.rotationDegrees(handX * handFlailZ * 20f));

            ms.mulPose(Vector3f.XP.rotationDegrees(90));
            if (isRightHand)
            {   ms.mulPose(Vector3f.ZP.rotationDegrees(-130));
            }
            else
            {   ms.mulPose(Vector3f.ZP.rotationDegrees(-230));
            }

            // Scale and translate hand so it's smaller on-screen
            ms.scale(0.5f, 0.5f, 0.5f);
            if (isRightHand)
            {   ms.translate(0.5, -0.1, 0.5);
            }
            else
            {   ms.translate(-1.2, -0.1, 0.5);
            }

            // Render the item/hand
            renderHand(ms, event.getBuffers(), event.getLight(), player, isRightHand, event.getHand(), handRenderer, playerRenderer, event.getItemStack());
            ms.popPose();
            ms.popPose();
        }
    }

    public static void transformArm(LivingEntity entity, ModelRenderer arm, HandSide side)
    {
        if (entity instanceof PlayerEntity && EntityHelper.holdingLamp(entity, side))
        {
            PlayerEntity player = ((PlayerEntity) entity);
            // Turn the player's arm so their "palm" is face-down
            float sideMultiplier = side == HandSide.RIGHT ? 1 : -1;
            arm.zRot += (float) (0.5*Math.PI) * sideMultiplier;
            arm.yRot = -arm.xRot * sideMultiplier - (float) (0.5*Math.PI) * sideMultiplier * 1.04f;
            arm.xRot = (float) -Math.PI/2;
            arm.x -= 1 * sideMultiplier;
            if (player.isCrouching())
            {   arm.xRot -= 0.4f;
            }

            // Better swinging animation
            if (player.swinging && side == EntityHelper.getArmFromHand(player.swingingArm, player))
            {   swingArm(arm, player, side);
            }
        }
    }

    private static void swingArm(ModelRenderer arm, LivingEntity player, HandSide side)
    {
        float sideMultiplier = side == HandSide.RIGHT ? 1 : -1;
        float partialTick = Minecraft.getInstance().getFrameTime();
        float attackAnim = player.getAttackAnim(partialTick);
        float playerPitch = player.getViewXRot(partialTick);
        float pitchFactor = getSwingHorizontalOffset(side, playerPitch);
        float windUpTime = 0.3f;
        float windUpPoint = 1.5f;
        float midSwingTime = 0.7f;

        if (attackAnim < windUpTime / 3)
        {
            arm.xRot += CSMath.blendLog(0f, windUpPoint, attackAnim, 0, windUpTime / 3) / pitchFactor;
        }
        else if (attackAnim < windUpTime)
        {
            arm.xRot += CSMath.blendLog(windUpPoint, 1f, attackAnim, windUpTime / 3, windUpTime) / pitchFactor;
        }
        else if (attackAnim < midSwingTime)
        {   arm.xRot += CSMath.blend(1f, 0.2f, attackAnim, windUpTime, midSwingTime) / pitchFactor;
        }
        else
        {   arm.xRot += CSMath.blendExp(0.2f, 0f, attackAnim, midSwingTime, 1) / pitchFactor;
        }
        float pitchSwingHeight = playerPitch < 0 ? playerPitch/20 : playerPitch/60;
        arm.yRot += (Math.pow(attackAnim - 0.5, 2) - 0.25) * pitchSwingHeight * sideMultiplier;
        if (side == HandSide.LEFT)
        {   arm.zRot += Math.sin(attackAnim*Math.PI)*0.5f;
        }
    }

    private static float getSwingHorizontalOffset(HandSide side, float playerPitch)
    {
        float pitchFactor;
        if (side == HandSide.RIGHT)
        {
            pitchFactor = playerPitch < 0 ? CSMath.blend(1.8f, 0.6f, playerPitch, 0, -90)
                                          : CSMath.blend(1.8f, 3f, playerPitch, 0, 90);
        }
        else
        {
            pitchFactor = playerPitch < 0 ? CSMath.blend(3f, 0.5f, playerPitch, 0, -90)
                                          : CSMath.blend(3f, 3f, playerPitch, 0, 90);
        }
        return pitchFactor;
    }

    public static void rotateArmorShoulder(BipedModel<?> model, HandSide side, boolean slim)
    {
        if (side == HandSide.RIGHT)
        {
            float xRot = model.rightArm.xRot;
            model.rightArm.zRot -= CSMath.toRadians(90);
            model.rightArm.xRot = -model.rightArm.yRot - CSMath.toRadians(90);
            model.rightArm.yRot = xRot + CSMath.toRadians(90);
            model.rightArm.x += 1;
            if (!slim)
            {   model.rightArm.y -= 1;
            }
        }
        else if (side == HandSide.LEFT)
        {
            float xRot = model.leftArm.xRot;
            model.leftArm.zRot += CSMath.toRadians(90);
            model.leftArm.xRot = model.leftArm.yRot - CSMath.toRadians(90);
            model.leftArm.yRot = -xRot - CSMath.toRadians(90);
            model.leftArm.x -= 1;
            if (!slim)
            {   model.leftArm.y -= 1;
            }
        }
    }

    private static void renderHand(MatrixStack ms, IRenderTypeBuffer bufferSource, int light, AbstractClientPlayerEntity player, boolean isRightHand,
                                   Hand hand, FirstPersonRenderer handRenderer, PlayerRenderer playerRenderer, ItemStack itemStack)
    {
        boolean isSelected = player.getItemInHand(hand).getItem() == ModItems.SOULSPRING_LAMP;
        // Render arm
        ms.pushPose();
        ms.pushPose();
        ms.scale(1, 1.2f, 1);
        ms.mulPose(Vector3f.XP.rotationDegrees(-25));
        ms.translate(0, -0.2, 0.25);
        // The hand moves a little bit when being put away. I don't know why
        if (!isSelected)
        {   ms.translate(0, isRightHand ? -0.012 : 0.015, 0);
            ms.mulPose(Vector3f.ZP.rotationDegrees(2.4f * (isRightHand ? -1 : 1)));
            ms.mulPose(Vector3f.XP.rotationDegrees(2.3f));
        }
        // Render arm for the correct side
        if (isRightHand)
        {
            if (isSelected)
            {
                ms.translate(-0.365, -0.2, -0.075);
                ms.mulPose(Vector3f.YP.rotationDegrees(-90));
                ms.mulPose(Vector3f.ZP.rotationDegrees(-90));
            }
            else
            {   ms.translate(-0.3925, 0.06, 0.38);
                ms.mulPose(Vector3f.YP.rotationDegrees(-90));
            }
            playerRenderer.renderRightHand(ms, bufferSource, light, player);
        }
        else
        {
            if (isSelected)
            {
                ms.translate(-0.335, -0.2, -0.075);
                ms.mulPose(Vector3f.YP.rotationDegrees(90));
                ms.mulPose(Vector3f.ZP.rotationDegrees(90));
            }
            else
            {   ms.translate(-0.325, 0.06, 0.38);
                ms.mulPose(Vector3f.YP.rotationDegrees(87));
            }
            playerRenderer.renderLeftHand(ms, bufferSource, light, player);
        }
        ms.popPose();

        // Render lamp item
        ms.pushPose();
        ms.mulPose(Vector3f.XP.rotationDegrees(-90));
        ms.translate(-0.35, 0.1, 0.625);
        ms.scale(1, 1, 0.8f);
        if (isRightHand)
        {
            ms.mulPose(Vector3f.ZP.rotationDegrees(90));
            ms.translate(-0.1, 0.125, 0);
            handRenderer.renderItem(player, itemStack, ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, false, ms, bufferSource, light);
        }
        else
        {
            ms.mulPose(Vector3f.ZP.rotationDegrees(90));
            ms.translate(-0.1, 0.125, 0);
            handRenderer.renderItem(player, itemStack, ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, false, ms, bufferSource, light);
        }
        ms.popPose();
        ms.popPose();
    }
}