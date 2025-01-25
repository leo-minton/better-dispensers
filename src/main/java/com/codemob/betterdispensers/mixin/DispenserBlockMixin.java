package com.codemob.experiments.mixin;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(DispenserBlock.class)
public class DispenserBlockMixin {
    @Redirect(method = "dispenseFrom",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/dispenser/DispenseItemBehavior;dispense(Lnet/minecraft/core/dispenser/BlockSource;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    public ItemStack dispense(DispenseItemBehavior instance, BlockSource blockSource, ItemStack itemStack) {
        Direction direction = blockSource.state().getValue(DispenserBlock.FACING);
        BlockPos blockpos = blockSource.pos().relative(direction);
        ServerLevel level = blockSource.level();

        if (itemStack.is(Items.GOAT_HORN)) {
            Optional<Holder<Instrument>> maybeInstrument = ((InstrumentItem) itemStack.getItem())
                    .getInstrument(itemStack);
            if (maybeInstrument.isPresent()) {
                Instrument instrument = maybeInstrument.get().value();
                SoundEvent soundevent = instrument.soundEvent().value();
                float f = instrument.range() / 16.0F;
                level.playSound(null, blockSource.pos(), soundevent, SoundSource.RECORDS, f, 1.0F);
            }
            return itemStack;
        } else if (itemStack.getItem() instanceof HoeItem hoeItem) {
            //noinspection DataFlowIssue
            BlockHitResult hitResult = new BlockHitResult(null, direction.getOpposite(), blockpos, true);
            InteractionResult result = hoeItem.useOn(new UseOnContext(level, null, InteractionHand.MAIN_HAND, itemStack, hitResult));
            if (result.consumesAction()) {
                itemStack.hurtAndBreak(1, level, null, t -> {});
            }
            return itemStack;
        } else if (itemStack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof BushBlock)
        {
            //noinspection DataFlowIssue
            BlockHitResult hitResult = new BlockHitResult(null, Direction.UP, blockpos, true);
            itemStack.useOn(new UseOnContext(level, null, InteractionHand.MAIN_HAND, itemStack, hitResult));

            return itemStack;
        } if (itemStack.is(Items.FIRE_CHARGE)) {
            BlockState blockState = level.getBlockState(blockpos);
            if (blockState.is(Blocks.DRAGON_WALL_HEAD)
                    && blockState.getValue(WallSkullBlock.FACING) == direction)
            {
                RandomSource randomsource = level.getRandom();
                double x = randomsource.triangle(direction.getStepX(), 0.11485000000000001);
                double y = randomsource.triangle(direction.getStepY(), 0.11485000000000001);
                double z = randomsource.triangle(direction.getStepZ(), 0.11485000000000001);
                Vec3 velocity = new Vec3(x, y, z);
                Vec3 position = blockpos.getCenter();
                ArmorStand armorStand = new ArmorStand(level, position.x(), position.y(), position.z());
                DragonFireball dragonFireball = new DragonFireball(level, armorStand, velocity.normalize());
                dragonFireball.cachedOwner = null;
                dragonFireball.ownerUUID = null;
                dragonFireball.playSound(SoundEvents.ENDER_DRAGON_SHOOT);
                level.addFreshEntity(dragonFireball);

                itemStack.shrink(1);
                return itemStack;
            } else {
                return instance.dispense(blockSource, itemStack);
            }
        } else {
            return instance.dispense(blockSource, itemStack);
        }
    }
}
