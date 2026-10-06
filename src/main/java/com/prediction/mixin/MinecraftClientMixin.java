package com.prediction.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow @Nullable public HitResult crosshairTarget;
    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void prediction$cancelAttackOnPlayerWithPickaxe(CallbackInfoReturnable<Boolean> cir) {
        if (this.player == null) return;

        ItemStack stack = this.player.getMainHandStack();
        boolean isPickaxe = stack.isIn(ItemTags.PICKAXES) || stack.getItem() instanceof PickaxeItem;

        if (isPickaxe && this.crosshairTarget instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof PlayerEntity) {
            // Отменяем атаку по игроку
            cir.setReturnValue(false);

            // Пускаем луч сквозь игрока до ближайшего блока
            HitResult blockHit = this.player.raycast(this.player.getBlockInteractionRange(), 1.0F, false);
            if (blockHit instanceof BlockHitResult bhr && blockHit.getType() == HitResult.Type.BLOCK) {
                this.crosshairTarget = bhr;
                if (this.interactionManager != null) {
                    this.interactionManager.attackBlock(bhr.getBlockPos(), bhr.getSide());
                    this.player.swingHand(Hand.MAIN_HAND);
                }
            }
        }
    }
}
