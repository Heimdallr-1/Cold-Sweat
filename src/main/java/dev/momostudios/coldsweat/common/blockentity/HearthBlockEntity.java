package dev.momostudios.coldsweat.common.blockentity;

import com.mojang.datafixers.util.Pair;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.GlassFluidPipeBlock;
import dev.momostudios.coldsweat.ColdSweat;
import dev.momostudios.coldsweat.api.temperature.modifier.HearthTempModifier;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import dev.momostudios.coldsweat.api.util.Temperature;
import dev.momostudios.coldsweat.common.block.HearthBottomBlock;
import dev.momostudios.coldsweat.common.capability.ModCapabilities;
import dev.momostudios.coldsweat.common.container.HearthContainer;
import dev.momostudios.coldsweat.common.event.HearthSaveDataHandler;
import dev.momostudios.coldsweat.config.ClientSettingsConfig;
import dev.momostudios.coldsweat.core.init.BlockEntityInit;
import dev.momostudios.coldsweat.core.init.ParticleTypesInit;
import dev.momostudios.coldsweat.core.network.ColdSweatPacketHandler;
import dev.momostudios.coldsweat.core.network.message.HearthResetMessage;
import dev.momostudios.coldsweat.util.ClientOnlyHelper;
import dev.momostudios.coldsweat.util.compat.CompatManager;
import dev.momostudios.coldsweat.config.ConfigSettings;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.registries.ModEffects;
import dev.momostudios.coldsweat.util.registries.ModSounds;
import dev.momostudios.coldsweat.util.world.SpreadPath;
import dev.momostudios.coldsweat.util.world.WorldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.network.PacketDistributor;

import java.lang.reflect.Method;
import java.util.*;

@Mod.EventBusSubscriber
public class HearthBlockEntity extends RandomizableContainerBlockEntity
{
    static Method TICK_DOWN_EFFECT;
    static
    {
        try
        {   TICK_DOWN_EFFECT = ObfuscationReflectionHelper.findMethod(MobEffectInstance.class, "m_19579_");
            TICK_DOWN_EFFECT.setAccessible(true);
        }
        catch (Exception ignored) {}
    }

    // List of SpreadPaths, which determine where the Hearth is affecting and how it spreads through/around blocks
    ArrayList<SpreadPath> paths = new ArrayList<>();
    // Used for client-side rendering of the Hearth's F3 debug
    ArrayList<SpreadPath> displayPaths = new ArrayList<>();
    // Used as a lookup table for detecting duplicate paths (faster than ArrayList#contains())
    Set<BlockPos> pathLookup = new HashSet<>();
    // Lookup exclusively used for detecting block updates within the hearth's range
    Set<BlockPos> affectedBlocks = new HashSet<>();

    // Stores previously-called chunks for quicker access
    HashMap<ChunkPos, LevelChunk> loadedChunks = new HashMap<>();

    List<MobEffectInstance> effects = new ArrayList<>();

    private static final int INSULATION_TIME = 1200;
    public static final int MAX_FUEL = 1000;
    public static final int SLOT_COUNT = 1;
    private static final boolean CREATE_LOADED = CompatManager.isCreateLoaded();

    protected NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    BlockPos blockPos = this.getBlockPos();

    int hotFuel = 0;
    int coldFuel = 0;
    boolean shouldUseHotFuel = false;
    boolean shouldUseColdFuel = false;
    boolean hasHotFuel = false;
    boolean hasColdFuel = false;
    int insulationLevel = 0;

    boolean isPlayerNearby = false;
    List<Player> players = new ArrayList<>();
    int rebuildCooldown = 0;
    boolean forceRebuild = false;
    Set<BlockPos> notifyQueue = new HashSet<>();

    public int ticksExisted = 0;

    private LevelChunk workingChunk = null;
    private ChunkPos workingCoords = new ChunkPos(this.getBlockPos().getX() >> 4, this.getBlockPos().getZ() >> 4);

    boolean showParticles = true;
    int frozenPaths = 0;
    boolean spreading = true;
    private boolean registeredLocation = false;


