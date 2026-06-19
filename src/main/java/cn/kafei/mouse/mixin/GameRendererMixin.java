package cn.kafei.mouse.mixin;

import cn.kafei.mouse.VirtualCursorOverlayRenderer;
import cn.kafei.mouse.VirtualMouseService;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void mouse$syncVirtualCursorForRender(DeltaTracker deltaTracker, boolean renderWorld, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        VirtualMouseService.refreshForCurrentScreen(mc);
        VirtualMouseService.flushQueuedRelativeMove(mc); // 按渲染帧消费位移，避免 GUI 光标只有 20TPS 更新。
        VirtualMouseService.syncRenderPosition(mc, deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V",
            shift = At.Shift.AFTER
        )
    )
    private void mouse$renderVirtualCursorOverlay(DeltaTracker deltaTracker, boolean renderWorld, CallbackInfo ci) {
        VirtualCursorOverlayRenderer.renderFrameEndCursor(Minecraft.getInstance(), deltaTracker.getGameTimeDeltaPartialTick(false));
    }
}
