package cn.kafei.mouse.mixin;

import cn.kafei.mouse.InjectedKeyboardState;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InputConstants.class)
public class InputConstantsMixin {
    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    private static void mouse$injectKeyState(long window, int key, CallbackInfoReturnable<Boolean> cir) {
        if (InjectedKeyboardState.isKeyPressed(key)) {
            cir.setReturnValue(true);
        }
    }
}
