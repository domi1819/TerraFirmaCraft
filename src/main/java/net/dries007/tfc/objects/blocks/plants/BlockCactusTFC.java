/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.objects.blocks.plants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;

import net.dries007.tfc.api.capability.size.Size;
import net.dries007.tfc.api.capability.size.Weight;
import net.dries007.tfc.api.types.Plant;
import net.dries007.tfc.objects.blocks.BlocksTFC;
import net.dries007.tfc.world.classic.CalenderTFC;
import net.dries007.tfc.world.classic.ClimateTFC;
import net.dries007.tfc.world.classic.chunkdata.ChunkDataTFC;

@ParametersAreNonnullByDefault
public class BlockCactusTFC extends BlockStackPlantTFC implements IGrowable
{
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 15);
    private static final AxisAlignedBB CACTUS_COLLISION_AABB = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 0.9375D, 0.9375D);
    private static final Map<Plant, BlockCactusTFC> MAP = new HashMap<>();

    public static BlockCactusTFC get(Plant plant)
    {
        return MAP.get(plant);
    }

    public BlockCactusTFC(Plant plant)
    {
        super(plant);
        if (MAP.put(plant, this) != null) throw new IllegalStateException("There can only be one.");

        setSoundType(SoundType.GROUND);
        setHardness(0.25F);
        this.setDefaultState(this.blockState.getBaseState().withProperty(GROWTHSTAGE, CalenderTFC.Month.MARCH.id()).withProperty(PART, EnumBlockPart.SINGLE));
    }

    @Override
    public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient)
    {
        int i;
        for (i = 1; worldIn.getBlockState(pos.down(i)).getBlock() == this; ++i) {}
        return i < 3 && worldIn.isAirBlock(pos.up()) && canBlockStay(worldIn, pos.up(), state);
    }

    @Override
    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state)
    {
        return true;
    }

    @Override
    public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state)
    {
        worldIn.setBlockState(pos.up(), this.getDefaultState());
        IBlockState iblockstate = state.withProperty(DAYPERIOD, getCurrentTime(worldIn)).withProperty(AGE, 0).withProperty(GROWTHSTAGE, CalenderTFC.getMonthOfYear().id()).withProperty(PART, getPlantPart(worldIn, pos));
        worldIn.setBlockState(pos, iblockstate);
        iblockstate.neighborChanged(worldIn, pos.up(), this, pos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState)
    {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, CACTUS_COLLISION_AABB.offset(state.getOffset(worldIn, pos)));
    }

    @Override
    public void onEntityCollision(World worldIn, BlockPos pos, IBlockState state, Entity entityIn)
    {
        entityIn.attackEntityFrom(DamageSource.CACTUS, 1.0F);
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(AGE);
    }

    @Override
    @Nonnull
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos)
    {
        return state.withProperty(GROWTHSTAGE, CalenderTFC.getMonthOfYear().id()).withProperty(PART, getPlantPart(worldIn, pos));
    }

    @Override
    @Nonnull
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, AGE, GROWTHSTAGE, PART, DAYPERIOD);
    }

    @Override
    @Nonnull
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        return FULL_BLOCK_AABB.offset(state.getOffset(source, pos));
    }

    @Override
    @Nonnull
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(AGE, meta).withProperty(GROWTHSTAGE, CalenderTFC.getMonthOfYear().id());
    }

    @Override
    public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos)
    {
        return false;
    }

    @Override
    public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random random)
    {
        if (!worldIn.isAreaLoaded(pos, 1)) return;
        int currentStage = state.getValue(GROWTHSTAGE);
        int expectedStage = CalenderTFC.getMonthOfYear().id();
        int currentTime = state.getValue(DAYPERIOD);
        int expectedTime = getCurrentTime(worldIn);

        if (currentTime != expectedTime)
        {
            worldIn.setBlockState(pos, state.withProperty(DAYPERIOD, expectedTime).withProperty(GROWTHSTAGE, currentStage).withProperty(PART, getPlantPart(worldIn, pos)));
        }
        if (currentStage != expectedStage && random.nextDouble() < 0.5)
        {
            worldIn.setBlockState(pos, state.withProperty(DAYPERIOD, expectedTime).withProperty(GROWTHSTAGE, expectedStage).withProperty(PART, getPlantPart(worldIn, pos)));
        }
        this.updateTick(worldIn, pos, state, random);
    }

    @Override
    @Nonnull
    public Block.EnumOffsetType getOffsetType()
    {
        return EnumOffsetType.XYZ;
    }

    @Override
    public Size getSize(ItemStack stack)
    {
        return Size.NORMAL;
    }

    @Override
    public Weight getWeight(ItemStack stack)
    {
        return Weight.HEAVY;
    }

    @Override
    protected boolean canSustainBush(IBlockState state)
    {
        return BlocksTFC.isSand(state);
    }

    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {
        if (!worldIn.isAreaLoaded(pos, 1)) return;

        if (ClimateTFC.getHeightAdjustedBiomeTemp(worldIn, pos) > 20 && plant.isValidSunlight(worldIn.getLightFromNeighbors(pos.up())) && canGrow(worldIn, pos, state, worldIn.isRemote))
        {
            int j = state.getValue(AGE);

            if (rand.nextFloat() < getGrowthRate(worldIn, pos) && net.minecraftforge.common.ForgeHooks.onCropsGrowPre(worldIn, pos.up(), state, true))
            {
                if (j == 15)
                {
                    grow(worldIn, rand, pos, state);
                }
                else
                {
                    worldIn.setBlockState(pos, state.withProperty(DAYPERIOD, getCurrentTime(worldIn)).withProperty(AGE, j + 1).withProperty(PART, getPlantPart(worldIn, pos)));
                }
                net.minecraftforge.common.ForgeHooks.onCropsGrowPost(worldIn, pos, state, worldIn.getBlockState(pos));
            }
        }

        if (!canBlockStay(worldIn, pos, state))
        {
            worldIn.setBlockToAir(pos);
        }
    }

    @Override
    public boolean canBlockStay(World worldIn, BlockPos pos, IBlockState state)
    {
        IBlockState soil = worldIn.getBlockState(pos.down());
        if (state.getBlock() == this)
        {
            for (EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL)
            {
                IBlockState blockState = worldIn.getBlockState(pos.offset(enumfacing));
                Material material = blockState.getMaterial();

                if (material.isSolid() || material == Material.LAVA)
                {
                    return blockState.getBlock() == this;
                }
            }

            return soil.getBlock().canSustainPlant(soil, worldIn, pos.down(), net.minecraft.util.EnumFacing.UP, this) && plant.isValidTempRain(ClimateTFC.getHeightAdjustedBiomeTemp(worldIn, pos), ChunkDataTFC.getRainfall(worldIn, pos));
        }
        return this.canSustainBush(soil);
    }

    @Override
    @Nonnull
    public EnumPlantType getPlantType(net.minecraft.world.IBlockAccess world, BlockPos pos)
    {
        return EnumPlantType.Desert;
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos)
    {
        return super.canPlaceBlockAt(worldIn, pos) && this.canBlockStay(worldIn, pos, worldIn.getBlockState(pos));
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos)
    {
        if (!this.canBlockStay(worldIn, pos, state))
        {
            worldIn.destroyBlock(pos, true);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos)
    {
        return CACTUS_COLLISION_AABB.offset(blockState.getOffset(worldIn, pos));
    }
}
