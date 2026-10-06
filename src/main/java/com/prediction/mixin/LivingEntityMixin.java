package com.prediction.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.registry.tag.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    /**
     * Отключает возможность наведения прицела и удара по игрокам,
     * если в руках у клиента находится кирка.
     */
    @Inject(method = "canHit", at = @At("HEAD"), cancellable = true)
    private void prediction$ignorePlayersWhenHoldingPickaxe(CallbackInfoReturnable<Boolean> cir) {
        // Проверяем, что сущность под прицелом — другой игрок
        if ((Object) this instanceof PlayerEntity) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && (Object) this != client.player) {
                ItemStack stack = client.player.getMainHandStack();
                // Проверяем, держим ли мы кирку (ванильную или из любого мода)
                if (stack.getItem() instanceof PickaxeItem || stack.isIn(ItemTags.PICKAXES)) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
