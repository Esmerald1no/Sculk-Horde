package com.github.sculkhoard.common.block;

import com.github.sculkhoard.common.block.BlockInfestation.SpreadingBlock;
import com.github.sculkhoard.common.block.BlockInfestation.SpreadingTile;
import com.github.sculkhoard.common.tileentity.InfectedDirtTile;
import com.github.sculkhoard.common.tileentity.InfestedStoneActiveTile;
import com.github.sculkhoard.core.BlockRegistry;
import com.github.sculkhoard.core.TileEntityRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.extensions.IForgeBlock;

import javax.annotation.Nullable;
import java.util.Random;

import static com.github.sculkhoard.core.SculkHoard.DEBUG_MODE;

public class InfestedStoneActiveBlock extends SpreadingBlock implements IForgeBlock {

    /**
     * MATERIAL is simply what the block is made up. This affects its behavior & interactions.<br>
     * MAP_COLOR is the color that will show up on a map to represent this block
     */
    public static Material MATERIAL = Material.STONE;
    public static MaterialColor MAP_COLOR = MaterialColor.COLOR_GRAY;

    /**
     * HARDNESS determines how difficult a block is to break<br>
     * 0.6f = dirt<br>
     * 1.5f = stone<br>
     * 2f = log<br>
     * 3f = iron ore<br>
     * 50f = obsidian
     */
    public static float HARDNESS = 1.5f;

    /**
     * BLAST_RESISTANCE determines how difficult a block is to blow up<br>
     * 0.5f = dirt<br>
     * 2f = wood<br>
     * 6f = cobblestone<br>
     * 1,200f = obsidian
     */
    public static float BLAST_RESISTANCE = 6f;

    /**
     * PREFERRED_TOOL determines what type of tool will break the block the fastest and be able to drop the block if possible
     */
    public static ToolType PREFERRED_TOOL = ToolType.PICKAXE;

    /**
     *  Harvest Level Affects what level of tool can mine this block and have the item drop<br>
     *
     *  -1 = All<br>
     *  0 = Wood<br>
     *  1 = Stone<br>
     *  2 = Iron<br>
     *  3 = Diamond<br>
     *  4 = Netherite
     */
    public static int HARVEST_LEVEL = 2;

    /**
     *  validSpreadBlocks is a list of blocks that this block can spread to.<br>
     *  DEFAULT_MAX_SPREAD_ATTEMPTS is the max number of spread attempts assigned
     *  to this block by default.<br>
     *  This only really applies to the root block because every child of the root
     *  block will have a smaller amount of spread attempts assigned to it by the
     *  immediate parent.
     */
    public static Block[] validSpreadBlocks = {Blocks.STONE};
   // public static int DEFAULT_MAX_SPREAD_ATTEMPTS = 10;

    /**
     * The Constructor that takes in properties
     * @param prop The Properties
     */
    public InfestedStoneActiveBlock(Properties prop) {
        super(prop);
    }


    /**
     * A simpler constructor that does not take in properties.<br>
     * I made this so that registering blocks in BlockRegistry.java can look cleaner
     */
    public InfestedStoneActiveBlock() {
        this(getProperties());
    }
    /**
     * Determines the properties of a block.<br>
     * I made this in order to be able to establish a block's properties from within the block class and not in the BlockRegistry.java
     * @return The Properties of the block
     */
    public static Properties getProperties()
    {
        return Properties.of(MATERIAL, MAP_COLOR)
                .strength(HARDNESS, BLAST_RESISTANCE)
                .harvestTool(PREFERRED_TOOL)
                .harvestLevel(HARVEST_LEVEL)
                .sound(SoundType.STONE);
    }


    /**
     * Returns what block state we want this block to convert into if converted back to original victim.
     * @return The BlockState this block will convert into
     */
    public BlockState getVictimVariant()
    {
        return Blocks.STONE.defaultBlockState();
    }

    /**
     * Returns what block state we want this block to convert into after its done spreading.
     * @return The BlockState this block will convert into
     */
    public BlockState getDormantVariant()
    {
        return BlockRegistry.INFESTED_STONE_DORMANT.get().defaultBlockState();
    }

    @Override
    public boolean isValidVictim(BlockState blockState)
    {
        //NOTE: I made this an if statement for the sake of efficiency
        if(blockState.getBlock() == Blocks.STONE)
        {
            return true;
        }
        return false;
    }

    /**
     * A function called by forge to create the tile entity.
     * @param state The current blockstate
     * @param world The world the block is in
     * @return Returns the tile entity.
     */
    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return TileEntityRegistry.INFESTED_STONE_ACTIVE_TILE.get().create();
    }


    /**
     * Just returns the tile entity
     * @param world The world to check
     * @param thisBlockPos The position to check
     * @return The tile entity
     */
    @Nullable
    public InfestedStoneActiveTile getTileEntity(World world, BlockPos thisBlockPos)
    {
        //Get tile entity for this block
        InfestedStoneActiveTile thisTile = (InfestedStoneActiveTile) world.getBlockEntity(thisBlockPos);

        if(thisTile == null || !(thisTile instanceof SpreadingTile))
            createTileEntity(world.getBlockState(thisBlockPos), world);

        return thisTile;
    }


}
