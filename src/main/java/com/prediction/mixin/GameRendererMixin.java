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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    /**
     * Перехватываем расчет прицела в самом конце каждого кадра.
     * Если в руке кирка и прицел наведен на игрока, сквозь него ищется блок.
     */
    @Inject(method = "updateCrosshairTarget", at = @At("TAIL"))
    private void prediction$passThroughPlayerWithPickaxe(float tickProgress, CallbackInfo ci) {
        if (this.client.player == null || this.client.world == null) {
            return;
        }

        // Проверяем, держит ли игрок кирку в главной руке (ванильную или из любого мода)
        ItemStack heldStack = this.client.player.getMainHandStack();
        boolean isPickaxe = heldStack.isIn(ItemTags.PICKAXES) || heldStack.getItem() instanceof PickaxeItem;
        if (!isPickaxe) {
            return; // С мечом, рукой или топором цель не меняется — будет обычный удар по игроку
        }

        // Если прицел захватил хитбокс другого игрока
        if (this.client.crosshairTarget instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof PlayerEntity) {
            Entity cameraEntity = this.client.getCameraEntity();
            if (cameraEntity == null) {
                cameraEntity = this.client.player;
            }

            // Пускаем рейкаст блоков на допустимую дистанцию взаимодействия
            double reach = this.client.player.getBlockInteractionRange();
            HitResult blockHit = cameraEntity.raycast(reach, tickProgress, false);

            // Если за игроком есть блок — делаем его активной целью прицела
            if (blockHit.getType() == HitResult.Type.BLOCK) {
                this.client.crosshairTarget = blockHit;
            }
        }
    }
}
