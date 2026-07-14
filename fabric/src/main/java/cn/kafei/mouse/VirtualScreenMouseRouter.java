package cn.kafei.mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class VirtualScreenMouseRouter {
    private static final double WHEEL_DELTA = 120.0;

    private VirtualScreenMouseRouter() {
    }

    public static void dispatchMove(Screen screen, double guiX, double guiY, int activeButton, double dragX, double dragY) {
        Screen.wrapScreenError(() -> screen.mouseMoved(guiX, guiY), "mouseMoved event handler", screen.getClass().getCanonicalName());
        if (activeButton != -1) {
            Screen.wrapScreenError(() -> {
                screen.mouseDragged(guiX, guiY, activeButton, dragX, dragY);
            }, "mouseDragged event handler", screen.getClass().getCanonicalName());
        }
        screen.afterMouseMove();
    }

    public static void dispatchClick(Screen screen, double guiX, double guiY, int button) {
        screen.afterMouseAction();
        Screen.wrapScreenError(() -> {
            screen.mouseClicked(guiX, guiY, button);
        }, "mouseClicked event handler", screen.getClass().getCanonicalName());
    }

    public static void dispatchRelease(Screen screen, double guiX, double guiY, int button) {
        Screen.wrapScreenError(() -> {
            screen.mouseReleased(guiX, guiY, button);
        }, "mouseReleased event handler", screen.getClass().getCanonicalName());
    }

    public static void dispatchScroll(Minecraft mc, Screen screen, double guiX, double guiY, int rolling) {
        double rawOffsetY = rolling / WHEEL_DELTA;
        if (rawOffsetY == 0.0) {
            return;
        }
        boolean discreteScroll = mc.options.discreteMouseScroll().get();
        double scrollSensitivity = mc.options.mouseWheelSensitivity().get();
        double scrollOffsetY = (discreteScroll ? Math.signum(rawOffsetY) : rawOffsetY) * scrollSensitivity;
        screen.mouseScrolled(guiX, guiY, 0.0, scrollOffsetY);
        screen.afterMouseAction();
    }
}
