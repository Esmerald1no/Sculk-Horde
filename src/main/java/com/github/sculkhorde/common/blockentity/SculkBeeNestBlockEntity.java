package com.github.sculkhorde.common.blockentity;

import com.github.sculkhorde.common.block.SculkBeeNestBlock;
import com.github.sculkhorde.common.entity.SculkBeeHarvesterEntity;
import com.github.sculkhorde.common.entity.SculkBeeInfectorEntity;
import com.github.sculkhorde.common.entity.infection.CursorSurfaceInfectorEntity;
import com.github.sculkhorde.common.structures.procedural.SculkBeeNestProceduralStructure;
import com.github.sculkhorde.core.BlockEntityRegistry;
import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.util.TickUnits;
import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class SculkBeeNestBlockEntity extends BlockEntity
{
    public static final String TAG_FLOWER_POS = "FlowerPos";
    public static final String MIN_OCCUPATION_TICKS = "MinOccupationTicks";
    public static final String ENTITY_DATA = "EntityData";
    public static final String TICKS_IN_HIVE = "TicksInHive";
    public static final String HAS_NECTAR = "HasNectar";
    public static final String BEES = "Bees";
    private static final List<String> IGNORED_BEE_TAGS = Arrays.asList("Air", "ArmorDropChances", "ArmorItems", "Brain", "CanPickUpLoot", "DeathTime", "FallDistance", "FallFlying", "Fire", "HandDropChances", "HandItems", "HurtByTimestamp", "HurtTime", "LeftHanded", "Motion", "NoGravity", "OnGround", "PortalCooldown", "Pos", "Rotation", "CannotEnterHiveTicks", "TicksSincePollination", "CropsGrownSincePollination", "HivePos", "Passengers", "Leash", "UUID");
    public static final int MAX_OCCUPANTS = 4;
    private static final int MIN_TICKS_BEFORE_REENTERING_HIVE = 400;
    private static final int MIN_OCCUPATION_TICKS_NECTAR = 2400;
    public static final int MIN_OCCUPATION_TICKS_NECTARLESS = 600;
    private final List<BeeData> stored = Lists.newArrayList();
    @Nullable
    private BlockPos savedFlowerPos;

    //The procedural structure that will be built
    private SculkBeeNestProceduralStructure beeNestStructure;

    //Used for ticking this block at an interval
    private int tickTracker = 0;

    //Keep track of last time since repair so we know when to restart
    private long lastTimeSinceRepair = -1;

    public SculkBeeNestBlockEntity(BlockPos p_155134_, BlockState p_155135_) {
        super(BlockEntityRegistry.SCULK_BEE_NEST_BLOCK_ENTITY.get(), p_155134_, p_155135_);
    }

    public void setChanged() {
        if (this.isFireNearby()) {
            this.emptyAllLivingFromHive((Player)null, this.level.getBlockState(this.getBlockPos()), BeeReleaseStatus.EMERGENCY);
        }

        super.setChanged();
    }

    public boolean isFireNearby() {
        if (this.level == null) {
            return false;
        } else {
            for(BlockPos blockpos : BlockPos.betweenClosed(this.worldPosition.offset(-1, -1, -1), this.worldPosition.offset(1, 1, 1))) {
                if (this.level.getBlockState(blockpos).getBlock() instanceof FireBlock) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean isEmpty() {
        return this.stored.isEmpty();
    }

    public boolean isFull() {
        return this.stored.size() == MAX_OCCUPANTS;
    }

    public void emptyAllLivingFromHive(@Nullable Player p_58749_, BlockState p_58750_, BeeReleaseStatus p_58751_) {
        List<Entity> list = this.releaseAllOccupants(p_58750_, p_58751_);
        if (p_58749_ != null) {
            for(Entity entity : list) {
                if (entity instanceof Bee) {
                    Bee bee = (Bee)entity;
                    if (p_58749_.position().distanceToSqr(entity.position()) <= 16.0D) {
                        if (!this.isSedated()) {
                            bee.setTarget(p_58749_);
                        } else {
                            bee.setStayOutOfHiveCountdown(MIN_TICKS_BEFORE_REENTERING_HIVE);
                        }
                    }
                }
            }
        }

    }

    private List<Entity> releaseAllOccupants(BlockState p_58760_, BeeReleaseStatus p_58761_) {
        List<Entity> list = Lists.newArrayList();
        this.stored.removeIf((p_272556_) -> {
            return releaseOccupant(this.level, this.worldPosition, p_58760_, p_272556_, list, p_58761_, this.savedFlowerPos);
        });
        if (!list.isEmpty()) {
            super.setChanged();
        }

        return list;
    }

    public void addFreshHarvesterOccupant() {
        this.addOccupantWithPresetTicks(new SculkBeeHarvesterEntity(level), false, 0);
    }

    public void addFreshInfectorOccupant() {
        this.addOccupantWithPresetTicks(new SculkBeeInfectorEntity(level), false, 0);
    }

    public void addOccupant(Entity entity, boolean hasNectar) {
        this.addOccupantWithPresetTicks(entity, hasNectar, 0);
    }

    @VisibleForDebug
    public int getOccupantCount() {
        return this.stored.size();
    }

    public static int getHoneyLevel(BlockState p_58753_) {
        return p_58753_.getValue(BeehiveBlock.HONEY_LEVEL);
    }

    @VisibleForDebug
    public boolean isSedated() {
        return CampfireBlock.isSmokeyPos(this.level, this.getBlockPos());
    }

    public void addOccupantWithPresetTicks(Entity entity, boolean hasNectar, int ticks) {
        if (this.stored.size() < MAX_OCCUPANTS)
        {
            entity.stopRiding();
            entity.ejectPassengers();
            CompoundTag compoundtag = new CompoundTag();
            entity.save(compoundtag);
            this.storeBee(compoundtag, ticks, hasNectar);
            if (this.level != null)
            {
                if (entity instanceof Bee)
                {
                    Bee bee = (Bee)entity;
                    if (bee.hasSavedFlowerPos() && (!this.hasSavedFlowerPos() || this.level.random.nextBoolean())) {
                        this.savedFlowerPos = bee.getSavedFlowerPos();
                    }
                }

                BlockPos blockpos = this.getBlockPos();
                this.level.playSound((Player)null, (double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ(), SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1.0F, 1.0F);
                this.level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(entity, this.getBlockState()));

                //Give Sculk Horde Mass
                SculkHorde.savedData.addSculkAccumulatedMass(5);

                //Summon Surface Infestor
                CursorSurfaceInfectorEntity cursor = new CursorSurfaceInfectorEntity(level);
                cursor.setPos(blockpos.getX(), blockpos.getY() - 1, blockpos.getZ());
                cursor.setMaxInfections(100);
                cursor.setMaxRange(100);
                cursor.setTickIntervalMilliseconds(500);
                cursor.setSearchIterationsPerTick(10);
                level.addFreshEntity(cursor);
            }

            entity.discard();
            super.setChanged();
        }
    }

    public void storeBee(CompoundTag p_155158_, int p_155159_, boolean p_155160_) {
        this.stored.add(new BeeData(p_155158_, p_155159_, p_155160_ ? MIN_OCCUPATION_TICKS_NECTAR : MIN_OCCUPATION_TICKS_NECTARLESS));
    }

    private static boolean releaseOccupant(Level level, BlockPos blockPos, BlockState blockState, BeeData beeData, @Nullable List<Entity> entities, BeeReleaseStatus beeReleaseStatus, @Nullable BlockPos blockPos1) {
        if (beeReleaseStatus == BeeReleaseStatus.EMERGENCY)
        {
            return false;
        }
        else
        {
            CompoundTag compoundtag = beeData.entityData.copy();
            removeIgnoredBeeTags(compoundtag);
            compoundtag.put("HivePos", NbtUtils.writeBlockPos(blockPos));
            compoundtag.putBoolean("NoGravity", true);
            Direction direction = blockState.getValue(BeehiveBlock.FACING);
            BlockPos blockpos = blockPos.relative(direction);
            boolean flag = !level.getBlockState(blockpos).getCollisionShape(level, blockpos).isEmpty();
            if (flag && beeReleaseStatus != BeeReleaseStatus.EMERGENCY)
            {
                return false;
            }

            Entity entity = EntityType.loadEntityRecursive(compoundtag, level, (p_58740_) -> {
                return p_58740_;
            });
            if (entity == null || blockState.getValue(SculkBeeNestBlock.CLOSED))
            {
                return false;
            }


            if (!(entity instanceof SculkBeeHarvesterEntity))
            {
                return false;
            }
            SculkBeeHarvesterEntity bee = (SculkBeeHarvesterEntity)entity;
            if (blockPos1 != null && !bee.hasSavedFlowerPos() && level.random.nextFloat() < 0.9F)
            {
                bee.setSavedFlowerPos(blockPos1);
            }

            if (beeReleaseStatus == BeeReleaseStatus.HONEY_DELIVERED)
            {
                bee.dropOffNectar();

                // Make a random cell under the hive mature
                Optional<SculkBeeNestBlockEntity> blockEntity =  level.getBlockEntity(blockPos, BlockEntityRegistry.SCULK_BEE_NEST_BLOCK_ENTITY.get());
                blockEntity.ifPresent(beeNestStructure -> {
                    if(beeNestStructure.beeNestStructure != null)
                    {
                        beeNestStructure.beeNestStructure.makeRandomBlockMature();
                    }
                });

                int i = getHoneyLevel(blockState);
                if (i < 5) {
                    int j = level.random.nextInt(100) == 0 ? 2 : 1;
                    if (i + j > 5) {
                        --j;
                    }

                    level.setBlockAndUpdate(blockPos, blockState.setValue(BeehiveBlock.HONEY_LEVEL, Integer.valueOf(i + j)));
                }
            }

            setBeeReleaseData(beeData.ticksInHive, bee);
            if (entities != null)
            {
                entities.add(bee);
            }

            float f = entity.getBbWidth();
            double d3 = flag ? 0.0D : 0.55D + (double)(f / 2.0F);
            double d0 = (double)blockPos.getX() + 0.5D + d3 * (double)direction.getStepX();
            double d1 = (double)blockPos.getY() + 0.5D - (double)(entity.getBbHeight() / 2.0F);
            double d2 = (double)blockPos.getZ() + 0.5D + d3 * (double)direction.getStepZ();
            entity.moveTo(d0, d1, d2, entity.getYRot(), entity.getXRot());


            level.playSound((Player)null, blockPos, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Context.of(entity, level.getBlockState(blockPos)));
            return level.addFreshEntity(entity);

        }
    }

    static void removeIgnoredBeeTags(CompoundTag p_155162_) {
        for(String s : IGNORED_BEE_TAGS) {
            p_155162_.remove(s);
        }

    }

    private static void setBeeReleaseData(int p_58737_, SculkBeeHarvesterEntity p_58738_)
    {
    }

    private boolean hasSavedFlowerPos() {
        return this.savedFlowerPos != null;
    }

    private static void tickOccupants(Level p_155150_, BlockPos p_155151_, BlockState p_155152_, List<BeeData> p_155153_, @Nullable BlockPos p_155154_) {
        boolean flag = false;

        BeeData beehiveblockentity$beedata;
        for(Iterator<BeeData> iterator = p_155153_.iterator(); iterator.hasNext(); ++beehiveblockentity$beedata.ticksInHive) {
            beehiveblockentity$beedata = iterator.next();
            if (beehiveblockentity$beedata.ticksInHive > beehiveblockentity$beedata.minOccupationTicks) {
                BeeReleaseStatus beehiveblockentity$beereleasestatus = beehiveblockentity$beedata.entityData.getBoolean("HasNectar") ? BeeReleaseStatus.HONEY_DELIVERED : BeeReleaseStatus.BEE_RELEASED;
                if (releaseOccupant(p_155150_, p_155151_, p_155152_, beehiveblockentity$beedata, (List<Entity>)null, beehiveblockentity$beereleasestatus, p_155154_)) {
                    flag = true;
                    iterator.remove();
                }
            }
        }

        if (flag) {
            setChanged(p_155150_, p_155151_, p_155152_);
        }

    }

    public static void serverTick(Level level, BlockPos blockPos, BlockState p_155147_, SculkBeeNestBlockEntity blockEntity)
    {
        tickOccupants(level, blockPos, p_155147_, blockEntity.stored, blockEntity.savedFlowerPos);
        if (!blockEntity.stored.isEmpty() && level.getRandom().nextDouble() < 0.005D) {
            double d0 = (double)blockPos.getX() + 0.5D;
            double d1 = (double)blockPos.getY();
            double d2 = (double)blockPos.getZ() + 0.5D;
            level.playSound((Player)null, d0, d1, d2, SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        /** Structure Building Process **/
        blockEntity.tickTracker++;

        //Tick Every Minute
        if(blockEntity.tickTracker < TickUnits.convertMinutesToTicks(1)) { return; }

        blockEntity.tickTracker = 0;
        long timeElapsed = TimeUnit.MINUTES.convert(System.nanoTime() - blockEntity.lastTimeSinceRepair, TimeUnit.NANOSECONDS);

        //If the Bee Nest Structure hasnt been initialized yet, do it
        if(blockEntity.beeNestStructure == null)
        {
            //Create Structure
            blockEntity.beeNestStructure = new SculkBeeNestProceduralStructure((ServerLevel) level, blockPos);
            blockEntity.beeNestStructure.generatePlan();
        }

        //If currently building, call build tick.
        //Repair routine will restart after an hour
        long repairIntervalInMinutes = 30;
        if(blockEntity.beeNestStructure.isCurrentlyBuilding())
        {
            blockEntity.beeNestStructure.buildTick();
            blockEntity.lastTimeSinceRepair = System.nanoTime();
        }
        //If enough time has passed, or we havent built yet, start build
        else if((timeElapsed >= repairIntervalInMinutes || blockEntity.lastTimeSinceRepair == -1) && blockEntity.beeNestStructure.canStartToBuild())
        {
            blockEntity.beeNestStructure.startBuildProcedure();
        }
    }

    public void load(CompoundTag p_155156_) {
        super.load(p_155156_);
        this.stored.clear();
        ListTag listtag = p_155156_.getList("Bees", 10);

        for(int i = 0; i < listtag.size(); ++i) {
            CompoundTag compoundtag = listtag.getCompound(i);
            BeeData beehiveblockentity$beedata = new BeeData(compoundtag.getCompound("EntityData"), compoundtag.getInt("TicksInHive"), compoundtag.getInt("MinOccupationTicks"));
            this.stored.add(beehiveblockentity$beedata);
        }

        this.savedFlowerPos = null;
        if (p_155156_.contains("FlowerPos")) {
            this.savedFlowerPos = NbtUtils.readBlockPos(p_155156_.getCompound("FlowerPos"));
        }

    }

    protected void saveAdditional(CompoundTag p_187467_) {
        super.saveAdditional(p_187467_);
        p_187467_.put("Bees", this.writeBees());
        if (this.hasSavedFlowerPos()) {
            p_187467_.put("FlowerPos", NbtUtils.writeBlockPos(this.savedFlowerPos));
        }

    }

    public ListTag writeBees() {
        ListTag listtag = new ListTag();

        for(BeeData beehiveblockentity$beedata : this.stored) {
            CompoundTag compoundtag = beehiveblockentity$beedata.entityData.copy();
            compoundtag.remove("UUID");
            CompoundTag compoundtag1 = new CompoundTag();
            compoundtag1.put("EntityData", compoundtag);
            compoundtag1.putInt("TicksInHive", beehiveblockentity$beedata.ticksInHive);
            compoundtag1.putInt("MinOccupationTicks", beehiveblockentity$beedata.minOccupationTicks);
            listtag.add(compoundtag1);
        }

        return listtag;
    }

    static class BeeData {
        final CompoundTag entityData;
        int ticksInHive;
        final int minOccupationTicks;

        BeeData(CompoundTag p_58786_, int p_58787_, int p_58788_) {
            removeIgnoredBeeTags(p_58786_);
            this.entityData = p_58786_;
            this.ticksInHive = p_58787_;
            this.minOccupationTicks = p_58788_;
        }
    }

    public static enum BeeReleaseStatus {
        HONEY_DELIVERED,
        BEE_RELEASED,
        EMERGENCY;
    }

}
