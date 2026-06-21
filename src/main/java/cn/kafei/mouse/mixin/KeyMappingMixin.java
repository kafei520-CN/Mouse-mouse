package cn.kafei.mouse.mixin;

import cn.kafei.mouse.InputIsolationService;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public class KeyMappingMixin {
    @Inject(method = "releaseAll", at = @At("HEAD"), cancellable = true)
    private static void mouse$preventReleaseAllWhenIsolated(CallbackInfo ci) {
        if (InputIsolationService.isIsolationEnabled()) {
            ci.cancel();
        }
    }
}
