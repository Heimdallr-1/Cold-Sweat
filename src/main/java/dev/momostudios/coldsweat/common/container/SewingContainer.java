package dev.momostudios.coldsweat.common.container;

import com.mojang.datafixers.util.Pair;
import dev.momostudios.coldsweat.common.capability.IInsulatableCap;
import dev.momostudios.coldsweat.common.capability.ItemInsulationCap;
import dev.momostudios.coldsweat.common.capability.ModCapabilities;
import dev.momostudios.coldsweat.common.event.ArmorInsulation;
import dev.momostudios.coldsweat.core.init.MenuInit;
import dev.momostudios.coldsweat.util.config.ConfigSettings;
import dev.momostudios.coldsweat.util.math.CSMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class SewingContainer extends AbstractContainerMenu
{
    BlockPos pos;
    Inventory playerInventory;
    SewingInventory sewingInventory;

    public static class SewingInventory implements Container
    {
        private final NonNullList<ItemStack> stackList;
        private final AbstractContainerMenu menu;

        public SewingInventory(AbstractContainerMenu menu)
        {
            this.stackList = NonNullList.withSize(3, ItemStack.EMPTY);
            this.menu = menu;
        }

        @Override
        public int getContainerSize()
        {
            return 3;
        }

        @Override
        public boolean isEmpty()
        {
            return !stackList.stream().anyMatch(stack -> !stack.isEmpty());
        }

        @Nonnull
        @Override
        public ItemStack getItem(int index)
        {
            return stackList.get(index);
        }

        @Nonnull
        @Override
        public ItemStack removeItem(int index, int count)
        {
            ItemStack itemstack = ContainerHelper.removeItem(this.stackList, index, count);
            if (!itemstack.isEmpty()) {
                this.menu.slotsChanged(this);
            }

            return itemstack;
        }

        @Nonnull
        @Override
        public ItemStack removeItemNoUpdate(int index)
        {
            return ContainerHelper.takeItem(this.stackList, index);
        }

        @Override
        public void setItem(int index, ItemStack stack)
        {
            this.stackList.set(index, stack);
            this.menu.slotsChanged(this);
        }

        @Override
        public void setChanged() {}

        @Override
        public boolean stillValid(Player player)
        {
            return true;
        }

        @Override
        public void clearContent()
        {
            stackList.clear();
        }
    }

    public SewingContainer(final int windowId, final Inventory playerInv)
    {
        super(MenuInit.SEWING_CONTAINER_TYPE.get(), windowId);
        this.pos = playerInv.player.blockPosition();
        this.playerInventory = playerInv;
        sewingInventory = new SewingInventory(this);

        // Input 1
        this.addSlot(new Slot(sewingInventory, 0, 43, 26)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                Pair<Double, Double> insulation = ArmorInsulation.getItemInsulation(stack);
                return stack.getItem() instanceof ArmorItem
                    && insulation.getFirst() == 0 && insulation.getSecond() == 0;
            }
            @Override
            public void onTake(Player player, ItemStack stack)
            {
                super.onTake(player, stack);
                SewingContainer.this.takeInput();
            }
            @Override
            public void setChanged()
            {
                super.setChanged();
                SewingContainer.this.testForRecipe();
            }
        });

        // Input 2
        this.addSlot(new Slot(sewingInventory, 1, 43, 53)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                Pair<Double, Double> insulation = ArmorInsulation.getItemInsulation(stack);
                return insulation.getFirst() > 0 || insulation.getSecond() > 0 || stack.getItem() instanceof ShearsItem;
            }
            @Override
            public void onTake(Player player, ItemStack stack)
            {
                super.onTake(player, stack);
                SewingContainer.this.takeInput();
            }
            @Override
            public void setChanged()
            {
                super.setChanged();
                SewingContainer.this.testForRecipe();
            }
        });

        // Output
        this.addSlot(new Slot(sewingInventory, 2, 121, 39)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack)
            {
                super.onTake(player, stack);
                SewingContainer.this.takeOutput(stack);
            }
        });

        // Main player inventory
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new Slot(playerInv, col + (9 * row) + 9, 8 + col * 18, 166 - (4 - row) * 18 - 10));
            }
        }

        // Player Hotbar
        for (int col = 0; col < 9; col++)
        {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    public SewingContainer(int i, Inventory inventory, FriendlyByteBuf friendlyByteBuf)
    {
        this(i, inventory);
        try {
            this.pos = BlockPos.of(friendlyByteBuf.readLong());
        } catch (Exception ignored) {}
    }

    public void setItem(int index, ItemStack stack)
    {
        this.sewingInventory.setItem(index, stack);
        this.setRemoteSlot(index, stack);
    }

    public void growItem(int index, int amount)
    {
        ItemStack stack = this.sewingInventory.getItem(index);
        stack.grow(amount);
        this.sewingInventory.setItem(index, stack);
        this.setRemoteSlot(index, stack);
    }

    public ItemStack getItem(int index)
    {
        return this.sewingInventory.getItem(index);
    }

    private void takeInput()
    {
        this.sewingInventory.setItem(2, ItemStack.EMPTY);
    }
    private void takeOutput(ItemStack stack)
    {
        Player player = this.playerInventory.player;
        ItemStack input1 = this.sewingInventory.getItem(0);
        ItemStack input2 = this.sewingInventory.getItem(1);

        // If insulation is being removed
        if (this.getItem(1).getItem() instanceof ShearsItem)
        {
            // Damage shears
            input2.hurt(1, new Random(), null);
            input1.getCapability(ModCapabilities.ITEM_INSULATION).ifPresent(cap ->
            {
                // Put the insulation item that was removed in the first input slot
                this.setItem(0, cap.getInsulationItems().get(cap.getInsulationItems().size() - 1));
                // Play shear sound
                player.level.playSound(null, player.blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS, 0.8F, 1.0F);
            });
        }
        // If insulation is being added
        else
        {
            // Remove input items
            this.growItem(0, -1);
            this.growItem(1, -1);
            player.level.playSound(null, player.blockPosition(), SoundEvents.LLAMA_SWAG, SoundSource.BLOCKS, 0.5f, 1f);
        }

        // Get equip sound for the armor item
        SoundEvent equipSound = stack.getItem().getEquipSound();
        if (equipSound != null) player.level.playSound(null, player.blockPosition(), equipSound, SoundSource.BLOCKS, 1f, 1f);
    }

    private ItemStack testForRecipe()
    {
        ItemStack armorItem = this.getItem(0);
        ItemStack insulatorItem = this.getItem(1);
        ItemStack result = ItemStack.EMPTY;

        // Is the first item armor, and the second item an insulator
        if (armorItem.getItem() instanceof ArmorItem)
        {
            ItemStack processed = armorItem.copy();
            IInsulatableCap insulCap = processed.getCapability(ModCapabilities.ITEM_INSULATION).orElse(new ItemInsulationCap());
            int filledSlots = insulCap.getInsulationItems().size();

            // Shears are used to remove insulation
            if (insulatorItem.getItem() instanceof ShearsItem && filledSlots > 0)
            {
                insulCap.removeInsulationItem(insulCap.getInsulationItems().size() - 1);
            }
            // Item is for insulation
            else if (ConfigSettings.INSULATION_ITEMS.get().containsKey(insulatorItem.getItem())
            && filledSlots < ArmorInsulation.getInsulationSlots(armorItem) && (!(insulatorItem.getItem() instanceof ArmorItem)
            || LivingEntity.getEquipmentSlotForItem(armorItem) == LivingEntity.getEquipmentSlotForItem(insulatorItem)))
            {
                ItemStack insulator = insulatorItem.copy();
                insulator.setCount(1);
                insulCap.addInsulationItem(insulator);
            }
            else return ItemStack.EMPTY;

            processed.getOrCreateTag().putBoolean("Insulated", true);

            // Set NBT data that will be synced to the client (only used for tooltip)
            ListTag list = new ListTag();
            for (Pair<Double, Double> pair : insulCap.getInsulation())
            {
                CompoundTag tag = new CompoundTag();
                tag.putDouble("Cold", pair.getFirst());
                tag.putDouble("Hot", pair.getSecond());
                list.add(tag);
            }
            processed.getOrCreateTag().put("Insulation", list);

            // Remove "Insulated" tag if armor has no insulation left
            processed.getCapability(ModCapabilities.ITEM_INSULATION).ifPresent(cap ->
            {
                if (cap.getInsulationItems().isEmpty())
                    processed.getOrCreateTag().putBoolean("Insulated", false);
            });

            this.setItem(2, processed);
            result = processed;
        }
        return result;
    }

    public SewingContainer(final int windowId, final Player playerInv, BlockPos pos)
    {
        this(windowId, playerInv.getInventory());
        this.pos = pos;
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);

        // Drop the contents of the input slots
        if (player instanceof ServerPlayer)
        {
            for (int i = 0; i < sewingInventory.getContainerSize(); i++)
            {
                ItemStack itemStack = this.getSlot(i).getItem();
                if (!itemStack.isEmpty() && i != 2)
                {
                    if (player.isAlive() && !((ServerPlayer) player).hasDisconnected())
                    {
                        player.getInventory().placeItemBackInInventory(itemStack);
                    }
                    else player.drop(itemStack, false, true);

                    setCarried(ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public boolean stillValid(Player playerIn)
    {
        if (this.pos != null)
            return playerIn.distanceToSqr(Vec3.atCenterOf(this.pos)) <= 64.0D;
        else return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem())
        {
            ItemStack slotItem = slot.getItem();
            newStack = slotItem.copy();
            if (CSMath.isInRange(index, 0, 2))
            {
                if (this.moveItemStackTo(slotItem, 3, 39, true))
                {
                    slot.onTake(player, newStack);
                }
                else return ItemStack.EMPTY;
            }
            else
            {
                Pair<Double, Double> itemValue = ArmorInsulation.getItemInsulation(slotItem);
                if (itemValue.getFirst() > 0 || itemValue.getSecond() > 0 || slotItem.getItem() instanceof ShearsItem)
                {
                    if (this.moveItemStackTo(slotItem, 1, 2, false))
                    {
                        slot.onQuickCraft(slotItem, newStack);
                    }
                    else return ItemStack.EMPTY;
                }
                else if (slotItem.getItem() instanceof ArmorItem)
                {
                    if (!this.moveItemStackTo(slotItem, 0, 1, false))
                    {
                        slot.onQuickCraft(slotItem, newStack);
                    }
                    else return ItemStack.EMPTY;
                }
                else if (index == 2)
                {
                    if (!this.moveItemStackTo(slotItem, 3, 39, false))
                    {
                        slot.onQuickCraft(slotItem, newStack);
                    }
                    else return ItemStack.EMPTY;
                }
                else if (CSMath.isInRange(index, slots.size() - 9, slots.size()))
                {
                    if (!this.moveItemStackTo(slotItem, 3, 29, false))
                    {
                        slot.onQuickCraft(slotItem, newStack);
                    }
                    else return ItemStack.EMPTY;
                }
                else if (CSMath.isInRange(index, 3, slots.size() - 9))
                {
                    if (!this.moveItemStackTo(slotItem, slots.size() - 9, slots.size(), false))
                    {
                        slot.onQuickCraft(slotItem, newStack);
                    }
                    else return ItemStack.EMPTY;
                }
                return ItemStack.EMPTY;
            }

            if (slotItem.isEmpty())
            {
                slot.set(ItemStack.EMPTY);
            }
            else slot.setChanged();
        }

        return newStack;
    }
}
