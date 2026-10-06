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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow @Nullable public HitResult crosshairTarget;
    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;

    /**
     * 1. При одиночном клике ЛКМ: если в руке кирка и смотрим на игрока,
     * отменяем атаку и принудительно начинаем ломать блок за ним.
     */
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void prediction$overrideAttack(CallbackInfoReturnable<Boolean> cir) {
        if (this.player == null || this.interactionManager == null) return;

        ItemStack stack = this.player.getMainHandStack();
        boolean isPickaxe = stack.getItem() instanceof PickaxeItem || stack.isIn(ItemTags.PICKAXES);

        if (isPickaxe && this.crosshairTarget instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof PlayerEntity) {
            // Пускаем луч сквозь любые сущности чисто по блокам
            HitResult hit = this.player.raycast(this.player.getBlockInteractionRange(), 1.0F, false);
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                this.crosshairTarget = blockHit;
                this.interactionManager.attackBlock(blockHit.getBlockPos(), blockHit.getSide());
                this.player.swingHand(Hand.MAIN_HAND);
                cir.setReturnValue(true);
            }
        }
    }

    /**
     * 2. При зажатой ЛКМ: каждый тик переключаем цель на блок за игроком,
     * чтобы игра продолжала разрушение блока (анимация трещин и снос блока).
     */
    @Inject(method = "handleBlockBreaking", at = @At("HEAD"))
    private void prediction$overrideContinuousBreaking(boolean breaking, CallbackInfo ci) {
        if (breaking && this.player != null) {
            ItemStack stack = this.player.getMainHandStack();
            boolean isPickaxe = stack.getItem() instanceof PickaxeItem || stack.isIn(ItemTags.PICKAXES);

            if (isPickaxe && this.crosshairTarget instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof PlayerEntity) {
                HitResult hit = this.player.raycast(this.player.getBlockInteractionRange(), 1.0F, false);
                if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                    this.crosshairTarget = blockHit;
                }
            }
        }
    }
}
