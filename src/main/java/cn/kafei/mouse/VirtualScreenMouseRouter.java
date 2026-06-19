package cn.kafei.mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.ClientHooks;

public final class VirtualScreenMouseRouter {
    private static final double WHEEL_DELTA = 120.0;

    private VirtualScreenMouseRouter() {
    }

    public static void dispatchMove(Screen screen, double guiX, double guiY, int activeButton, double dragX, double dragY) {
        Screen.wrapScreenError(() -> screen.mouseMoved(guiX, guiY), "mouseMoved event handler", screen.getClass().getCanonicalName());
        if (activeButton != -1) {
            Screen.wrapScreenError(() -> {
                if (ClientHooks.onScreenMouseDragPre(screen, guiX, guiY, activeButton, dragX, dragY)) {
                    return;
                }
                if (screen.mouseDragged(guiX, guiY, activeButton, dragX, dragY)) {
                    return;
                }
                ClientHooks.onScreenMouseDragPost(screen, guiX, guiY, activeButton, dragX, dragY);
            }, "mouseDragged event handler", screen.getClass().getCanonicalName());
        }
        screen.afterMouseMove();
    }

    public static void dispatchClick(Screen screen, double guiX, double guiY, int button) {
        screen.afterMouseAction();
        boolean[] handled = new boolean[]{false};
        Screen.wrapScreenError(() -> {
            handled[0] = ClientHooks.onScreenMouseClickedPre(screen, guiX, guiY, button);
            if (!handled[0]) {
                handled[0] = screen.mouseClicked(guiX, guiY, button);
                handled[0] = ClientHooks.onScreenMouseClickedPost(screen, guiX, guiY, button, handled[0]);
            }
        }, "mouseClicked event handler", screen.getClass().getCanonicalName());
    }

    public static void dispatchRelease(Screen screen, double guiX, double guiY, int button) {
        boolean[] handled = new boolean[]{false};
        Screen.wrapScreenError(() -> {
            handled[0] = ClientHooks.onScreenMouseReleasedPre(screen, guiX, guiY, button);
            if (!handled[0]) {
                handled[0] = screen.mouseReleased(guiX, guiY, button);
                handled[0] = ClientHooks.onScreenMouseReleasedPost(screen, guiX, guiY, button, handled[0]);
            }
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
        if (!ClientHooks.onScreenMouseScrollPre(mc.mouseHandler, screen, 0.0, scrollOffsetY)) {
            if (!screen.mouseScrolled(guiX, guiY, 0.0, scrollOffsetY)) {
                ClientHooks.onScreenMouseScrollPost(mc.mouseHandler, screen, 0.0, scrollOffsetY);
            }
        }
        screen.afterMouseAction();
    }
}
