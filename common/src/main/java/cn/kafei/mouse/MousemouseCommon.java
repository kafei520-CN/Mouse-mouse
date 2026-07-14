package cn.kafei.mouse;

import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import java.util.List;

public class MousemouseCommon {
    public static final String MODID = "mouse";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static KeyMapping CONFIG_KEY;
    private static boolean safetyChordHeld = false;

    public static void initClient() {
        MinecraftPauseSupport.configure();
        SplitterLauncher.start(Minecraft.getInstance().gameDirectory.toPath());
        InstanceDeviceSelectionService.setSelectedDeviceIds(List.of());
        InputIsolationService.setIsolationEnabled(false);
    }

    public static void onTickStart() {
        VirtualMouseService.savePrevPosition();
    }

    public static void onTickEnd() {
        Minecraft mc = Minecraft.getInstance();
        boolean chordDown = consumeSafetyConfigChord();
        WindowFocusService.updateNativeMouseCapture(mc, chordDown);
        VirtualMouseService.refreshForCurrentScreen(mc);
        if (chordDown && !safetyChordHeld) {
            mc.setScreen(new DeviceSelectScreen(mc.screen));
        }
        safetyChordHeld = chordDown;
    }

    public static void onShutdown() {
        InjectedKeyboardState.clear();
        IPCClient.getInstance().disconnect();
        SplitterLauncher.stop();
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
