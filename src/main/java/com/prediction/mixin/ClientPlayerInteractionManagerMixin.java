package com.prediction.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.registry.tag.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
    private void prediction$cancelPlayerAttackWithPickaxe(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (target instanceof PlayerEntity) {
            ItemStack stack = player.getMainHandStack();
            boolean isPickaxe = stack.getItem() instanceof PickaxeItem || stack.isIn(ItemTags.PICKAXES);
            if (isPickaxe) {
                ci.cancel(); // Блокирует урон по игроку
            }
        }
    }
}
