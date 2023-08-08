package dev.momostudios.coldsweat.api.temperature.modifier;

import dev.momostudios.coldsweat.api.event.common.TempModifierEvent;
import dev.momostudios.coldsweat.api.event.core.TempModifierRegisterEvent;
import dev.momostudios.coldsweat.api.util.Temperature;
import dev.momostudios.coldsweat.core.init.TempModifierInit;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;

import java.util.function.Function;

/**
 * TempModifiers are applied to entities to dynamically change their temperature.<br>
 * For example, biome temperature, time of day, depth, and waterskins are all TempModifiers<br>
 *<br>
 * It is up to you to apply and remove these modifiers manually.<br>
 *<br>
 * TempModifiers must be REGISTERED using {@link TempModifierRegisterEvent}<br>
 * (see {@link TempModifierInit} for an example)<br>
 */
public abstract class TempModifier
{
    private CompoundTag nbt = new CompoundTag();
    private int expireTicks = -1;
    private int ticksExisted = 0;
    private int tickRate = 1;
    private double lastInput = 0;
    private double lastOutput = 0;
    private Function<Double, Double> function = temp -> temp;

    /**
     * Default constructor (REQUIRED for proper registration).<br>
     */
    public TempModifier() {}

    /**
     * Determines what the provided temperature would be, given the player it is being applied to.<br>
     * This is basically a simple in-out system. It is given a temperature, and returns a new temperature based on the PlayerEntity.<br>
     * <br>
     * @param entity the entity that is being affected by the modifier.<br>
     * @return the new temperature.<br>
     */
    protected abstract Function<Double, Double> calculate(LivingEntity entity, Temperature.Type type);

    /**
     * Posts this TempModifier's {@link #calculate(LivingEntity, dev.momostudios.coldsweat.api.util.Temperature.Type)} to the Forge event bus.<br>
     * Returns the stored value if this TempModifier has a tickRate set, and it is not the right tick.<br>
     * <br>
     * @param temp the Temperature being fed into the {@link #calculate(LivingEntity, dev.momostudios.coldsweat.api.util.Temperature.Type)} method.
     * @param entity the entity that is being affected by the modifier.
     */
    public final double update(double temp, LivingEntity entity, Temperature.Type type)
    {
        TempModifierEvent.Calculate.Pre pre = new TempModifierEvent.Calculate.Pre(this, entity, temp);
        MinecraftForge.EVENT_BUS.post(pre);

        if (pre.isCanceled()) return pre.getTemperature();

        this.function = this.calculate(entity, type);

        TempModifierEvent.Calculate.Post post = new TempModifierEvent.Calculate.Post(this, entity, this.getResult(pre.getTemperature()));
        MinecraftForge.EVENT_BUS.post(post);

        return post.getTemperature();
    }

    /**
     * @param temp the Temperature to calculate with
     * @return The result of this TempModifier's unique stored function. Stores the input and output.
     */
    public final double getResult(double temp)
    {
        lastInput = temp;
        return lastOutput = function.apply(temp);
    }

    /**
     * Sets the number of ticks this TempModifier will exist before it is automatically removed.<br>
     * @param ticks the number of ticks this modifier will last.
     * @return this TempModifier instance (allows for in-line building).
     */
    public final TempModifier expires(int ticks)
    {
        expireTicks = ticks;
        return this;
    }
    public final int getExpireTime()
    {   return expireTicks;
    }
    public final int getTicksExisted()
    {   return ticksExisted;
    }
    public final int setTicksExisted(int ticks)
    {   return ticksExisted = ticks;
    }

    /**
     * TempModifiers can be configured to run {@link TempModifier#calculate(LivingEntity)} at a specified interval.<br>
     * This is useful if the TempModifier is expensive to calculate, and you want to avoid it being called each tick.<br>
     * <br>
     * Every X ticks, the TempModifier's {@code getResult()} function will be called, then stored internally.<br>
     * Every other time {@code calculate()} is called, the stored value will be returned until X ticks have passed.<br>
     * (new TempModifiers ALWAYS run {@code getResult()} when they are called for the first time).<br>
     * <br>
     * @param ticks the number of ticks between each call to {@code getResult()}.
     * @return this TempModifier instance (allows for in-line building).
     */
    public final TempModifier tickRate(int ticks)
    {   tickRate = Math.max(1, ticks);
        return this;
    }

    public final int getTickRate()
    {   return tickRate;
    }

    /**
     * @return The Temperature this TempModifier was last given
     */
    public final double getLastInput()
    {   return lastInput;
    }

    /**
     * @return The Temperature this TempModifier's function last returned
     */
    public final double getLastOutput()
    {   return lastOutput;
    }

    public final CompoundTag getNBT()
    {
        return nbt;
    }

    public void setNBT(CompoundTag data)
    {
        this.nbt = data;
    }

    /**
     * The ID is used to mark the TempModifier when it is stored in NBT
     * @return the String ID of the TempModifier. You should include your mod's ID to prevent duplicate IDs.<br>
     */
    public abstract String getID();

    @Override
    public String toString()
    {
        return this.getID();
    }
}
