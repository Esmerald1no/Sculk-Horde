package com.github.sculkhorde.common.entity.infection;

import com.github.sculkhorde.core.ModConfig;
import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.util.BlockAlgorithms;
import com.github.sculkhorde.util.TickUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.concurrent.TimeUnit;

/** This Entity is used to traverse the world and infect blocks.
 * Once spawned, it will use breadth-first search to find the nearest block to infect.
 * Once it has found a block to infect, it will infect it and then move on to the next block.
 * This will continue until it has either reached its max distance or max infections.
 */
public abstract class CursorEntity extends Entity
{
    protected enum State
    {
        IDLE,
        SEARCHING,
        EXPLORING,
        FINISHED
    }


    protected State state = State.IDLE;

    protected int MAX_TRANSFORMATIONS = 100;
    protected int currentTransformations = 0;
    protected int MAX_RANGE = 20;
    protected long MAX_LIFETIME_MILLIS = TimeUnit.SECONDS.toMillis(60 * 5);
    protected long creationTickTime = System.currentTimeMillis();
    protected long lastTickTime = 0;

    protected long ticksRemainingBeforeCheckingIfInCursorList = 0;
    protected final long CHECK_DELAY_TICKS = TickUnits.convertSecondsToTicks(5);
    protected boolean canBeManuallyTicked = true;

    protected int searchIterationsPerTick = 20;
    protected long tickIntervalMilliseconds = 1000;

    protected BlockPos origin = BlockPos.ZERO;
    protected BlockPos target = BlockPos.ZERO;
    protected HashMap<Long, Boolean> positionsSearched = new HashMap<>();
    Queue<BlockPos> searchQueue = new LinkedList<>();
    public boolean isSuccessful = false;

    //Create a hash map to store all visited nodes
    protected HashMap<Long, Boolean> visitedPositons = new HashMap<>();



    public CursorEntity(EntityType<?> pType, Level pLevel) {
        super(pType, pLevel);
        creationTickTime = System.currentTimeMillis();
    }

    public void setMaxTransformations(int MAX_INFECTIONS) {
        this.MAX_TRANSFORMATIONS = MAX_INFECTIONS;
    }

    public void setMaxRange(int MAX_RANGE) {
        this.MAX_RANGE = MAX_RANGE;
    }

    public void setMaxLifeTimeMillis(long MAX_LIFETIME) {
        this.MAX_LIFETIME_MILLIS = MAX_LIFETIME;
    }

    public void setSearchIterationsPerTick(int iterations) {
        this.searchIterationsPerTick = iterations;
    }

    public void setTickIntervalMilliseconds(long milliseconds) {
        this.tickIntervalMilliseconds = milliseconds;
    }

    public void setCanBeManuallyTicked(boolean value) { canBeManuallyTicked = value; }

    public boolean canBeManuallyTicked() { return canBeManuallyTicked; }

    public void setState(State state)
    {
        this.state = state;
    }


    /**
     * Returns true if the block is considered obstructed.
     * @param state the block state
     * @param pos the block position
     * @return true if the block is considered obstructed
     */
    protected boolean isObstructed(BlockState state, BlockPos pos)
    {
        if(SculkHorde.savedData.getSculkAccumulatedMass() <= 0)
        {
            return false;
        }

        if(BlockAlgorithms.getBlockDistance(origin, pos) > MAX_RANGE)
        {
            return true;
        }
        else if(state.isAir())
        {
            return true;
        }
        // This is to prevent the entity from getting stuck in a loop
        else if(visitedPositons.containsKey(pos.asLong()))
        {
            return true;
        }
        return false;
    }

    /**
     * Returns true if the block is considered a target.
     * @param pos the block position
     * @return true if the block is considered a target
     */
    protected boolean isTarget(BlockPos pos)
    {
        return false;
    }

    /**
     * Transforms the block at the given position.
     * @param pos the position of the block
     */
    protected void transformBlock(BlockPos pos)
    {
        level().setBlockAndUpdate(pos, Blocks.DIAMOND_BLOCK.defaultBlockState());
    }

    protected void spawnParticleEffects()
    {
        //this.level().addParticle(ParticleTypes.TOTEM_OF_UNDYING, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), 0.0D, 0.1D, 0.0D);
    }

    protected void resetSearchTick()
    {
        searchQueue.clear();
        positionsSearched.clear();
    }

