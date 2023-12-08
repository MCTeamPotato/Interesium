package com.teampotato.interesium.mixin;

import com.teampotato.interesium.api.ExtendedBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Bee.BeeGoToHiveGoal.class)
public abstract class BeeGoToHiveGoalMixin {
    @Shadow private List<BlockPos> blacklistedTargets;

    @Inject(method = "blacklistTarget", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private void onBlacklistTarget(BlockPos pos, CallbackInfo ci) {
        ((ExtendedBlockPos)pos).interesium$setIsInBlacklistedTargets(Boolean.TRUE);
    }

    @Inject(method = "blacklistTarget", at = @At(value = "INVOKE", target = "Ljava/util/List;remove(I)Ljava/lang/Object;", shift = At.Shift.BEFORE))
    private void onUnBlacklistTarget(BlockPos pos, CallbackInfo ci) {
        ((ExtendedBlockPos)this.blacklistedTargets.get(0)).interesium$setIsInBlacklistedTargets(null);
    }

    @Inject(method = "clearBlacklist", at = @At("HEAD"))
    private void onClearBlacklist(CallbackInfo ci) {
        this.blacklistedTargets.forEach(blockPos -> ((ExtendedBlockPos)blockPos).interesium$setIsInBlacklistedTargets(null));
    }

    @Inject(method = "isTargetBlacklisted", at = @At("HEAD"), cancellable = true)
    private void interesium$isTargetBlacklisted(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(((ExtendedBlockPos) pos).interesium$isInBlacklistedTargets() != null);
    }
}
