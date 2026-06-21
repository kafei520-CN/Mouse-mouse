package cn.kafei.mouse.mixin;

import cn.kafei.mouse.InputIsolationService;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftIsWindowActiveMixin {
    @Inject(method = "isWindowActive", at = @At("HEAD"), cancellable = true)
    private void mouse$forceWindowActive(CallbackInfoReturnable<Boolean> cir) {
        if (InputIsolationService.isIsolationEnabled()) {
            cir.setReturnValue(true);
        }
    }
}
