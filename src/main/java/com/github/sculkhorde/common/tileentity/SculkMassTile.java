package com.github.sculkhorde.common.tileentity;

import com.github.sculkhorde.common.entity.infection.CursorSurfaceInfectorEntity;
import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.core.TileEntityRegistry;
import com.github.sculkhorde.core.gravemind.entity_factory.EntityFactory;
import com.github.sculkhorde.core.gravemind.entity_factory.ReinforcementRequest;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class SculkMassTile extends BlockEntity {

    /**
     * storedSculkMass is the value of sculk mass was this block has.
     * This value is used to determine the mobs that are spawened or the area
     * that will be infected by the sculk.
     * storedSculkMassIdentifier is the string used to identify storedSculkMass
     * in CompoundNBT. It allows us to read/write to it.<br>
     */
    private int storedSculkMass = 0;
    private String storedSculkMassIdentifier = "storedSculkMass";


    /**
     * The Constructor that takes in properties
     * @param type The Tile Entity Type
     */
    public SculkMassTile(BlockPos pos, BlockState state) {
        super(TileEntityRegistry.SCULK_MASS_TILE.get(), pos, state);
    }

    /**
     * ???
     * @param blockState The blocks current blockstate
     * @param compoundNBT Where NBT data is stored??
     */
    @Override
    public void load(CompoundTag compoundNBT) {
        super.load(compoundNBT);
        this.storedSculkMass = compoundNBT.getInt(storedSculkMassIdentifier);
    }

    /**
     * ???
     * @param compoundNBT Where NBT data is stored??
     * @return ???
     */
    @Override
    public void saveAdditional(CompoundTag compoundNBT) {

        compoundNBT.putInt(storedSculkMassIdentifier, this.storedSculkMass);
        super.saveAdditional(compoundNBT);
    }

    public int getStoredSculkMass()
    {
        return storedSculkMass;
    }

    public void setStoredSculkMass(int value)
    {
        storedSculkMass = value;
    }

    public void addStoredSculkMass(int value)
    {
        storedSculkMass += value;
    }

    public void tick()
    {
        // If world is not a server world, return
        if(level.isClientSide)
        {
            return;
        }
        // Tick every 5 seconds
        if(level.getGameTime() % 100 != 0)
        {
            return;
        }

        // If the tile entity at this location is not a sculk mass tile, return
        if(!(this.level.getBlockEntity(this.getBlockPos()) instanceof SculkMassTile))
        {
            return;
        }

        // Get the tile entity at this location
        SculkMassTile thisTile = (SculkMassTile) this.level.getBlockEntity(this.getBlockPos());

        EntityFactory entityFactory = SculkHorde.entityFactory;
        ReinforcementRequest context = new ReinforcementRequest(this.getBlockPos());

        context.sender = ReinforcementRequest.senderType.SculkMass;
        context.budget = thisTile.getStoredSculkMass();

        //Attempt to call in reinforcements and then update stored sculk mass
        entityFactory.requestReinforcementSculkMass(level, getBlockPos(), context);
        if(context.isRequestViewed && context.isRequestApproved)
        {
            thisTile.setStoredSculkMass(context.remaining_balance);

            // Spawn Block Traverser
            CursorSurfaceInfectorEntity cursor = new CursorSurfaceInfectorEntity(level);
            cursor.setPos(this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ());
            cursor.setMaxInfections(thisTile.getStoredSculkMass());
            cursor.setMaxRange(thisTile.getStoredSculkMass());
            level.addFreshEntity(cursor);

            //Destroy if run out of sculk mass
            if(thisTile.getStoredSculkMass() <= 0)
            {
                level.destroyBlock(this.getBlockPos(), false);
            }
        }
    }
}
