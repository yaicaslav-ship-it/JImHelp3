package com.prediction.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.registry.tag.ItemTags;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    /**
     * Перехватываем предикат проверки сущностей в методе updateCrosshairTarget.
     * Если в руке кирка — игроки полностью исключаются из рейкаста прицела.
     */
    @ModifyArg(
        method = "updateCrosshairTarget",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/projectile/ProjectileUtil;raycast(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Box;Ljava/util/function/Predicate;D)Lnet/minecraft/util/hit/EntityHitResult;"
        ),
        index = 4
    )
    private Predicate<Entity> prediction$ignorePlayersWithPickaxe(Predicate<Entity> originalPredicate) {
        return entity -> {
            if (entity instanceof PlayerEntity) {
                if (this.client.player != null) {
                    ItemStack stack = this.client.player.getMainHandStack();
                    boolean isPickaxe = stack.isIn(ItemTags.PICKAXES) || stack.getItem() instanceof PickaxeItem;
                    if (isPickaxe) {
                        return false; // Игнорируем игрока — прицел проходит сквозь него на блок
                    }
                }
            }
            return originalPredicate.test(entity);
        };
    }
}
