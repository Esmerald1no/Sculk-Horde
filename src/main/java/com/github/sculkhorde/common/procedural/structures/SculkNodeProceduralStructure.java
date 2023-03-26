package com.github.sculkhorde.common.procedural.structures;

import com.github.sculkhorde.util.BlockAlgorithms;
import com.github.sculkhorde.core.BlockRegistry;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.Optional;

public class SculkNodeProceduralStructure extends ProceduralStructure
{
    private final int SHELL_RADIUS = 5;
    private final int CAVE_RADIUS = SHELL_RADIUS * 2;
    private final int HALLWAY_RADIUS = SHELL_RADIUS;
    public SculkNodeProceduralStructure(ServerWorld worldIn, BlockPos originIn)
    {
        super(worldIn, originIn);
    }

    @Override
    public void buildTick()
    {
        super.buildTick();

        //Build Child Structures
        for(ProceduralStructure childStructure : childStructuresQueue)
        {
            childStructure.buildTick();
        }
    }

    public Optional<BlockPos> findLivingRockStructureIfExists(ServerWorld world, BlockPos placementPos)
    {
        if(world.getBlockState(placementPos).getBlock().equals(BlockRegistry.SCULK_LIVING_ROCK_ROOT_BLOCK.get()))
        {
            return Optional.of(placementPos);
        }

        int offsetBelow = 0;
        int offsetAbove = 0;

        //Search all the way to bedrock for the structure
        while(placementPos.offset(0, offsetBelow, 0).getY() > 0)
        {
            if(world.getBlockState(placementPos.offset(0, offsetBelow, 0)).getBlock().equals(BlockRegistry.SCULK_LIVING_ROCK_ROOT_BLOCK.get()))
            {
                return Optional.of(placementPos.offset(0, offsetBelow, 0));
            }
            offsetBelow --;
        }

        while(placementPos.offset(0, offsetAbove, 0).getY() > world.getHeight())
        {
            if(world.getBlockState(placementPos.offset(0, offsetAbove, 0)).getBlock().equals(BlockRegistry.SCULK_LIVING_ROCK_ROOT_BLOCK.get()))
            {
                return Optional.of(placementPos.offset(0, offsetAbove, 0));
            }
            offsetAbove ++;
        }
        return Optional.empty();
    }

    public BlockPos findLivingRockPlacementPosition(ServerWorld world, BlockPos placementPos)
    {
        if(findLivingRockStructureIfExists(world, placementPos).isPresent())
            return findLivingRockStructureIfExists(world, placementPos).get();

        int attempts = 0;
        int MAX_ATTEMPTS = 100;
        int offsetBelow = 0;
        int offsetAbove = 0;
        //Try and find solid ground to place this block on
        while(world.getBlockState(placementPos.offset(0, offsetBelow, 0)).canBeReplaced(Fluids.WATER) && attempts <= MAX_ATTEMPTS)
        {
            offsetBelow --;
            attempts++;
        }
        attempts = 0;
        while(world.getBlockState(placementPos.offset(0, offsetAbove, 0)).canBeReplaced(Fluids.WATER) && attempts <= MAX_ATTEMPTS)
        {
            offsetAbove ++;
            attempts++;
        }

        if(Math.abs(offsetBelow) < offsetAbove)
        {
            return placementPos.offset(0, offsetBelow, 0);
        }
        return placementPos.offset(0, offsetAbove, 0);
    }

    /**
     * This method fills the building queue with what blocks should
     * be placed down.
     */
    @Override
    public void generatePlan()
    {

        this.plannedBlockQueue.clear();

        childStructuresQueue.add(new SculkNodeCaveProceduralStructure(this.world, this.origin, 10));

        // Add 4 SculkNodeCaveHallway Procedural Structure going in all 4 directions. Each one starts at the respective
        // edge of the cave structure depending on the direction. The length should be 64 blocks.
        /**
         *  Add 4 SculkNodeCaveHallway Procedural Structure going in all 4 directions.
         *  Each one starts at the respective edge of the cave structure depending on the direction.
         *  I.E. The North cave hallway starts at the North-most block of the cave structure and
         *  goes North 64 blocks.
         *  The length should be 64 blocks.
         */
        //childStructuresQueue.add(new SculkNodeCaveHallwayProceduralStructure(this.world, this.origin.offset(0, 0, -SHELL_RADIUS), this.origin.offset(0, 0, -SHELL_RADIUS - 64), 5));

        for(ProceduralStructure entry : childStructuresQueue)
        {
            entry.generatePlan();
        }

        ArrayList<BlockPos> blockPositionsInCircle = BlockAlgorithms.getBlockPosInCircle(this.origin, SHELL_RADIUS, false);

        for(BlockPos position : blockPositionsInCircle)
        {
            //I dont know why i have to do -1 for the radius, something is wrong with the math
            if(BlockAlgorithms.getBlockDistance(this.origin, position) < SHELL_RADIUS - 1)
            {
                plannedBlockQueue.add(new PlannedBlock(this.world, BlockRegistry.SCULK_ARACHNOID.get().defaultBlockState(), position));
            }
            else
            {
                plannedBlockQueue.add(new PlannedBlock(this.world, BlockRegistry.SCULK_DURA_MATTER.get().defaultBlockState(), position));
            }
        }

        ArrayList<BlockPos> surroundingLivingRock = BlockAlgorithms.getPointsOnCircumference(this.origin, 5, SHELL_RADIUS *3);
        surroundingLivingRock.addAll(BlockAlgorithms.getPointsOnCircumference(this.origin, 10, SHELL_RADIUS *6));
        surroundingLivingRock.addAll(BlockAlgorithms.getPointsOnCircumference(this.origin, 20, SHELL_RADIUS *9));
        for(BlockPos position : surroundingLivingRock)
        {
            plannedBlockQueue.add(new PlannedBlock(this.world, BlockRegistry.SCULK_LIVING_ROCK_ROOT_BLOCK.get().defaultBlockState(), findLivingRockPlacementPosition(this.world, position)));
        }
    }
}
