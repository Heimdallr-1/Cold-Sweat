package com.momosoftworks.coldsweat.data.codec.requirement;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.data.codec.util.IntegerBounds;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public record ItemRequirement(Optional<List<Either<TagKey<Item>, Item>>> items, Optional<TagKey<Item>> tag,
                              Optional<IntegerBounds> count, Optional<IntegerBounds> durability,
                              Optional<List<EnchantmentRequirement>> enchantments, Optional<List<EnchantmentRequirement>> storedEnchantments,
                              Optional<Potion> potion, NbtRequirement nbt)
{
    public static final Codec<ItemRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ConfigHelper.tagOrRegistryCodec(Registries.ITEM, ForgeRegistries.ITEMS).listOf().optionalFieldOf("items").forGetter(predicate -> predicate.items),
            TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(predicate -> predicate.tag),
            IntegerBounds.CODEC.optionalFieldOf("count").forGetter(predicate -> predicate.count),
            IntegerBounds.CODEC.optionalFieldOf("durability").forGetter(predicate -> predicate.durability),
            EnchantmentRequirement.CODEC.listOf().optionalFieldOf("enchantments").forGetter(predicate -> predicate.enchantments),
            EnchantmentRequirement.CODEC.listOf().optionalFieldOf("stored_enchantments").forGetter(predicate -> predicate.storedEnchantments),
            ForgeRegistries.POTIONS.getCodec().optionalFieldOf("potion").forGetter(predicate -> predicate.potion),
            NbtRequirement.CODEC.optionalFieldOf("nbt", new NbtRequirement(new CompoundTag())).forGetter(predicate -> predicate.nbt)
    ).apply(instance, ItemRequirement::new));

    public boolean test(ItemStack stack, boolean ignoreCount)
    {
        if (stack.isEmpty() && items.isPresent() && !items.get().isEmpty())
        {   return false;
        }

        if (this.nbt.tag().isEmpty())
        {   return true;
        }
        if (items.isPresent())
        {
            for (int i = 0; i < items.get().size(); i++)
            {
                Either<TagKey<Item>, Item> either = items.get().get(i);
                if (either.map(stack::is, stack::is))
                {   break;
                }
                if (i == items.get().size() - 1)
                {   return false;
                }
            }
        }
        if (tag.isPresent() && !stack.is(tag.get()))
        {   return false;
        }
        if (!ignoreCount && count.isPresent() && !count.get().test(stack.getCount()))
        {   return false;
        }
        else if (durability.isPresent() && !durability.get().test(stack.getMaxDamage() - stack.getDamageValue()))
        {   return false;
        }
        else if (potion.isPresent() && !potion.get().getEffects().equals(PotionUtils.getPotion(stack).getEffects()))
        {   return false;
        }
        else if (!nbt.test(stack.getTag()))
        {   return false;
        }
        else if (enchantments.isPresent())
        {
            Map<Enchantment, Integer> stackEnchantments = EnchantmentHelper.deserializeEnchantments(stack.getEnchantmentTags());
            for (EnchantmentRequirement enchantment : enchantments.get())
            {
                if (!enchantment.test(stackEnchantments))
                {   return false;
                }
            }
        }
        else if (storedEnchantments.isPresent())
        {
            Map<Enchantment, Integer> stackEnchantments = EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(stack));
            for (EnchantmentRequirement enchantment : storedEnchantments.get())
            {   if (!enchantment.test(stackEnchantments))
                {   return false;
                }
            }
        }
        return true;
    }

    public CompoundTag serialize()
    {
        CompoundTag nbt = new CompoundTag();
        items.ifPresent(itemList -> nbt.put("items", NBTHelper.listTagOf(itemList.stream().map(either -> StringTag.valueOf(either.map(tag -> "#" + tag.location(),
                                                                                                                       item -> ForgeRegistries.ITEMS.getKey(item).toString())))
                                                                     .collect(Collectors.toList()))));
        count.ifPresent(count -> nbt.put("count", count.serialize()));
        durability.ifPresent(durability -> nbt.put("durability", durability.serialize()));
        enchantments.ifPresent(enchantments -> nbt.put("enchantments", NBTHelper.listTagOf(enchantments.stream().map(EnchantmentRequirement::serialize).collect(Collectors.toList()))));
        storedEnchantments.ifPresent(enchantments -> nbt.put("stored_enchantments", NBTHelper.listTagOf(enchantments.stream().map(EnchantmentRequirement::serialize).collect(Collectors.toList()))));
        potion.ifPresent(potion -> nbt.putString("potion", ForgeRegistries.POTIONS.getKey(potion).toString()));
        if (!this.nbt.tag().isEmpty()) nbt.put("nbt", this.nbt.serialize());
        return nbt;
    }

    public static ItemRequirement deserialize(CompoundTag nbt)
    {
        Optional<List<Either<TagKey<Item>, Item>>> items = Optional.of(nbt.getList("items", 8)
                                                           .stream()
                                                           .map(tg ->
                                                           {
                                                                 String string = tg.getAsString();
                                                                 ResourceLocation location = new ResourceLocation(string.replace("#", ""));
                                                                 if (!string.contains("#"))
                                                                 {   return Either.<TagKey<Item>, Item>right(ForgeRegistries.ITEMS.getValue(location));
                                                                 }

                                                                 return Either.<TagKey<Item>, Item>left(TagKey.create(Registries.ITEM, location));
                                                           })
                                                           .toList());

        Optional<TagKey<Item>> tag = nbt.contains("tag") ? Optional.of(TagKey.create(Registries.ITEM, new ResourceLocation(nbt.getString("tag"))))
                                                         : Optional.empty();

        Optional<IntegerBounds> count = nbt.contains("count") ? Optional.of(IntegerBounds.deserialize(nbt.getCompound("count")))
                                                              : java.util.Optional.empty();

        Optional<IntegerBounds> durability = nbt.contains("durability") ? Optional.of(IntegerBounds.deserialize(nbt.getCompound("durability")))
                                                                        : java.util.Optional.empty();

        Optional<List<EnchantmentRequirement>> enchantments = nbt.contains("enchantments") ? Optional.of(nbt.getList("enchantments", 10)
                                                                                                         .stream()
                                                                                                         .map(tg -> EnchantmentRequirement.deserialize(((CompoundTag) tg)))
                                                                                                         .toList())
                                                                                           : java.util.Optional.empty();

        Optional<List<EnchantmentRequirement>> storedEnchantments = nbt.contains("stored_enchantments") ? Optional.of(nbt.getList("stored_enchantments", 10)
                                                                                                                      .stream()
                                                                                                                      .map(tg -> EnchantmentRequirement.deserialize(((CompoundTag) tg)))
                                                                                                                      .toList())
                                                                                                        : java.util.Optional.empty();

        Optional<Potion> potion = nbt.contains("potion") ? java.util.Optional.ofNullable(ForgeRegistries.POTIONS.getValue(new ResourceLocation(nbt.getString("potion"))))
                                                         : java.util.Optional.empty();

        NbtRequirement nbtReq = nbt.contains("nbt") ? NbtRequirement.deserialize(nbt.getCompound("nbt"))
                                                      : new NbtRequirement(new CompoundTag());

        return new ItemRequirement(items, tag, count, durability, enchantments, storedEnchantments, potion, nbtReq);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {   return true;
        }
        if (obj == null || getClass() != obj.getClass())
        {   return false;
        }

        ItemRequirement that = (ItemRequirement) obj;

        if (!items.equals(that.items))
        {   return false;
        }
        if (!items.equals(that.items))
        {   return false;
        }
        if (!count.equals(that.count))
        {   return false;
        }
        if (!durability.equals(that.durability))
        {   return false;
        }
        if (!enchantments.equals(that.enchantments))
        {   return false;
        }
        if (!storedEnchantments.equals(that.storedEnchantments))
        {   return false;
        }
        if (!potion.equals(that.potion))
        {   return false;
        }
        return nbt.equals(that.nbt);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("ItemRequirement{");
        items.ifPresent(itemList -> itemList.forEach(either -> builder.append(either.map(tag -> "#" + tag.location(),
                                                                                         item -> ForgeRegistries.ITEMS.getKey(item)).toString())
                                                                      .append(", ")));
        count.ifPresent(bounds -> builder.append(bounds.toString()).append(", "));
        durability.ifPresent(bounds -> builder.append(bounds.toString()).append(", "));
        enchantments.ifPresent(enchantments -> builder.append("Enchantments: {").append(enchantments.stream().map(EnchantmentRequirement::toString).collect(Collectors.joining(", "))).append("}, "));
        storedEnchantments.ifPresent(enchantments -> builder.append("Stored Enchantments: {").append(enchantments.stream().map(EnchantmentRequirement::toString).collect(Collectors.joining(", "))).append("}, "));
        potion.ifPresent(potion -> builder.append("Potion: ").append(ForgeRegistries.POTIONS.getKey(potion).toString()));
        builder.append("NBT: ").append(nbt.toString()).append(", ");
        builder.append("}");

        return builder.toString();
    }
}
