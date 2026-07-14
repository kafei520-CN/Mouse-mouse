package cn.kafei.mouse;

import cn.kafei.mouse.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;

import java.util.List;

public final class ScreenInteractionHelper {
    private ScreenInteractionHelper() {
    }

    public static boolean isHoveringInteractable(Screen screen, double guiX, double guiY) {
        return findHoveredListener(screen.children(), guiX, guiY) != null;
    }

    public static Slot getHoveredSlot(Screen screen) {
        if (screen instanceof AbstractContainerScreenAccessor accessor) {
            return accessor.getHoveredSlot();
        }
        return null;
    }

    public static boolean isHoveringContainerSlot(Screen screen) {
        Slot slot = getHoveredSlot(screen);
        return slot != null && slot.isActive();
    }

    private static GuiEventListener findHoveredListener(List<? extends GuiEventListener> listeners, double guiX, double guiY) {
        for (GuiEventListener listener : listeners) {
            if (!listener.isMouseOver(guiX, guiY)) {
                continue;
            }
            if (listener instanceof ContainerEventHandler handler) {
                GuiEventListener child = findHoveredListener(handler.children(), guiX, guiY);
                if (child != null) {
                    return child;
                }
            }
            return listener;
        }
        return null;
    }
}
