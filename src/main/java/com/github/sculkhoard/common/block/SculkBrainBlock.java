package com.github.sculkhoard.common.block;

import com.github.sculkhoard.common.tileentity.SculkBrainTile;
import com.github.sculkhoard.core.SculkHoard;
import com.github.sculkhoard.core.TileEntityRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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

/**
 * Chunk Loader Code created by SuperMartijn642
 */

public class SculkBrainBlock extends Block implements IForgeBlock {

    /**
     * MATERIAL is simply what the block is made up. This affects its behavior & interactions.<br>
     * MAP_COLOR is the color that will show up on a map to represent this block
     */
    public static Material MATERIAL = Material.PLANT;
    public static MaterialColor MAP_COLOR = CrustBlock.MAP_COLOR;

    /**
     * HARDNESS determines how difficult a block is to break<br>
     * 0.6f = dirt<br>
     * 1.5f = stone<br>
     * 2f = log<br>
     * 3f = iron ore<br>
     * 50f = obsidian
     */
    public static float HARDNESS = 50f;

    /**
     * BLAST_RESISTANCE determines how difficult a block is to blow up<br>
     * 0.5f = dirt<br>
     * 2f = wood<br>
     * 6f = cobblestone<br>
     * 1,200f = obsidian
     */
    public static float BLAST_RESISTANCE = 10f;

    /**
     * PREFERRED_TOOL determines what type of tool will break the block the fastest and be able to drop the block if possible
     */
    public static ToolType PREFERRED_TOOL = ToolType.SHOVEL;

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
    public static int HARVEST_LEVEL = 3;

    /**
     * The Constructor that takes in properties
     * @param prop The Properties
     */
    public SculkBrainBlock(Properties prop) {
        super(prop);
    }

    /**
     * A simpler constructor that does not take in properties.<br>
     * I made this so that registering blocks in BlockRegistry.java can look cleaner
     */
    public SculkBrainBlock() {
        this(getProperties());
    }

    /**
     * This function is called when this block is placed. <br>
     * Will set the nbt data maxSpreadAttempts.
     * @param world The world the block is in
     * @param bp The position the block is in
     * @param blockState The state of the block
     * @param entity The entity that placed it
     * @param itemStack The item stack it was placed from
     */
    @Override
    public void setPlacedBy(World world, BlockPos bp, BlockState blockState, @Nullable LivingEntity entity, ItemStack itemStack) {
        super.setPlacedBy(world, bp, blockState, entity, itemStack);
    }

    /**
     * Determines if this block will randomly tick or not.
     * @param blockState The current blockstate
     * @return True/False
     */
    @Override
    public boolean isRandomlyTicking(BlockState blockState) {
        return false;
    }

    /**
     * Gets called every time the block randomly ticks.
     * @param blockState The current Blockstate
     * @param serverWorld The current ServerWorld
     * @param bp The current Block Position
     * @param random ???
     */
    @Override
    public void randomTick(BlockState blockState, ServerWorld serverWorld, BlockPos bp, Random random) {

        boolean DEBUG_THIS = false;
        SculkHoard.entityFactory.addSculkAccumulatedMass(1);

    }

    /**
     * Returns the state that this block should transform into when right clicked by a tool.
     * For example: Used to determine if an axe can strip, a shovel can path, or a hoe can till.
     * Return null if vanilla behavior should be disabled.
     *
     * @param state The current state
     * @param world The world
     * @param pos The block position in world
     * @param player The player clicking the block
     * @param stack The stack being used by the player
     * @return The resulting state after the action has been performed
     */
    public BlockState getToolModifiedState(BlockState state, World world, BlockPos pos, PlayerEntity player, ItemStack stack, ToolType toolType)
    {
        if(DEBUG_MODE)
        {
            SculkBrainTile tile = getTileEntity(world, pos);

            System.out.println("Block at (" +
                    pos.getX() + ", " +
                    pos.getY() + ", " +
                    pos.getZ() + ") "
            );
        }

        return null; //Just Return null because We Are Not Modifying it
    }

    /**
     * Determines if a specified mob type can spawn on this block, returning false will
     * prevent any mob from spawning on the block.
     *
     * @param state The current state
     * @param world The current world
     * @param pos Block position in world
     * @param type The Mob Category Type
     * @return True to allow a mob of the specified category to spawn, false to prevent it.
     */
    public boolean canCreatureSpawn(BlockState state, IBlockReader world, BlockPos pos, EntitySpawnPlacementRegistry.PlacementType type, EntityType<?> entityType)
    {
        return false;
    }

    /**
     * Determines the properties of a block.<br>
     * I made this in order to be able to establish a block's properties from within the block class and not in the BlockRegistry.java
     * @return The Properties of the block
     */
    public static Properties getProperties()
    {
        Properties prop = Properties.of(MATERIAL, MAP_COLOR)
                .strength(HARDNESS, BLAST_RESISTANCE)
                .harvestTool(PREFERRED_TOOL)
                .harvestLevel(HARVEST_LEVEL)
                .sound(SoundType.GRASS);
        return prop;
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
        return TileEntityRegistry.SCULK_BRAIN_TILE.get().create();
    }

    /**
     * Returns If true we have a tile entity
     * @param state The current block state
     * @return True
     */
    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    /**
     * Just returns the tile entity
     * @param world The world to check
     * @param thisBlockPos The position to check
     * @return The tile entity
     */
    public SculkBrainTile getTileEntity(World world, BlockPos thisBlockPos)
    {
        //Get tile entity for this block
        TileEntity tileEntity = world.getBlockEntity(thisBlockPos);
        SculkBrainTile thisTile = null;
        if(thisTile == null)
        {
            if(!(thisTile instanceof SculkBrainTile))
            {
                thisTile = (SculkBrainTile) world.getBlockEntity(thisBlockPos);
            }
            else
            {
                System.out.println("Error: Tile of wrong instance.");
            }
        }
        else
        {
            System.out.println("Error: Tile is null.");
        }

        return thisTile;
    }

    @Override
    public void onPlace(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving){
        TileEntity tile = worldIn.getBlockEntity(pos);
        if(tile instanceof SculkBrainTile)
            ((SculkBrainTile)tile).loadAllChunks();
    }

    @Override
    public void onRemove(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving){
        TileEntity tile = worldIn.getBlockEntity(pos);
        if(tile instanceof SculkBrainTile)
            ((SculkBrainTile)tile).unloadAllChunks();
        super.onRemove(state, worldIn, pos, newState, isMoving);
    }



}