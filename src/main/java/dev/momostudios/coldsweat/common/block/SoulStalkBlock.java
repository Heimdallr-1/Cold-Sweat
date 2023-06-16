package dev.momostudios.coldsweat.common.block;

import dev.momostudios.coldsweat.api.util.Temperature;
import dev.momostudios.coldsweat.core.itemgroup.ColdSweatGroup;
import dev.momostudios.coldsweat.config.ConfigSettings;
import dev.momostudios.coldsweat.util.math.CSMath;
import dev.momostudios.coldsweat.util.registries.ModBlocks;
import dev.momostudios.coldsweat.util.registries.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.IPlantable;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class SoulStalkBlock extends Block implements IPlantable
{
    public static final IntegerProperty AGE = BlockStateProperties.AGE_15;
    public static final IntegerProperty SECTION = IntegerProperty.create("section", 0, 3);
    protected static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);
    
    public SoulStalkBlock(Properties p_49795_)
    {
        super(p_49795_);
        this.registerDefaultState(this.defaultBlockState().setValue(AGE, 0).setValue(SECTION, 0));
    }

    public static Properties getProperties()
    {
        return Properties
                .of(Material.PLANT)
                .sound(SoundType.BIG_DRIPLEAF)
                .strength(0f, 0.5f)
                .randomTicks()
                .lightLevel(state -> state.getValue(SECTION) == 3 ? 6 : 0)
                .noOcclusion()
                .noCollission();
    }

    public static Item.Properties getItemProperties()
    {
        return new Item.Properties().tab(ColdSweatGroup.COLD_SWEAT);
    }

    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, Random rand)
    {
        if (level.isEmptyBlock(pos.above()))
        {
            // Get the height of the plant
            int i;
            for(i = 1; level.getBlockState(pos.below(i)).is(this); ++i)
            {}

            if (i < 6 && rand.nextDouble() < 0.05 + CSMath.blend(ConfigSettings.MIN_TEMP.get(), ConfigSettings.MAX_TEMP.get(), Temperature.getTemperatureAt(pos.below(i - 1), level), 0, 0.95))
            {
                int j = state.getValue(AGE);
                if (ForgeHooks.onCropsGrowPre(level, pos, state, true))
                {
                    if (j >= 8)
                    {
                        level.setBlockAndUpdate(pos.above(), this.defaultBlockState().setValue(SECTION, 3));
                        level.setBlock(pos, state.setValue(AGE, 0).setValue(SECTION, rand.nextInt(2) + 1), 4);
                    }
                    else level.setBlock(pos, state.setValue(AGE, j + 1), 4);
                }
            }
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        BlockPos pos = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(pos.below()).is(this) ? this.defaultBlockState().setValue(SECTION, 3)
                : context.getLevel().getBlockState(pos.above()).isAir() && this.canSurvive(this.defaultBlockState(), context.getLevel(), pos) ? this.defaultBlockState()
                : null;
        return state;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState lastState, boolean p_60570_)
    {
        if (level.getBlockState(pos.below()).is(BlockTags.SOUL_FIRE_BASE_BLOCKS))
        {
            if (level.getBlockState(pos.above()).isAir())
            {   level.setBlock(pos.above(), ModBlocks.SOUL_STALK.defaultBlockState().setValue(SECTION, 3), 3);
            }
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState otherState, LevelAccessor ilevel, BlockPos pos, BlockPos otherPos)
    {
        if (!this.canSurvive(state, ilevel, pos))
        {
            return Blocks.AIR.defaultBlockState();
        }

        if (direction == Direction.UP)
        {
            if (!otherState.is(this))
            {   return Blocks.AIR.defaultBlockState();
            }
            else if (state.getValue(SECTION) == 3)
            {   return state.setValue(SECTION, new Random().nextInt(2) + 1);
            }
        }
        return state;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter getter, BlockPos pos, CollisionContext context)
    {   return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {   builder.add(AGE, SECTION);
    }

    @Override
    public BlockState getPlant(BlockGetter level, BlockPos pos)
    {   return this.defaultBlockState();
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        BlockState below = level.getBlockState(pos.below());
        return below.is(BlockTags.SOUL_FIRE_BASE_BLOCKS) || below.is(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder)
    {
        List<ItemStack> drops = super.getDrops(state, builder);
        if (!drops.isEmpty())
            return drops;

        int section = state.getValue(SECTION);
        if (section == 2 || section == 3)
            drops.add(new ItemStack(ModItems.SOUL_SPROUT, 1));
        if (section != 0 && section != 3)
            drops.add(new ItemStack(Items.STICK, new Random().nextInt(3)));
        return drops;
    }
}
