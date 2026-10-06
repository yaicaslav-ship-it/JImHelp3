package com.prediction.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
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
    @Shadow private int attackCooldown;

    /**
     * Рейкаст блока строго позади сущности в пределах дистанции взаимодействия игрока.
     */
    private BlockHitResult prediction$raycastBlockBehind() {
        if (this.player == null || this.world == null) {
            return null;
        }

        double reach = this.player.getBlockInteractionRange();
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
     * Если в руке кирка и прицел наведен на игрока, подменяем цель на блок позади него.
     */
    private void prediction$redirectTargetIfHoldingPickaxe() {
        if (this.player == null || this.world == null) {
            return;
        }

        // Проверяем: в главной руке должна быть кирка (PickaxeItem)
        if (this.player.getMainHandStack().getItem() instanceof PickaxeItem) {
            if (this.crosshairTarget instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof PlayerEntity) {
                BlockHitResult blockHit = prediction$raycastBlockBehind();
                if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
                    this.crosshairTarget = blockHit;
                    this.attackCooldown = 0; // Сбрасываем задержку атаки, чтобы блок начинал ломаться сразу
                }
            }
        }
    }

    /**
     * 1. Перехват одиночного клика ЛКМ (начало удара / ломания).
     */
    @Inject(method = "doAttack", at = @At("HEAD"))
    private void prediction$onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        prediction$redirectTargetIfHoldingPickaxe();
    }

    /**
     * 2. Перехват удержания ЛКМ (продолжение разрушения блока).
     */
    @Inject(method = "handleBlockBreaking", at = @At("HEAD"))
    private void prediction$onHandleBlockBreaking(boolean breaking, CallbackInfo ci) {
        if (breaking) {
            prediction$redirectTargetIfHoldingPickaxe();
        }
    }

    /**
     * 3. Перехват обработки ввода перед проверкой кликов.
     */
    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void prediction$onHandleInputEvents(CallbackInfo ci) {
        prediction$redirectTargetIfHoldingPickaxe();
    }
}
