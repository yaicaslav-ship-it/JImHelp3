package com.prediction.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.registry.tag.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(Entity.class)
public abstract class EntityMixin {

    /**
     * Если игрок держит кирку, все сторонние игроки перестают быть "целью для удара" (hitbox uncollidable).
     */
    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
    private void prediction$ignorePlayersWithPickaxe(CallbackInfoReturnable<Boolean> cir) {
        // Проверяем, является ли эта сущность игроком
        if ((Object) this instanceof PlayerEntity) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                ItemStack stack = client.player.getMainHandStack();
                boolean isPickaxe = stack.isIn(ItemTags.PICKAXES) || stack.getItem() instanceof PickaxeItem;
                
                // Если держим кирку — хитбокс игрока не ловит прицел
                if (isPickaxe) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
