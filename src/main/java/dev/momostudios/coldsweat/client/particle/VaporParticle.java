package dev.momostudios.coldsweat.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class VaporParticle extends TextureSheetParticle
{
    private final SpriteSet ageSprite;
    private final boolean hasGravity;
    private boolean collidedY;
    private float maxAlpha;

    protected VaporParticle(ClientLevel world, double x, double y, double z, double vx, double vy, double vz, SpriteSet spriteSet, boolean hasGravity)
    {
        super(world, x, y, z);
        this.ageSprite = spriteSet;
        this.alpha = 0.0f;
        this.maxAlpha = (float) (Math.random() / 3 + 0.2f);
        this.scale(3f + (float) (Math.random() / 2.5f));
        this.setSize(quadSize / 10f, quadSize / 10f);
        this.lifetime = 40 + (int) (Math.random() * 20 - 10);
        this.hasPhysics = true;
        this.setParticleSpeed(vx, vy, vz);
        this.setSpriteFromAge(spriteSet);
        this.hasGravity = hasGravity;
        this.gravity = hasGravity ? 0.04f : -0.04f;
    }

    @Nonnull
    @Override
    public ParticleRenderType getRenderType()
    {
        return ParticleUtil.PARTICLE_SHEET_TRANSPARENT;
    }

    @Override
    public void tick()
    {
        if (Minecraft.getInstance().options.particles == ParticleStatus.MINIMAL)
            this.remove();

        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime)
        {
            this.remove();
        }
        else
        {
            this.yd -= 0.04D * gravity;
            this.move(xd * (onGround ? 1 : 0.2), yd, zd * (onGround ? 1 : 0.2));
            this.xd *= 0.99;
            this.yd *= 0.99;
            this.xd *= 0.99;
        }

        this.setSpriteFromAge(ageSprite);

        if (hasGravity)
        {
            if (this.alpha < maxAlpha)
                this.alpha += 0.02f;
            else if (this.age > 32)
                this.alpha -= 0.02f;

            if (this.alpha < 0.035 && this.age > 10)
                this.remove();
        }
        else
        {
            if (this.age < 10)
                this.alpha += 0.07f;
            else if (this.age > this.lifetime - this.alpha / 0.02f)
                this.alpha -= 0.02f;

            if (this.alpha < 0.07  && this.age > 10)
                this.remove();
        }
    }

    @Override
    public void move(double x, double y, double z)
    {
        double d0 = x;
        double d1 = y;
        double d2 = z;
        if (this.hasPhysics && (x != 0.0D || y != 0.0D || z != 0.0D)) {
            Vec3 vec3 = Entity.collideBoundingBox((Entity)null, new Vec3(x, y, z), this.getBoundingBox(), this.level, List.of());
            x = vec3.x;
            y = vec3.y;
            z = vec3.z;
        }

        if (x != 0.0D || y != 0.0D || z != 0.0D) {
            this.setBoundingBox(this.getBoundingBox().move(x, collidedY ? 0 : y, z));
            AABB axisalignedbb = this.getBoundingBox();
            this.x = (axisalignedbb.minX + axisalignedbb.maxX) / 2.0D;
            this.y = axisalignedbb.minY + (hasGravity ? 0.2 : 0);
            this.z = (axisalignedbb.minZ + axisalignedbb.maxZ) / 2.0D;
        }

        if (Math.abs(d1) >= 1.0E-5d && Math.abs(y) < 1.0E-5d) {
            this.collidedY = true;
        }

        this.onGround = d1 != y && d1 < 0.0D;
        if (d0 != x) {
            this.xd = 0.0D;
        }

        if (d2 != z) {
            this.zd = 0.0D;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class SteamFactory implements ParticleProvider<SimpleParticleType>
    {
        private final SpriteSet sprite;

        public SteamFactory(SpriteSet spriteSet) {
            this.sprite = spriteSet;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
        {
            if (Minecraft.getInstance().options.particles != ParticleStatus.MINIMAL)
                return new VaporParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprite, false);
            else
                return null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class MistFactory implements ParticleProvider<SimpleParticleType>
    {
        private final SpriteSet sprite;

        public MistFactory(SpriteSet spriteSet) {
            this.sprite = spriteSet;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed)
        {
            ParticleStatus status = Minecraft.getInstance().options.particles;
            if (status != ParticleStatus.MINIMAL && status != ParticleStatus.DECREASED)
                return new VaporParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprite, true);
            else
                return null;
        }
    }
}
