package cn.kafei.mouse;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;


@Mod(value = Mousemouse.MODID, dist = Dist.CLIENT)
public class MousemouseClient {
    public static final KeyMapping CONFIG_KEY = new KeyMapping(
        "key.mouse.config", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc"
    );

    public MousemouseClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterKeys);
    }

    private void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(CONFIG_KEY);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MinecraftPauseSupport.configure()); // 关闭失焦暂停，允许多窗口同时运行
        SplitterLauncher.start();
        InstanceDeviceSelectionService.setSelectedDeviceIds(java.util.List.of());
        InputIsolationService.setIsolationEnabled(false);
    }

    public static boolean consumeSafetyConfigChord() {
        return InjectedKeyboardState.isSafetyChordPressed() || isNativeSafetyConfigChordPressed();
    }

    private static boolean isNativeSafetyConfigChordPressed() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        boolean altDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
        boolean f8Down = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F8) == GLFW.GLFW_PRESS;
        return altDown && f8Down;
    }
}
