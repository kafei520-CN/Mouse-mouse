package cn.kafei.mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

public final class InputIsolationService {
    private static volatile boolean isolationEnabled;
    private static final ThreadLocal<Boolean> injectedKeyboardEvent = ThreadLocal.withInitial(() -> false);

    private InputIsolationService() {
    }

    public static void setIsolationEnabled(boolean enabled) {
        isolationEnabled = enabled;
    }

    public static boolean isIsolationEnabled() {
        return isolationEnabled;
    }

    public static void beginInjectedKeyboardEvent() {
        injectedKeyboardEvent.set(true);
    }

    public static void endInjectedKeyboardEvent() {
        injectedKeyboardEvent.set(false);
    }

    public static boolean isInjectedKeyboardEvent() {
        return injectedKeyboardEvent.get();
    }

    // 判断原生 GLFW 键盘事件是否应被屏蔽，避免焦点窗口吃到所有键盘。
    public static boolean shouldBlockKeyboard(long windowPointer, int key) {
        Minecraft mc = Minecraft.getInstance();
        if (!isIsolationTarget(mc, windowPointer)) return false;
        return !isSafetyKeyChord(mc, key);
    }

    // 判断原生 GLFW 鼠标事件是否应被屏蔽，选中设备输入由 IPC 单独注入。
    public static boolean shouldBlockMouse(long windowPointer) {
        return isIsolationTarget(Minecraft.getInstance(), windowPointer);
    }

    public static boolean shouldBlockMouseMovement() {
        Minecraft mc = Minecraft.getInstance();
        return getRoutedInputMode(mc).isWorld();
    }

    public static boolean shouldUseVirtualScreenInput(Minecraft mc) {
        return getRoutedInputMode(mc).isScreen();
    }

    public static boolean shouldRenderVirtualCursor(Screen screen) {
        return isolationEnabled && screen != null && !(screen instanceof DeviceSelectScreen);
    }

    public static RoutedInputMode getRoutedInputMode(Minecraft mc) {
        if (!isolationEnabled || mc == null || mc.getOverlay() != null) {
            return RoutedInputMode.DISABLED;
        }
        if (mc.screen != null && !(mc.screen instanceof DeviceSelectScreen)) {
            return RoutedInputMode.SCREEN;
        }
        if (mc.player != null && mc.screen == null) {
            return RoutedInputMode.WORLD;
        }
        return RoutedInputMode.DISABLED;
    }

    private static boolean isIsolationTarget(Minecraft mc, long windowPointer) {
        if (!isolationEnabled) return false;
        if (mc == null || mc.getWindow().getWindow() != windowPointer) return false;
        if (mc.getOverlay() != null || mc.screen instanceof DeviceSelectScreen) return false;
        if (mc.screen != null) return true;
        return mc.player != null;
    }

    private static boolean isSafetyKeyChord(Minecraft mc, int key) {
        if (key != GLFW.GLFW_KEY_F8) return false;
        long window = mc.getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;
    }

    public enum RoutedInputMode {
        DISABLED,
        WORLD,
        SCREEN;

        public boolean isWorld() {
            return this == WORLD;
        }

        public boolean isScreen() {
            return this == SCREEN;
        }
    }
}