    public HearthBlockEntity(BlockPos pos, BlockState state)
    {
        super(BlockEntityInit.HEARTH_BLOCK_ENTITY_TYPE.get(), pos, state);
        this.addPath(new SpreadPath(pos).setOrigin(blockPos));
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onBlockUpdate(BlockEvent.NeighborNotifyEvent event)
    {
        BlockPos bpos = event.getPos();
        if (bpos.closerThan(blockPos, this.getMaxRange()) && affectedBlocks.contains(bpos))
        {   this.sendBlockUpdate(bpos);
        }
    }

    /**
     * Range of the Hearth starting from an exit point
     */
    public int getSpreadRange()
    {
        return 20;
    }

    /**
     * Range of the Hearth starting from the Hearth's position
     */
    public int getMaxRange()
    {
        return 96;
    }

    public int maxPaths()
    {
        return 12000;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container." + ColdSweat.MOD_ID + ".hearth");
    }

    @Override
    public Component getDisplayName()
    {   return this.getCustomName() != null ? this.getCustomName() : this.getDefaultName();
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> itemsIn)
    {
        this.items = itemsIn;
    }

    public static <T extends BlockEntity> void tickSelf(Level level, BlockPos pos, BlockState state, T te)
    {
        if (te instanceof HearthBlockEntity hearth)
        {
            hearth.tick(level, pos);
        }
    }

    public void tick(Level level, BlockPos pos)
    {
        // Register the hearth's position to the global map
        if (!this.registeredLocation)
        {   HearthSaveDataHandler.HEARTH_POSITIONS.add(Pair.of(pos, level.dimension().location()));
            this.registeredLocation = true;
        }

        // Easy access to clientside test
        boolean isClient = level.isClientSide;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        this.ticksExisted++;

        if (rebuildCooldown > 0) rebuildCooldown--;

        if (this.level != null && this.ticksExisted % 20 == 0)
        {
            this.isPlayerNearby = false;
            players.clear();
            for (Player player : this.level.players())
            {
                if (player.blockPosition().closerThan(pos, this.getMaxRange()))
                {
                    players.add(player);
                    this.isPlayerNearby = true;
                }
            }
        }

        // Tick down the time for each effect
        if (!effects.isEmpty())
        {
            effects.removeIf(effect ->
            {
                try
                {
                    TICK_DOWN_EFFECT.invoke(effect);
                    if (effect.getDuration() <=0) return true;
                } catch (Exception ignored) {}
                return false;
            });
        }

        // Clear paths every 5 minutes to account for calculation errors
        if (this.ticksExisted % 6000 == 0)
        {   this.replacePaths(new ArrayList<>());
        }

        // Reset if a nearby block has been updated
        if (forceRebuild || (rebuildCooldown <= 0 && !notifyQueue.isEmpty()))
        {   this.updateNotifiedPaths();
        }

        if (hotFuel > 0 || coldFuel > 0)
        {
            // Gradually increases insulation amount
            if (insulationLevel < INSULATION_TIME)
                insulationLevel++;

            if (this.isPlayerNearby)
            {
                if (this.ticksExisted % 10 == 0)
                {
                    showParticles = isClient
                            && Minecraft.getInstance().options.particles().get() == ParticleStatus.ALL
                            && !HearthSaveDataHandler.DISABLED_HEARTHS.contains(Pair.of(pos, level.dimension().location().toString()));
                }

                // Mark as not spreading if all paths are frozen
                if (this.frozenPaths >= paths.size())
                    this.spreading = false;
                // Keep displayPaths updated (used for F3 wireframe view)
                else if (isClient && ClientSettingsConfig.getInstance().hearthDebug())
                {   displayPaths = new ArrayList<>(paths);
                }
                // Keep affectedBlocks updated (used for block update detection)
                if (pathLookup.size() != affectedBlocks.size())
                {   affectedBlocks = pathLookup;
                }

                if (paths.isEmpty()) this.addPath(new SpreadPath(pos).setOrigin(pos));

                /*
                 Partition the points into logical "sub-maps" to be iterated over separately each tick
                */
                int pathCount = paths.size();
                // Size of each partition (defaults to 1/30th of the total paths)
                int partSize = CSMath.clamp(pathCount / 30, 20, 400);
                // Number of partitions
                int partCount = CSMath.ceil(pathCount / (double) partSize);
                // Index of the last point being worked on this tick
                int lastIndex = partSize * ((this.ticksExisted % partCount) + 1);
                // Index of the first point being worked on this tick
                int firstIndex = Math.max(0, lastIndex - partSize);


                /*
                 Iterate over the specified partition of paths
                 */
                for (int i = firstIndex; i < Math.min(paths.size(), lastIndex); i++)
                {   // This operation is really fast because it's an ArrayList
                    SpreadPath spreadPath = paths.get(i);

                    int spX = spreadPath.getX();
                    int spY = spreadPath.getY();
                    int spZ = spreadPath.getZ();

                    // Use try-finally because there's still stuff to do even if "continue;" skips the rest of the code
                    try
                    {   // Don't try to spread if the path is frozen
                        if (spreadPath.isFrozen())
                        {
                            // Remove a 3D-checkerboard of paths after the Hearth is finished spreading to reduce pointless iteration overhead
                            // The Hearth is "finished spreading" when all paths are frozen
                            if (!spreading && (Math.abs(spY % 2) == 0) == (Math.abs(spX % 2) == Math.abs(spZ % 2)))
                            {   paths.remove(i);
                                // Go back and reiterate over the new path at this index
                                i--;
                            }
                            // Don't do anything else with this path
                            continue;
                        }

                        /*
                         Try to spread to new blocks
                         */

                        // The origin of the path is usually the hearth's position,
                        // but if it's spreading through Create pipes then the origin is the end of the pipe
                        if (pathCount < this.maxPaths() && spreadPath.withinDistance(spreadPath.getOrigin(), this.getSpreadRange()))
                        {
                            /*
                             Get the chunk at this position
                             */
                            LevelChunk chunk = getChunk(new ChunkPos(spX >> 4, spZ >> 4));
                            if (chunk == null) continue;

                            /*
                             Spreading algorithm
                             */

                            BlockPos pathPos = spreadPath.getPos();
                            BlockState fromState = WorldHelper.getBlockState(chunk, pathPos);

                            if (!WorldHelper.canSeeSky(chunk, level, pathPos.above(), 64))
                            {
                                // Try to spread in every direction from the current position
                                for (Direction direction : Direction.values())
                                {
                                    BlockPos tryPos = pathPos.relative(direction);

                                    // Avoid duplicate paths (ArrayList isn't duplicate-safe like Sets/Maps)
                                    // .add() functions to both add the path and check if it's already in the list
                                    if (pathLookup.add(tryPos))
                                    {
                                        SpreadPath newPath = spreadPath.spreadTo(tryPos, direction);

                                        // If the BlockState is a pipe, check if the new path is following the direction of the pipe
                                        if (this.isCorrectSpreadDirectionForPipes(pathPos, fromState, newPath, direction)
                                        && !WorldHelper.isSpreadBlocked(level, fromState, pathPos, direction, spreadPath.getDirection()))
                                        {   // Add the new path to the list
                                            paths.add(newPath);
                                        }
                                    }
                                }
                            }
                            // Remove this path if it has skylight access
                            else
                            {   pathLookup.remove(pathPos);
                                paths.remove(i);
                                i--;
                                continue;
                            }
                        }
                        // Track frozen paths to know when the Hearth is done spreading
                        spreadPath.freeze();
                        this.frozenPaths++;
                    }

                    /*
                     Give insulation & spawn particles
                     */
                    finally
                    {
                        // Air Particles
                        if (isClient && showParticles)
                        {
                            Random rand = new Random();
                            if (!(Minecraft.getInstance().options.renderDebug && ClientSettingsConfig.getInstance().hearthDebug()) && rand.nextFloat() < (spreading ? 0.016f : 0.032f))
                            {   float xr = rand.nextFloat();
                                float yr = rand.nextFloat();
                                float zr = rand.nextFloat();
                                float xm = rand.nextFloat() / 20 - 0.025f;
                                float zm = rand.nextFloat() / 20 - 0.025f;

                                level.addParticle(ParticleTypesInit.HEARTH_AIR.get(), false, spX + xr, spY + yr, spZ + zr, xm, 0, zm);
                            }
                        }

                        // Give insulation to players
                        if (!isClient)
                        {
                            for (int p = 0; p < players.size(); p++)
                            {   Player player = players.get(p);
                                // If player is null or not in range, skip
                                if (player == null || player.distanceToSqr(spX, spY, spZ) > 1)
                                    continue;

                                TempModifier modifier = Temperature.getModifier(player, Temperature.Type.WORLD, mod -> mod instanceof HearthTempModifier);

                                if (modifier == null || modifier.getTicksExisted() >= modifier.getExpireTime() - 60)
                                {   this.insulatePlayer(player);
                                    break;
                                }

                                players.remove(p);
                                p--;
                            }
                        }
                    }
                }

                // Drain fuel
                if (this.ticksExisted % 80 == 0)
                {
                    if (shouldUseColdFuel)
                    {   this.setColdFuel(coldFuel - 1, false);
                    }
                    if (shouldUseHotFuel)
                    {   this.setHotFuel(hotFuel - 1, false);
                    }
                    if (shouldUseHotFuel || shouldUseColdFuel)
                        this.updateFuelState();

                    shouldUseColdFuel = false;
                    shouldUseHotFuel = false;
                }
            }
        }

        // Input fuel
        if (this.ticksExisted % 10 == 0)
        {   this.checkForFuel();
        }

        // Particles
        if (isClient)
        {
            Random rand = new Random();
            if (rand.nextDouble() < coldFuel / 3000d)
            {   double d0 = x + 0.5d;
                double d1 = y + 1.8d;
                double d2 = z + 0.5d;
                double d3 = (rand.nextDouble() - 0.5) / 4;
                double d4 = (rand.nextDouble() - 0.5) / 4;
                double d5 = (rand.nextDouble() - 0.5) / 4;
                level.addParticle(ParticleTypesInit.STEAM.get(), d0 + d3, d1 + d4, d2 + d5, 0.0D, 0.04D, 0.0D);
            }
            if (rand.nextDouble() < hotFuel / 3000d)
            {   double d0 = x + 0.5d;
                double d1 = y + 1.8d;
                double d2 = z + 0.5d;
                double d3 = (rand.nextDouble() - 0.5) / 2;
                double d4 = (rand.nextDouble() - 0.5) / 2;
                double d5 = (rand.nextDouble() - 0.5) / 2;
                SimpleParticleType particle = Math.random() < 0.5 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE;
                level.addParticle(particle, d0 + d3, d1 + d4, d2 + d5, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    void checkForFuel()
    {
        ItemStack fuelStack = this.getItems().get(0);
        if (!fuelStack.isEmpty())
        {   // Potion items
            List<MobEffectInstance> itemEffects = PotionUtils.getMobEffects(fuelStack);
            if (!itemEffects.isEmpty() && !itemEffects.equals(effects))
            {
                if (fuelStack.getItem() instanceof PotionItem)
                {   this.getItems().set(0, Items.GLASS_BOTTLE.getDefaultInstance());
                }
                else if (!fuelStack.hasCraftingRemainingItem() || fuelStack.getCount() > 1)
                {   fuelStack.shrink(1);
                }
                else
                {   this.getItems().set(0, fuelStack.getCraftingRemainingItem());
                }

                level.playSound(null, this.blockPos.getX(), this.blockPos.getY(), this.blockPos.getZ(), SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1, 1);
                effects.clear();
                effects.addAll(itemEffects.stream().map(eff -> eff.save(new CompoundTag())).map(MobEffectInstance::load).toList());
            }
            else if (fuelStack.is(Items.MILK_BUCKET))
            {
                this.getItems().set(0, fuelStack.getCraftingRemainingItem());
                level.playSound(null, this.blockPos.getX(), this.blockPos.getY(), this.blockPos.getZ(), SoundEvents.WITCH_DRINK, SoundSource.BLOCKS, 1, 1);
                effects.clear();
            }
            // Normal fuel items
            else
            {
                int itemFuel = getItemFuel(fuelStack);
                if (itemFuel != 0)
                {
                    int fuel = itemFuel > 0 ? hotFuel : coldFuel;
                    if (fuel < MAX_FUEL - Math.abs(itemFuel) * 0.75)
                    {
                        if (!fuelStack.hasCraftingRemainingItem() || fuelStack.getCount() > 1)
                        {   int consumeCount = Math.min((int) Math.floor((MAX_FUEL - fuel) / (double) Math.abs(itemFuel)), fuelStack.getCount());
                            fuelStack.shrink(consumeCount);
                            addFuel(itemFuel * consumeCount);
                        }
                        else
                        {   this.setItem(0, fuelStack.getCraftingRemainingItem());
                            addFuel(itemFuel);
                        }
                    }
                }
            }
        }
    }

    void insulatePlayer(Player player)
    {
        // Get the player's temperature
        player.getCapability(ModCapabilities.PLAYER_TEMPERATURE).ifPresent(cap ->
        {   double temp = cap.getTemp(Temperature.Type.WORLD);
            double min = cap.getTemp(Temperature.Type.FLOOR);
            double max = cap.getTemp(Temperature.Type.CEIL);

            // If the player is already insulated, check the input temperature reported by the HearthTempModifier
            Optional<HearthTempModifier> mod = Temperature.getModifier(player, Temperature.Type.WORLD, HearthTempModifier.class);
            if (mod.isPresent())
                temp = mod.get().getLastInput();

            // Tell the hearth to use hot fuel
            shouldUseHotFuel |= hotFuel > 0 && temp < ConfigSettings.MIN_TEMP.get() + min;
            // Tell the hearth to use cold fuel
            shouldUseColdFuel |= coldFuel > 0 && temp > ConfigSettings.MAX_TEMP.get() + max;

            if (shouldUseHotFuel || shouldUseColdFuel)
            {   int effectLevel = Math.min(9, (int) ((insulationLevel / (double) INSULATION_TIME) * 9));
                player.addEffect(new MobEffectInstance(ModEffects.INSULATION, 120, effectLevel, false, false, true));
            }

            effects.forEach(effect -> player.addEffect(new MobEffectInstance(effect.getEffect(), effect.getEffect() == MobEffects.NIGHT_VISION ? 399 : 119,
                                                                             effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon())));
        });
    }

    private boolean isCorrectSpreadDirectionForPipes(BlockPos bpos, BlockState fromState, SpreadPath newPath, Direction direction)
    {
        if (CREATE_LOADED)
        {
            Block block = fromState.getBlock();
            if (!(block instanceof FluidPipeBlock) && !(block instanceof GlassFluidPipeBlock))
            {   return true;
            }
            if ((block instanceof FluidPipeBlock && fromState.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction)))
            || (block instanceof GlassFluidPipeBlock && fromState.getValue(RotatedPillarBlock.AXIS) == direction.getAxis()))
            {   newPath.setOrigin(bpos);
                return true;
            }
            return false;
        }
        return true;
    }

    void updateNotifiedPaths()
    {
        // Reset cooldown
        this.rebuildCooldown = 100;

        ArrayList<SpreadPath> pathCopy = new ArrayList<>(paths);
        // Stream through paths, unfreeze them, and filter out the ones that aren't in the notify queue
        paths.stream().peek(path -> path.setFrozen(false)).filter(path -> notifyQueue.contains(path.getPos()))
        // Get all children of the paths
        .flatMap(path -> path.getAllChildren().stream()).peek(path ->
        {   BlockPos bpos = path.getPos();
            // Remove paths from lookup table as well
            Arrays.stream(Direction.values()).map(bpos::relative).toList().forEach(pathLookup::remove);
        })
        // Remove updated paths and their children
        .forEach(pathCopy::remove);
        paths = pathCopy;
        affectedBlocks = pathLookup;

        // Un-freeze paths so areas can be re-checked
        frozenPaths = 0;
        spreading = true;

        // Tell client to reset paths too
        if (!this.level.isClientSide)
        {   ColdSweatPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() ->
                                                (LevelChunk) level.getChunkSource().getChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4, ChunkStatus.FULL, true)),
                                                new HearthResetMessage(blockPos, notifyQueue));
        }

        notifyQueue.clear();
        forceRebuild = false;
    }

