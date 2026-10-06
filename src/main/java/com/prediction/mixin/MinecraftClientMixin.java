package com.prediction.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow public HitResult crosshairTarget;

    /**
     * 1. Исключаем игроков из рейкаста сущностей при обновлении прицела.
     * Игра не будет цепляться за хитбокс игрока и оставит блок за ним в качестве цели.
     */
    @ModifyArg(
        method = "findCrosshairTarget",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"
        ),
        index = 4
    )
    private Predicate<Entity> prediction$ignorePlayerEntities(Predicate<Entity> originalPredicate) {
        return entity -> originalPredicate.test(entity) && !(entity instanceof PlayerEntity);
    }

    /**
     * 2. Страховка: если прицел всё равно указывает на игрока,
     * принудительно убираем сущность из цели перед обработкой кликов/ломания.
     */
    @Inject(method = "handleBlockBreaking", at = @At("HEAD"))
    private void prediction$forceBlockTargetOnBreak(boolean breaking, CallbackInfo ci) {
        if (this.crosshairTarget instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof PlayerEntity) {
            // Если перед блоком всё еще висит хитбокс игрока, сбрасываем его
            this.crosshairTarget = null;
        }
    }
}
