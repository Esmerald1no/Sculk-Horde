package com.github.sculkhorde.common.effect;

import com.github.sculkhorde.common.block.SculkMassBlock;
import com.github.sculkhorde.core.BlockRegistry;
import com.github.sculkhorde.core.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.MobEffectEvent;

import java.util.ArrayList;
import java.util.List;

public class SculkInfectionEffect extends MobEffect {

    public static int spawnInterval = 20;
    public static int liquidColor = 338997;
    public static MobEffectCategory effectType = MobEffectCategory.HARMFUL;


    /**
     * Old Dumb Constructor
     * @param effectType Determines if harmful or not
     * @param liquidColor The color in some number format
     */
    protected SculkInfectionEffect(MobEffectCategory effectType, int liquidColor) {
        super(effectType, liquidColor);
    }

    /**
     * Simpler Constructor
     */
    public SculkInfectionEffect() {
        this(effectType, liquidColor);
    }

    public static void onPotionExpire(MobEffectEvent.Expired event)
    {
        LivingEntity entity = event.getEntity();
        if(entity == null)
        {
            return;
        }

        //Spawn Effect Level + 1 number of mites
        int infectionDamage = 4;
        Level entityLevel = entity.level();
        BlockPos entityPosition = entity.blockPosition();
        float entityHealth = entity.getMaxHealth();

        //Spawn Mite
        EntityRegistry.SCULK_MITE.get().spawn((ServerLevel) event.getEntity().level(), entityPosition, MobSpawnType.SPAWNER);

        //Spawn Sculk Mass
        SculkMassBlock sculkMass = BlockRegistry.SCULK_MASS.get();
        sculkMass.spawn(entityLevel, entityPosition, entityHealth);
        //Do infectionDamage to victim per mite
        entity.hurt(entity.damageSources().magic(), infectionDamage);

    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        super.applyEffectTick(entity, amplifier);
    }

    /**
     * A function that is called every tick an entity has this effect. <br>
     * I do not use because it does not provide any useful inputs like
     * the entity it is affecting. <br>
     * I instead use ForgeEventSubscriber.java to handle the logic.
     * @param ticksLeft The amount of ticks remaining
     * @param amplifier The level of the effect
     * @return ??
     */
    @Override
    public boolean isDurationEffectTick(int ticksLeft, int amplifier) {
        return super.isDurationEffectTick(ticksLeft, amplifier);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
        return ret;
    }


}
