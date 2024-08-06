package com.momosoftworks.coldsweat.mixin_plugin;

import com.google.common.collect.ImmutableMap;
import com.momosoftworks.coldsweat.util.compat.CompatManager;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class ColdSweatMixinPlugin implements IMixinConfigPlugin
{
    private static final String compatMixinPackage = "com.momosoftworks.coldsweat.mixin.compat";
    private static final Map<String, Supplier<Boolean>> CONDITIONS = ImmutableMap.of(
            compatMixinPackage + "MixinCreateOverlay", () -> CompatManager.isCreateLoaded(),
            compatMixinPackage + "MixinSpoiledIcebox", () -> CompatManager.isSpoiledLoaded(),
            compatMixinPackage + "MixinSereneIceMelt", () -> CompatManager.isSereneSeasonsLoaded()
    );

    @Override
    public void onLoad(String mixinPackage)
    {

    }

    @Override
    public String getRefMapperConfig()
    {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName)
    {
        return CONDITIONS.getOrDefault(mixinClassName, () -> true).get();
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets)
    {

    }

    @Override
    public List<String> getMixins()
    {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
    {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo)
    {

    }
}
