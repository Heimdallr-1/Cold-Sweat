package dev.momostudios.coldsweat;

import dev.momostudios.coldsweat.client.gui.Overlays;
import dev.momostudios.coldsweat.common.capability.ITemperatureCap;
import dev.momostudios.coldsweat.common.entity.Chameleon;
import dev.momostudios.coldsweat.config.*;
import dev.momostudios.coldsweat.core.advancement.trigger.ModAdvancementTriggers;
import dev.momostudios.coldsweat.core.init.*;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.util.compat.CompatManager;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.SlotTypePreset;

@Mod(ColdSweat.MOD_ID)
public class ColdSweat
{
    public static final Logger LOGGER = LogManager.getFormatterLogger("Cold Sweat");
    public static final boolean REMAP_MIXINS = false;

    public static final String MOD_ID = "cold_sweat";

    public ColdSweat()
    {
        MinecraftForge.EVENT_BUS.register(this);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(this::commonSetup);
        bus.addListener(this::clientSetup);
        bus.addListener(this::registerCaps);
        if (CompatManager.isCuriosLoaded()) bus.addListener(this::registerCurioSlots);

        BlockInit.BLOCKS.register(bus);
        ItemInit.ITEMS.register(bus);
        EntityInit.ENTITY_TYPES.register(bus);
        BlockEntityInit.BLOCK_ENTITY_TYPES.register(bus);
        MenuInit.MENU_TYPES.register(bus);
        EffectInit.EFFECTS.register(bus);
        ParticleTypesInit.PARTICLES.register(bus);
        PotionInit.POTIONS.register(bus);
        SoundInit.SOUNDS.register(bus);
        FeatureInit.FEATURES.register(bus);

        // Setup configs
        WorldSettingsConfig.setup();
        ItemSettingsConfig.setup();
        ColdSweatConfig.setup();
        ClientSettingsConfig.setup();
        EntitySettingsConfig.setup();
    }

    public void commonSetup(final FMLCommonSetupEvent event)
    {
        // Setup packets
        ColdSweatPacketHandler.init();
        event.enqueueWork(() ->
        {
            SpawnPlacements.register(EntityInit.CHAMELEON.get(), SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Chameleon::canSpawn);

            // Register advancement triggers
            CriteriaTriggers.register(ModAdvancementTriggers.TEMPERATURE_CHANGED);
            CriteriaTriggers.register(ModAdvancementTriggers.SOUL_LAMP_FUELLED);
            CriteriaTriggers.register(ModAdvancementTriggers.BLOCK_AFFECTS_TEMP);
            CriteriaTriggers.register(ModAdvancementTriggers.ARMOR_INSULATED);
        });
        // Load configs to memory
        ConfigSettings.load();
    }

    public void clientSetup(final FMLClientSetupEvent event)
    {
        // Fix hearth transparency
        ItemBlockRenderTypes.setRenderLayer(BlockInit.HEARTH_BOTTOM.get(), RenderType.cutoutMipped());
        ItemBlockRenderTypes.setRenderLayer(BlockInit.SOUL_STALK.get(), RenderType.cutoutMipped());
        // Register GUI elements
        Overlays.registerOverlays();
    }

    public void registerCaps(RegisterCapabilitiesEvent event)
    {
        event.register(ITemperatureCap.class);
    }

    public void registerCurioSlots(InterModEnqueueEvent event)
    {
        event.enqueueWork(() ->
        {   InterModComms.sendTo(ColdSweat.MOD_ID, CuriosApi.MODID, SlotTypeMessage.REGISTER_TYPE,
                                 () -> SlotTypePreset.CHARM.getMessageBuilder().build());
        });
    }
}
