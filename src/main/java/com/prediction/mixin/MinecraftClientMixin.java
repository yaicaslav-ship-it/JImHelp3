package com.prediction.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow @Nullable public HitResult crosshairTarget;
    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public ClientWorld world;
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;

    /**
     * Вспомогательный метод: ищет блок сквозь сущности по направлению взгляда
     */
    private BlockHitResult prediction$raycastBlockBehind() {
        if (this.player == null || this.world == null || this.interactionManager == null) {
            return null;
        }

        double reach = (double) this.interactionManager.getReachDistance();
        Vec3d cameraPos = this.player.getCameraPosVec(1.0F);
        Vec3d rotationVec = this.player.getRotationVec(1.0F);
        Vec3d endPos = cameraPos.add(rotationVec.multiply(reach));

        return this.world.raycast(new RaycastContext(
            cameraPos,
            endPos,
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE,
            this.player
        ));
    }

    /**
     * 1. Перехват клика атаки (ЛКМ).
     * Если цель — игрок, отменяем удар по игроку и начинаем ломать блок позади него.
     */
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void prediction$overrideAttack(CallbackInfoReturnable<Boolean> cir) {
        if (this.crosshairTarget instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof PlayerEntity) {
            BlockHitResult blockHit = prediction$raycastBlockBehind();

            if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
                // Подменяем цель на блок
                this.crosshairTarget = blockHit;

                BlockPos pos = blockHit.getBlockPos();
                Direction side = blockHit.getSide();

                if (this.interactionManager != null && this.player != null) {
                    this.interactionManager.attackBlock(pos, side);
                    this.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                }
                cir.setReturnValue(true);
            }
        }
    }

    /**
     * 2. Перехват удержания ЛКМ (продолжение ломания блока).
     */
    @Inject(method = "handleBlockBreaking", at = @At("HEAD"))
    private void prediction$overrideContinuousBreaking(boolean breaking, CallbackInfo ci) {
        if (breaking && this.crosshairTarget instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof PlayerEntity) {
            BlockHitResult blockHit = prediction$raycastBlockBehind();
            if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
                this.crosshairTarget = blockHit;
            }
        }
    }
}
