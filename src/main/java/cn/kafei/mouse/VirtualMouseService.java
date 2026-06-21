package cn.kafei.mouse;

import cn.kafei.mouse.mixin.MouseHandlerAccessor;
import com.mojang.blaze3d.Blaze3D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.concurrent.atomic.AtomicInteger;

public final class VirtualMouseService {
    private static final double BASE_CURSOR_SPEED = 0.18;
    private static final double HOVER_SPEED_MULTIPLIER = 0.55;
    private static final double HOVER_SPEED_BLEND_FACTOR = 0.35;
    private static final AtomicInteger PENDING_RELATIVE_DX = new AtomicInteger();
    private static final AtomicInteger PENDING_RELATIVE_DY = new AtomicInteger();

    private static double screenCursorX;
    private static double screenCursorY;
    private static double prevScreenCursorX;
    private static double prevScreenCursorY;
    private static int activeButton = -1;
    private static boolean initialized;
    private static double currentSpeedMultiplier = 1.0D;
    private static Screen syncedScreen;

    private VirtualMouseService() {
    }

    public static void reset() {
        activeButton = -1;
        initialized = false;
        currentSpeedMultiplier = 1.0D;
        syncedScreen = null;
        clearPendingRelativeMove();
    }

    public static void savePrevPosition() {
        prevScreenCursorX = screenCursorX;
        prevScreenCursorY = screenCursorY;
    }

    // 每帧渲染前写入插值坐标，让原版 GUI 命中检测跟随虚拟光标。
    public static void syncRenderPosition(Minecraft mc, float partialTick) {
        if (!initialized) {
            return;
        }
        MouseHandlerAccessor accessor = (MouseHandlerAccessor) mc.mouseHandler;
        accessor.mouse$setXpos(prevScreenCursorX + (screenCursorX - prevScreenCursorX) * partialTick);
        accessor.mouse$setYpos(prevScreenCursorY + (screenCursorY - prevScreenCursorY) * partialTick);
    }

    public static void refreshForCurrentScreen(Minecraft mc) {
        if (!InputIsolationService.shouldUseVirtualScreenInput(mc) || mc.screen == null) {
            if (initialized && shouldRestoreNativeMousePosition(mc)) {
                restoreNativeMousePosition(mc);
            }
            reset();
            return;
        }

        Screen screen = mc.screen;
        if (!initialized || syncedScreen == null) {
            clearPendingRelativeMove();
            centerInWindow(mc);
            initialized = true;
        } else {
            screenCursorX = clamp(screenCursorX, 0.0, maxScreenX(mc));
            screenCursorY = clamp(screenCursorY, 0.0, maxScreenY(mc));
            syncMouseHandlerPosition(mc);
        }

        syncedScreen = screen;
    }

    // 累积 IPC 相对位移，在客户端 tick 统一消费，降低逐包抖动。
    public static void enqueueRelativeMove(int dx, int dy) {
        if (dx != 0) {
            PENDING_RELATIVE_DX.addAndGet(dx);
        }
        if (dy != 0) {
            PENDING_RELATIVE_DY.addAndGet(dy);
        }
    }

    // 在主线程按帧消费累计位移，保持 GUI 光标移动节奏稳定。
    public static void flushQueuedRelativeMove(Minecraft mc) {
        if (!InputIsolationService.shouldUseVirtualScreenInput(mc) || mc.screen == null) {
            clearPendingRelativeMove();
            return;
        }

        int dx = PENDING_RELATIVE_DX.getAndSet(0);
        int dy = PENDING_RELATIVE_DY.getAndSet(0);
        if (dx != 0 || dy != 0) {
            handleRelativeMove(mc, dx, dy);
        }
    }

    public static void handleRelativeMove(Minecraft mc, int dx, int dy) {
        Screen screen = mc.screen;
        if (!InputIsolationService.shouldUseVirtualScreenInput(mc) || screen == null || (dx == 0 && dy == 0)) {
            return;
        }
        refreshForCurrentScreen(mc);

        double previousGuiX = getGuiCursorX(mc);
        double previousGuiY = getGuiCursorY(mc);
        boolean hoveringInteractable = ScreenInteractionHelper.isHoveringContainerSlot(screen)
            || ScreenInteractionHelper.isHoveringInteractable(screen, previousGuiX, previousGuiY);
        double targetSpeedMultiplier = hoveringInteractable ? HOVER_SPEED_MULTIPLIER : 1.0D;
        currentSpeedMultiplier = blendSpeedMultiplier(currentSpeedMultiplier, targetSpeedMultiplier);
        double cursorSpeed = getCursorSpeed(mc) * currentSpeedMultiplier * WorldMouseService.screenSensitivity;

        screenCursorX = clamp(screenCursorX + dx * cursorSpeed, 0.0, maxScreenX(mc));
        screenCursorY = clamp(screenCursorY + dy * cursorSpeed, 0.0, maxScreenY(mc));
        syncMouseHandlerPosition(mc);

        double guiX = getGuiCursorX(mc);
        double guiY = getGuiCursorY(mc);
        if (guiX != previousGuiX || guiY != previousGuiY) {
            VirtualScreenMouseRouter.dispatchMove(screen, guiX, guiY, activeButton, guiX - previousGuiX, guiY - previousGuiY);
        }
    }

