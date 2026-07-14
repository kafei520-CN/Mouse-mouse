package cn.kafei.mouse;

import cn.kafei.mouse.mixin.MouseHandlerAccessor;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

public final class WindowFocusService {
    private WindowFocusService() {
    }

    public static boolean shouldAcceptRoutedInput(Minecraft mc) {
        if (!InputIsolationService.isIsolationEnabled() || mc == null) return false;
        if (mc.screen instanceof DeviceSelectScreen || mc.getOverlay() != null) return true;
        return InputIsolationService.getRoutedInputMode(mc) != InputIsolationService.RoutedInputMode.DISABLED;
    }

    public static void updateNativeMouseCapture(Minecraft mc, boolean safetyChordHeld) {
        if (mc == null) {
            return;
        }

        MouseHandler mouseHandler = mc.mouseHandler;
        if (mouseHandler == null) {
            return;
        }

        boolean shouldCapture = shouldCaptureNativeMouse(mc, safetyChordHeld);
        if (shouldCapture == mouseHandler.isMouseGrabbed()) {
            return;
        }

        if (shouldCapture) {
            grabNativeMouse(mc, mouseHandler);
        } else {
            mouseHandler.releaseMouse();
        }
    }

    public static boolean isCursorInsideWindow(Window window) {
        CursorScreenPosition position = getCursorScreenPosition(window);
        return position.x() >= 0.0
            && position.y() >= 0.0
            && position.x() < window.getScreenWidth()
            && position.y() < window.getScreenHeight();
    }

    public static double getCursorGuiX(Window window) {
        CursorScreenPosition position = getCursorScreenPosition(window);
        return position.x() * window.getGuiScaledWidth() / (double) window.getScreenWidth();
    }

    public static double getCursorGuiY(Window window) {
        CursorScreenPosition position = getCursorScreenPosition(window);
        return position.y() * window.getGuiScaledHeight() / (double) window.getScreenHeight();
    }

    public static CursorScreenPosition getCursorScreenPosition(Window window) {
        return new CursorScreenPosition(window.getScreenWidth() / 2.0, window.getScreenHeight() / 2.0);
    }

    private static boolean shouldCaptureNativeMouse(Minecraft mc, boolean safetyChordHeld) {
        if (safetyChordHeld) return false;
        if (!InputIsolationService.isIsolationEnabled()) return false;
        if (!mc.isWindowActive()) return false;
        if (mc.screen instanceof DeviceSelectScreen || mc.getOverlay() != null) return false;
        InputIsolationService.RoutedInputMode routedInputMode = InputIsolationService.getRoutedInputMode(mc);
        return routedInputMode.isWorld() || routedInputMode.isScreen();
    }

    // 只复用原版抓取原理，不调用 MouseHandler.grabMouse()，避免 GUI 被强制关闭。
    private static void grabNativeMouse(Minecraft mc, MouseHandler mouseHandler) {
        MouseHandlerAccessor accessor = (MouseHandlerAccessor) mouseHandler;
        double centerX = mc.getWindow().getScreenWidth() / 2.0;
        double centerY = mc.getWindow().getScreenHeight() / 2.0;
        accessor.mouse$setMouseGrabbed(true);
        accessor.mouse$setXpos(centerX);
        accessor.mouse$setYpos(centerY);
        InputConstants.grabOrReleaseMouse(mc.getWindow().getWindow(), 212995, centerX, centerY);
        accessor.mouse$invokeSetIgnoreFirstMove();
    }

    public record CursorScreenPosition(double x, double y) {
    }
}