    protected void addPositionToQueueIfValid(BlockPos pos)
    {
        boolean isPositionNotVisited = !positionsSearched.containsKey(pos.asLong());
        BlockState neighborBlockState = level().getBlockState(pos);
        boolean isPositionNotObstructed = !isObstructed(neighborBlockState, pos);

        // If not visited and is a valid block to navigate
        if (isPositionNotVisited && isPositionNotObstructed) {
            searchQueue.add(pos);
            positionsSearched.put(pos.asLong(), true);
        }
    }

    /**
     * Use Breadth-First Search to find the nearest infectable block within a certain maximum distance.
     * @return true if complete. false if not complete.
     */
    protected boolean searchTick() {
        // Initialize the visited positions map and the queue
        // Complete 20 times.
        for (int i = 0; i < Math.max(searchIterationsPerTick, 1); i++)
        {
            // Breadth-First Search

            if (searchQueue.isEmpty()) {
                isSuccessful = false;
                target = BlockPos.ZERO;
                return true;
            }

            BlockPos currentBlock = searchQueue.poll();

            // If the current block is a target, return it
            if (isTarget(currentBlock)) {
                isSuccessful = true;
                target = currentBlock;
                return true;
            }

            // Get all possible directions
            ArrayList<BlockPos> possibleBlocksToVisit = BlockAlgorithms.getNeighborsCube(currentBlock, false);
            Collections.shuffle(possibleBlocksToVisit);

            // Add all neighbors to the queue
            for (BlockPos neighbor : possibleBlocksToVisit) {
                addPositionToQueueIfValid(neighbor);
            }
        }

        return false;
    }

    public void exploreTick()
    {
        // Get Neighbors of Each Block
        ArrayList<BlockPos> neighbors = BlockAlgorithms.getNeighborsCube(this.blockPosition(), false);
        // Create a new list to store unobstructed neighbors
        ArrayList<BlockPos> unobstructedNeighbors = new ArrayList<>();
        // Check each neighbor for obstructions and add unobstructed neighbors to the new list
        for (BlockPos neighbor : neighbors)
        {
            if (!isObstructed(level().getBlockState(neighbor), neighbor)) {
                unobstructedNeighbors.add(neighbor);
            }
        }

        // If there are no non-obstructed neighbors, return
        if (neighbors.size() == 0) {
            return;
        }

        // Find the block that is closest to target in neighbors
        BlockPos closest = neighbors.get(0);
        for (BlockPos pos : neighbors)
        {
            if (BlockAlgorithms.getBlockDistance(pos, target) < BlockAlgorithms.getBlockDistance(closest, target)) {
                closest = pos;
            }
        }


        // Move to the closest block
        this.setPos(closest.getX() + 0.5, closest.getY(), closest.getZ() + 0.5);

        // If we've reached the target block, find a new target
        if (this.blockPosition().equals(target))
        {
            target = BlockPos.ZERO;
            BlockState stateOfCurrentBlock = level().getBlockState(this.blockPosition());

            boolean isTarget = isTarget(this.blockPosition());
            boolean isNotObstructed = !isObstructed(stateOfCurrentBlock, this.blockPosition());
            // If the block is not obstructed, infect it
            if(isTarget && isNotObstructed)
            {
                // Infect the block and increase the infection count
                transformBlock(this.blockPosition());
                currentTransformations++;
            }

            setState(State.SEARCHING);
            resetSearchTick();
            searchQueue.add(this.blockPosition());
        }

        // Mark position as visited
        visitedPositons.put(closest.asLong(), true);
    }

    public void cursorTick()
    {
        float timeElapsedMilliSeconds = System.currentTimeMillis() - lastTickTime;
        double tickIntervalMillisecondsAfterMultiplier = tickIntervalMilliseconds - (tickIntervalMilliseconds * (ModConfig.SERVER.infestation_speed_multiplier.get()));
        if (timeElapsedMilliSeconds < Math.max(tickIntervalMillisecondsAfterMultiplier, 1)) {
            return;
        }

        lastTickTime = System.currentTimeMillis();

        // Play Particles on Client
        if (this.level() != null && this.level().isClientSide) {
            for (int i = 0; i < 2; ++i) {
                spawnParticleEffects();
            }
            return;
        }

        // Keep track of the origin
        if (origin == BlockPos.ZERO)
        {
            origin = this.blockPosition();
        }

        long currentLifeTimeMilliseconds = System.currentTimeMillis() - creationTickTime;

        // Convert to seconds
        // If entity has lived too long, remove it
        if (currentLifeTimeMilliseconds >= MAX_LIFETIME_MILLIS)
        {
            setState(State.FINISHED);
        }
        else if (currentTransformations >= MAX_TRANSFORMATIONS)
        {
            setState(State.FINISHED);
        }

        if(state == State.IDLE)
        {
            searchQueue.add(this.blockPosition());
            setState(State.SEARCHING);
        }
        else if (state == State.SEARCHING)
        {

            // IF not complete, just return;
            if(!searchTick())
            {
                return;
            }

            // If we can't find a target, finish
            if (target.equals(BlockPos.ZERO)) {
                setState(state = State.FINISHED);
            }
            else // If we find target, start infecting
            {
                setState(state = State.EXPLORING);
                visitedPositons.clear();
            }
        }
        else if (state == State.EXPLORING)
        {
            exploreTick();
        }
        else if (state == State.FINISHED)
        {
            this.remove(RemovalReason.DISCARDED);
        }
    }