    LevelChunk getChunk(ChunkPos chunkPos)
    {
        LevelChunk chunk;
        // First try the previously accessed chunk
        if (chunkPos.equals(workingCoords) && workingChunk != null)
        {   chunk = workingChunk;
        }
        // Then search the cache of all previously accessed chunks
        else if ((chunk = loadedChunks.get(chunkPos)) == null)
        {   // If it isn't in the cache, get the chunk from the world
            loadedChunks.put(chunkPos, chunk = (LevelChunk) level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, true));
            workingCoords = chunkPos;
            workingChunk = chunk;
        }
        return chunk;
    }

    public List<MobEffectInstance> getEffects()
    {   return effects;
    }

    public static int getItemFuel(ItemStack item)
    {   return ConfigSettings.HEARTH_FUEL.get().getOrDefault(item.getItem(), 0d).intValue();
    }

    public int getHotFuel()
    {   return this.hotFuel;
    }

    public int getColdFuel()
    {   return this.coldFuel;
    }

    public void setHotFuel(int amount, boolean update)
    {
        this.hotFuel = CSMath.clamp(amount, 0, MAX_FUEL);

        if (amount <= 0 && hasHotFuel)
        {   hasHotFuel = false;
            level.playSound(null, blockPos, ModSounds.HEARTH_FUEL, SoundSource.BLOCKS, 1, (float) Math.random() * 0.2f + 0.9f);
        }
        else hasHotFuel = true;

        if (update) this.updateFuelState();
    }
    public void setHotFuelAndUpdate(int amount)
    {   setHotFuel(amount, true);
    }

    public void setColdFuel(int amount, boolean update)
    {
        this.coldFuel = CSMath.clamp(amount, 0, MAX_FUEL);

        if (amount <= 0 && hasColdFuel)
        {   hasColdFuel = false;
            level.playSound(null, blockPos, ModSounds.HEARTH_FUEL, SoundSource.BLOCKS, 1, (float) Math.random() * 0.2f + 0.9f);
        }
        else hasColdFuel = true;

        if (update) this.updateFuelState();
    }
    public void setColdFuelAndUpdate(int amount)
    {   setColdFuel(amount, true);
    }

    public void addFuel(int amount)
    {   if (amount > 0)
            setHotFuelAndUpdate(this.hotFuel + Math.abs(amount));
        else if (amount < 0)
            setColdFuelAndUpdate(this.coldFuel + Math.abs(amount));
    }

    public void updateFuelState()
    {
        if (level != null && !level.isClientSide)
        {   int hotFuel = this.getHotFuel();
            int coldFuel = this.getColdFuel();

            BlockState state = level.getBlockState(blockPos);
            int waterLevel = coldFuel == 0 ? 0 : (coldFuel < MAX_FUEL / 2 ? 1 : 2);
            int lavaLevel = hotFuel == 0 ? 0 : (hotFuel < MAX_FUEL / 2 ? 1 : 2);

            BlockState desiredState = state.setValue(HearthBottomBlock.WATER, waterLevel).setValue(HearthBottomBlock.LAVA, lavaLevel);
            if (state.getValue(HearthBottomBlock.WATER) != waterLevel || state.getValue(HearthBottomBlock.LAVA) != lavaLevel)
                level.setBlock(blockPos, desiredState, 3);

            this.setChanged();
        }
    }

    @Override
    public int getContainerSize()
    {   return SLOT_COUNT;
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInv)
    {   return new HearthContainer(id, playerInv, this);
    }

    @Override
    public void load(CompoundTag tag)
    {   super.load(tag);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        this.setColdFuelAndUpdate(tag.getInt("coldFuel"));
        this.setHotFuelAndUpdate(tag.getInt("hotFuel"));
        this.insulationLevel = tag.getInt("insulationLevel");
        this.loadEffects(tag);
        ContainerHelper.loadAllItems(tag, this.items);
    }

    @Override
    public void saveAdditional(CompoundTag tag)
    {   super.saveAdditional(tag);
        tag.putInt("coldFuel", this.getColdFuel());
        tag.putInt("hotFuel", this.getHotFuel());
        tag.putInt("insulationLevel", insulationLevel);
        this.saveEffects(tag);
        ContainerHelper.saveAllItems(tag, this.items);
    }

    void saveEffects(CompoundTag tag)
    {
        if (this.effects.size() > 0)
        {   ListTag list = new ListTag();
            for (MobEffectInstance effect : this.effects)
            {   list.add(effect.save(new CompoundTag()));
            }
            tag.put("Effects", list);
        }
    }

    void loadEffects(CompoundTag tag)
    {   this.effects.clear();
        if (tag.contains("Effects"))
        {   ListTag list = tag.getList("Effects", 10);
            for (int i = 0; i < list.size(); i++)
            {   this.effects.add(MobEffectInstance.load(list.getCompound(i)));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("HotFuel",  this.getHotFuel());
        tag.putInt("ColdFuel", this.getColdFuel());
        tag.putInt("InsulationLevel", insulationLevel);
        saveEffects(tag);

        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag)
    {
        super.handleUpdateTag(tag);
        this.setHotFuel(tag.getInt("HotFuel"), false);
        this.setColdFuel(tag.getInt("ColdFuel"), false);
        insulationLevel = tag.getInt("InsulationLevel");
        loadEffects(tag);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt)
    {   handleUpdateTag(pkt.getTag());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {   return ClientboundBlockEntityDataPacket.create(this);
    }

    public void replacePaths(ArrayList<SpreadPath> newPaths)
    {   paths = newPaths;
        pathLookup = newPaths.stream().map(SpreadPath::getPos).collect(HashSet::new, HashSet::add, HashSet::addAll);
        affectedBlocks = pathLookup;
        spreading = true;
        frozenPaths = 0;
        if (this.level.isClientSide)
        {   ClientOnlyHelper.addHearthPosition(this.blockPos);
        }
    }

    public void addPath(SpreadPath path)
    {   paths.add(path);
    }

    public void addPaths(Collection<SpreadPath> newPaths)
    {   paths.addAll(newPaths);
    }

    public boolean sendBlockUpdate(BlockPos pos)
    {
        if (notifyQueue.contains(pos))
            return false;

        if (pathLookup.contains(pos))
        {   notifyQueue.add(pos);
            return true;
        }
        for (Direction dir : Direction.values())
        {
            if (notifyQueue.contains(pos.relative(dir)))
            {   notifyQueue.add(pos);
                return true;
            }
        }
        return false;
    }

    public void forceUpdate(BlockPos pos)
    {   this.forceRebuild |= sendBlockUpdate(pos);
    }

    @Override
    public void setRemoved()
    {   super.setRemoved();
        HearthSaveDataHandler.HEARTH_POSITIONS.remove(this.blockPos);
        if (this.level.isClientSide)
        {   ClientOnlyHelper.removeHearthPosition(this.blockPos);
        }
    }

    public List<SpreadPath> getPaths()
    {   return this.displayPaths;
    }

    public Set<BlockPos> getPathLookup()
    {   return this.pathLookup;
    }
}