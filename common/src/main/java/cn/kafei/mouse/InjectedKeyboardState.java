package cn.kafei.mouse;

import org.lwjgl.glfw.GLFW;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InjectedKeyboardState {
    private static final Set<Integer> PRESSED_GLFW_KEYS = ConcurrentHashMap.newKeySet();

    private InjectedKeyboardState() {
    }

    public static void setKeyPressed(int glfwKey, boolean pressed) {
        if (pressed) {
            PRESSED_GLFW_KEYS.add(glfwKey);
        } else {
            PRESSED_GLFW_KEYS.remove(glfwKey);
        }
    }

    public static boolean isKeyPressed(int glfwKey) {
        return PRESSED_GLFW_KEYS.contains(glfwKey);
    }

    public static int getModifierMask(int glfwKey, boolean pressed) {
        boolean shift = isModifierPressed(GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT)
            || (pressed && isShiftKey(glfwKey));
        boolean control = isModifierPressed(GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL)
            || (pressed && isControlKey(glfwKey));
        boolean alt = isModifierPressed(GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT)
            || (pressed && isAltKey(glfwKey));
        boolean superKey = isModifierPressed(GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER)
            || (pressed && isSuperKey(glfwKey));

        int modifiers = 0;
        if (shift) modifiers |= GLFW.GLFW_MOD_SHIFT;
        if (control) modifiers |= GLFW.GLFW_MOD_CONTROL;
        if (alt) modifiers |= GLFW.GLFW_MOD_ALT;
        if (superKey) modifiers |= GLFW.GLFW_MOD_SUPER;
        return modifiers;
    }

    public static boolean isSafetyChordPressed() {
        return (isKeyPressed(GLFW.GLFW_KEY_LEFT_ALT) || isKeyPressed(GLFW.GLFW_KEY_RIGHT_ALT))
            && isKeyPressed(GLFW.GLFW_KEY_F8);
    }

    public static void clear() {
        PRESSED_GLFW_KEYS.clear();
    }

    private static boolean isModifierPressed(int leftKey, int rightKey) {
        return isKeyPressed(leftKey) || isKeyPressed(rightKey);
    }

    private static boolean isShiftKey(int glfwKey) {
        return glfwKey == GLFW.GLFW_KEY_LEFT_SHIFT || glfwKey == GLFW.GLFW_KEY_RIGHT_SHIFT;
    }

    private static boolean isControlKey(int glfwKey) {
        return glfwKey == GLFW.GLFW_KEY_LEFT_CONTROL || glfwKey == GLFW.GLFW_KEY_RIGHT_CONTROL;
    }

    private static boolean isAltKey(int glfwKey) {
        return glfwKey == GLFW.GLFW_KEY_LEFT_ALT || glfwKey == GLFW.GLFW_KEY_RIGHT_ALT;
    }

    private static boolean isSuperKey(int glfwKey) {
        return glfwKey == GLFW.GLFW_KEY_LEFT_SUPER || glfwKey == GLFW.GLFW_KEY_RIGHT_SUPER;
    }
}