    public static void handleButtonState(Minecraft mc, int button, boolean down, boolean up) {
        Screen screen = mc.screen;
        if (!InputIsolationService.shouldUseVirtualScreenInput(mc) || screen == null) {
            return;
        }

        refreshForCurrentScreen(mc);
        double guiX = getGuiCursorX(mc);
        double guiY = getGuiCursorY(mc);
        if (down) {
            activeButton = button;
            updateMousePressState(mc, button, true);
            VirtualScreenMouseRouter.dispatchClick(screen, guiX, guiY, button);
        }
        if (up) {
            VirtualScreenMouseRouter.dispatchRelease(screen, guiX, guiY, button);
            if (activeButton == button) {
                activeButton = -1;
            }
            updateMousePressState(mc, button, false);
        }
    }

    public static void handleScroll(Minecraft mc, int rolling) {
        Screen screen = mc.screen;
        if (!InputIsolationService.shouldUseVirtualScreenInput(mc) || screen == null || rolling == 0) {
            return;
        }

        refreshForCurrentScreen(mc);
        VirtualScreenMouseRouter.dispatchScroll(mc, screen, getGuiCursorX(mc), getGuiCursorY(mc), rolling);
    }

    public static boolean shouldRenderOverlay(Minecraft mc) {
        return mc != null && initialized && InputIsolationService.shouldRenderVirtualCursor(mc.screen);
    }

    public static double getRenderGuiX(Minecraft mc, float partialTick) {
        double renderX = prevScreenCursorX + (screenCursorX - prevScreenCursorX) * partialTick;
        return renderX * mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
    }

    public static double getRenderGuiY(Minecraft mc, float partialTick) {
        double renderY = prevScreenCursorY + (screenCursorY - prevScreenCursorY) * partialTick;
        return renderY * mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
    }

    public static int getCursorFillColor() {
        return activeButton == -1 ? 0xFFFFFFFF : 0xFFFFD54A;
    }

    private static double getGuiCursorX(Minecraft mc) {
        return screenCursorX * mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
    }

    private static double getGuiCursorY(Minecraft mc) {
        return screenCursorY * mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
    }

    private static void syncMouseHandlerPosition(Minecraft mc) {
        MouseHandlerAccessor accessor = (MouseHandlerAccessor) mc.mouseHandler;
        accessor.mouse$setXpos(screenCursorX);
        accessor.mouse$setYpos(screenCursorY);
    }

    private static void updateMousePressState(Minecraft mc, int button, boolean pressed) {
        MouseHandlerAccessor accessor = (MouseHandlerAccessor) mc.mouseHandler;
        accessor.mouse$setActiveButton(pressed ? button : -1);
        if (pressed) {
            accessor.mouse$setMousePressedTime(Blaze3D.getTime());
        }
    }

    private static void centerInWindow(Minecraft mc) {
        screenCursorX = mc.getWindow().getScreenWidth() / 2.0;
        screenCursorY = mc.getWindow().getScreenHeight() / 2.0;
        prevScreenCursorX = screenCursorX;
        prevScreenCursorY = screenCursorY;
        syncMouseHandlerPosition(mc);
    }

    private static double maxScreenX(Minecraft mc) {
        return Math.max(0.0, mc.getWindow().getScreenWidth() - 1.0);
    }

    private static double maxScreenY(Minecraft mc) {
        return Math.max(0.0, mc.getWindow().getScreenHeight() - 1.0);
    }

    private static void restoreNativeMousePosition(Minecraft mc) {
        WindowFocusService.CursorScreenPosition position = WindowFocusService.getCursorScreenPosition(mc.getWindow());
        MouseHandlerAccessor accessor = (MouseHandlerAccessor) mc.mouseHandler;
        accessor.mouse$setXpos(position.x());
        accessor.mouse$setYpos(position.y());
        accessor.mouse$setActiveButton(-1);
    }

    private static boolean shouldRestoreNativeMousePosition(Minecraft mc) {
        return mc != null
            && mc.isWindowActive()
            && WindowFocusService.isCursorInsideWindow(mc.getWindow());
    }

    private static void clearPendingRelativeMove() {
        PENDING_RELATIVE_DX.set(0);
        PENDING_RELATIVE_DY.set(0);
    }

    // 平滑逼近悬停目标速度，避免鼠标一进可交互区域就突变减速。
    private static double blendSpeedMultiplier(double current, double target) {
        return current + (target - current) * HOVER_SPEED_BLEND_FACTOR;
    }

    private static double getCursorSpeed(Minecraft mc) {
        return BASE_CURSOR_SPEED * Math.max(mc.getWindow().getGuiScale(), 1.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