    @Override
    public void tick() {
        super.tick();


        if(canBeManuallyTicked())
        {
            ticksRemainingBeforeCheckingIfInCursorList--;

            if(ticksRemainingBeforeCheckingIfInCursorList <= 0)
            {
                SculkHorde.cursorHandler.computeIfAbsent(this);
                ticksRemainingBeforeCheckingIfInCursorList = CHECK_DELAY_TICKS;
            }
        }
        boolean canBeManuallyTickedAndManualControlIsNotOn = (canBeManuallyTicked() && !SculkHorde.cursorHandler.isManualControlOfTickingEnabled());
        boolean cannotBeManuallyTicked = !canBeManuallyTicked();

        boolean shouldTick = canBeManuallyTickedAndManualControlIsNotOn || cannotBeManuallyTicked;

        if(shouldTick) {
            cursorTick();
        }

    }


    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     *
     * @param nbt
     */
    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        /*
        nbt.putInt("MAX_TRANSFORMATIONS", MAX_TRANSFORMATIONS);
        nbt.putInt("currentTransformations", currentTransformations);
        nbt.putInt("MAX_RANGE", MAX_RANGE);
        nbt.putLong("MAX_LIFETIME_MILLIS", MAX_LIFETIME_MILLIS);
        nbt.putLong("creationTickTime", creationTickTime);
        nbt.putLong("lastTickTime", lastTickTime);
        nbt.putLong("ticksRemainingBeforeCheckingIfInCursorList", ticksRemainingBeforeCheckingIfInCursorList);
        nbt.putLong("searchIterationsPerTick", searchIterationsPerTick);
        nbt.putLong("tickIntervalMilliseconds", tickIntervalMilliseconds);

        int stateValue;
        switch (state)
        {
            case SEARCHING -> stateValue = 1;
            case EXPLORING -> stateValue = 2;
            case FINISHED -> stateValue = 3;
            default -> stateValue = 0;
        }
        nbt.putInt("state", stateValue);

         */

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        /*
        MAX_TRANSFORMATIONS = nbt.getInt("MAX_TRANSFORMATIONS");
        currentTransformations = nbt.getInt("currentTransformations");
        MAX_RANGE = nbt.getInt("MAX_RANGE");
        MAX_LIFETIME_MILLIS = nbt.getLong("MAX_LIFETIME_MILLIS");
        creationTickTime = nbt.getLong("creationTickTime");
        lastTickTime = nbt.getLong("lastTickTime");
        ticksRemainingBeforeCheckingIfInCursorList = nbt.getLong("ticksRemainingBeforeCheckingIfInCursorList");
        searchIterationsPerTick = nbt.getInt("searchIterationsPerTick");
        tickIntervalMilliseconds = nbt.getLong("tickIntervalMilliseconds");

        State stateValue;
        switch (nbt.getInt("state"))
        {
            case 1 -> stateValue = State.SEARCHING;
            case 2 -> stateValue = State.EXPLORING;
            case 3 -> stateValue = State.FINISHED;
            default -> stateValue = State.IDLE;
        }
        state = State.IDLE;

         */
    }

    @Override
    protected void defineSynchedData() {

    }

    public void setTarget(BlockPos target) {
        this.target = target;
    }

    @Override
    public void onRemovedFromWorld() {
        if(level().isClientSide()) { return; }
    }

    public void chanceToThanosSnapThisCursor()
    {
        if(ModConfig.SERVER.thanos_snap_cursors_after_reaching_threshold.get() && SculkHorde.cursorHandler.isManualControlOfTickingEnabled())
        {
            if( random.nextBoolean())
            {
                discard();
            }
        }
    }
}
