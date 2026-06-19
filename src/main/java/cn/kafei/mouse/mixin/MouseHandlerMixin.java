package cn.kafei.mouse.mixin;

import cn.kafei.mouse.InputIsolationService;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    // 屏蔽游戏内原生鼠标按键，防止聚焦窗口接收未绑定设备输入。
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void mouse$blockNativeMouseButton(long windowPointer, int button, int action, int modifiers, CallbackInfo ci) {
        if (InputIsolationService.shouldBlockMouse(windowPointer)) {
            ci.cancel();
        }
    }

    // 屏蔽游戏内原生滚轮，物品栏切换由 IPC 鼠标包处理。
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void mouse$blockNativeMouseScroll(long windowPointer, double xOffset, double yOffset, CallbackInfo ci) {
        if (InputIsolationService.shouldBlockMouse(windowPointer)) {
            ci.cancel();
        }
    }

    // 屏蔽游戏内原生鼠标移动，避免未绑定设备累积视角增量。
    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void mouse$blockNativeMouseMove(long windowPointer, double xpos, double ypos, CallbackInfo ci) {
        if (InputIsolationService.shouldBlockMouse(windowPointer)) {
            ci.cancel();
        }
    }

    // 清空原生鼠标移动增量，避免当前实例被未绑定鼠标转动视角。
    @Inject(method = "handleAccumulatedMovement", at = @At("HEAD"), cancellable = true)
    private void mouse$blockNativeMouseMovement(CallbackInfo ci) {
        if (InputIsolationService.shouldBlockMouseMovement()) {
            accumulatedDX = 0.0;
            accumulatedDY = 0.0;
            ci.cancel();
        }
    }
}
