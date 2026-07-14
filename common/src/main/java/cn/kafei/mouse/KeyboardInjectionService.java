package cn.kafei.mouse;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class KeyboardInjectionService {
    private KeyboardInjectionService() {
    }

    // 通过 KeyboardHandler 注入键盘事件，让目标实例按原版路径处理 ESC、聊天、菜单和绑定键。
    public static void handleKey(int windowsVk, boolean pressed) {
        Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (!WindowFocusService.shouldAcceptRoutedInput(mc)) {
                return;
            }
            int glfwKey = KeyCodeMapper.windowsVkToGlfw(windowsVk);
            if (glfwKey == GLFW.GLFW_KEY_UNKNOWN) {
                MousemouseCommon.LOGGER.debug("Ignored unsupported keyboard VK {}", windowsVk);
                return;
            }

            int scanCode = KeyCodeMapper.windowsVkToScanCode(windowsVk);
            int action = pressed ? GLFW.GLFW_PRESS : GLFW.GLFW_RELEASE;
            InjectedKeyboardState.setKeyPressed(glfwKey, pressed);
            int modifiers = InjectedKeyboardState.getModifierMask(glfwKey, pressed);
            InputIsolationService.beginInjectedKeyboardEvent();
            try {
                mc.keyboardHandler.keyPress(mc.getWindow().getWindow(), glfwKey, scanCode, action, modifiers);
            } finally {
                InputIsolationService.endInjectedKeyboardEvent();
            }
        });
    }
}
