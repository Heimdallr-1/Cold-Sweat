package com.momosoftworks.coldsweat.core.init;

import com.momosoftworks.coldsweat.ColdSweat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SoundInit
{
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ColdSweat.MOD_ID);

    public static final RegistryObject<SoundEvent> FREEZE_SOUND_REGISTRY = SOUNDS.register("entity.player.damage.freeze",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ColdSweat.MOD_ID, "entity.player.damage.freeze")));

    public static final RegistryObject<SoundEvent> SOUL_LAMP_ON_SOUND_REGISTRY = SOUNDS.register("item.soulspring_lamp.on",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ColdSweat.MOD_ID, "item.soulspring_lamp.on")));
    public static final RegistryObject<SoundEvent> SOUL_LAMP_OFF_SOUND_REGISTRY = SOUNDS.register("item.soulspring_lamp.off",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ColdSweat.MOD_ID, "item.soulspring_lamp.off")));

    public static final RegistryObject<SoundEvent> WATERSKIN_POUR_SOUND_REGISTRY = SOUNDS.register("item.waterskin.pour",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ColdSweat.MOD_ID, "item.waterskin.pour")));
    public static final RegistryObject<SoundEvent> WATERSKIN_FILL_SOUND_REGISTRY = SOUNDS.register("item.waterskin.fill",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ColdSweat.MOD_ID, "item.waterskin.fill")));

    public static final RegistryObject<SoundEvent> HEARTH_FUEL_SOUND_REGISTRY = SOUNDS.register("block.hearth.fuel",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ColdSweat.MOD_ID, "block.hearth.fuel")));

    public static final RegistryObject<SoundEvent> CHAMELEON_AMBIENT_SOUND_REGISTRY = SOUNDS.register("entity.chameleon.ambient",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ColdSweat.MOD_ID, "entity.chameleon.ambient")));
    public static final RegistryObject<SoundEvent> CHAMELEON_HURT_SOUND_REGISTRY = SOUNDS.register("entity.chameleon.hurt",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ColdSweat.MOD_ID, "entity.chameleon.hurt")));
    public static final RegistryObject<SoundEvent> CHAMELEON_DEATH_SOUND_REGISTRY = SOUNDS.register("entity.chameleon.death",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ColdSweat.MOD_ID, "entity.chameleon.death")));
    public static final RegistryObject<SoundEvent> CHAMELEON_FIND_SOUND_REGISTRY = SOUNDS.register("entity.chameleon.find",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ColdSweat.MOD_ID, "entity.chameleon.find")));
    public static final RegistryObject<SoundEvent> CHAMELEON_TONGUE_IN_SOUND_REGISTRY = SOUNDS.register("entity.chameleon.tongue.in",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ColdSweat.MOD_ID, "entity.chameleon.tongue.in")));
    public static final RegistryObject<SoundEvent> CHAMELEON_TONGUE_OUT_SOUND_REGISTRY = SOUNDS.register("entity.chameleon.tongue.out",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ColdSweat.MOD_ID, "entity.chameleon.tongue.out")));
    public static final RegistryObject<SoundEvent> CHAMELEON_SHED_SOUND_REGISTRY = SOUNDS.register("entity.chameleon.shed",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ColdSweat.MOD_ID, "entity.chameleon.shed")));
}
