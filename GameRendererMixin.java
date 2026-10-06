package com.prediction.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    /**
     * Игнорирует игроков в рейкасте сущностей при наведении прицела.
     * Благодаря этому прицел и клики проходят сквозь модель игрока на блок сзади него.
     */
    @ModifyArg(
        method = "updateTargetedEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"
        ),
        index = 4
    )
    private Predicate<Entity> prediction$ignorePlayerHitboxes(Predicate<Entity> originalPredicate) {
        return entity -> originalPredicate.test(entity) && !(entity instanceof PlayerEntity);
    }
}
