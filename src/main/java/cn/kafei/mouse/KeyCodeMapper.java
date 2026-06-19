package cn.kafei.mouse;

import org.lwjgl.glfw.GLFW;

public final class KeyCodeMapper {
    private KeyCodeMapper() {
    }

    // 将 Windows Virtual-Key 转成 Minecraft 使用的 GLFW key symbol。
    public static int windowsVkToGlfw(int vk) {
        if (vk >= '0' && vk <= '9') return vk;
        if (vk >= 'A' && vk <= 'Z') return vk;

        return switch (vk) {
            case 0x08 -> GLFW.GLFW_KEY_BACKSPACE;
            case 0x09 -> GLFW.GLFW_KEY_TAB;
            case 0x0D -> GLFW.GLFW_KEY_ENTER;
            case 0x10 -> GLFW.GLFW_KEY_LEFT_SHIFT;
            case 0x11 -> GLFW.GLFW_KEY_LEFT_CONTROL;
            case 0x12 -> GLFW.GLFW_KEY_LEFT_ALT;
            case 0x13 -> GLFW.GLFW_KEY_PAUSE;
            case 0x14 -> GLFW.GLFW_KEY_CAPS_LOCK;
            case 0x1B -> GLFW.GLFW_KEY_ESCAPE;
            case 0x20 -> GLFW.GLFW_KEY_SPACE;
            case 0x21 -> GLFW.GLFW_KEY_PAGE_UP;
            case 0x22 -> GLFW.GLFW_KEY_PAGE_DOWN;
            case 0x23 -> GLFW.GLFW_KEY_END;
            case 0x24 -> GLFW.GLFW_KEY_HOME;
            case 0x25 -> GLFW.GLFW_KEY_LEFT;
            case 0x26 -> GLFW.GLFW_KEY_UP;
            case 0x27 -> GLFW.GLFW_KEY_RIGHT;
            case 0x28 -> GLFW.GLFW_KEY_DOWN;
            case 0x2C -> GLFW.GLFW_KEY_PRINT_SCREEN;
            case 0x2D -> GLFW.GLFW_KEY_INSERT;
            case 0x2E -> GLFW.GLFW_KEY_DELETE;
            case 0x5B -> GLFW.GLFW_KEY_LEFT_SUPER;
            case 0x5C -> GLFW.GLFW_KEY_RIGHT_SUPER;
            case 0x60 -> GLFW.GLFW_KEY_KP_0;
            case 0x61 -> GLFW.GLFW_KEY_KP_1;
            case 0x62 -> GLFW.GLFW_KEY_KP_2;
            case 0x63 -> GLFW.GLFW_KEY_KP_3;
            case 0x64 -> GLFW.GLFW_KEY_KP_4;
            case 0x65 -> GLFW.GLFW_KEY_KP_5;
            case 0x66 -> GLFW.GLFW_KEY_KP_6;
            case 0x67 -> GLFW.GLFW_KEY_KP_7;
            case 0x68 -> GLFW.GLFW_KEY_KP_8;
            case 0x69 -> GLFW.GLFW_KEY_KP_9;
            case 0x6A -> GLFW.GLFW_KEY_KP_MULTIPLY;
            case 0x6B -> GLFW.GLFW_KEY_KP_ADD;
            case 0x6D -> GLFW.GLFW_KEY_KP_SUBTRACT;
            case 0x6E -> GLFW.GLFW_KEY_KP_DECIMAL;
            case 0x6F -> GLFW.GLFW_KEY_KP_DIVIDE;
            case 0x70 -> GLFW.GLFW_KEY_F1;
            case 0x71 -> GLFW.GLFW_KEY_F2;
            case 0x72 -> GLFW.GLFW_KEY_F3;
            case 0x73 -> GLFW.GLFW_KEY_F4;
            case 0x74 -> GLFW.GLFW_KEY_F5;
            case 0x75 -> GLFW.GLFW_KEY_F6;
            case 0x76 -> GLFW.GLFW_KEY_F7;
            case 0x77 -> GLFW.GLFW_KEY_F8;
            case 0x78 -> GLFW.GLFW_KEY_F9;
            case 0x79 -> GLFW.GLFW_KEY_F10;
            case 0x7A -> GLFW.GLFW_KEY_F11;
            case 0x7B -> GLFW.GLFW_KEY_F12;
            case 0x90 -> GLFW.GLFW_KEY_NUM_LOCK;
            case 0x91 -> GLFW.GLFW_KEY_SCROLL_LOCK;
            case 0xBA -> GLFW.GLFW_KEY_SEMICOLON;
            case 0xBB -> GLFW.GLFW_KEY_EQUAL;
            case 0xBC -> GLFW.GLFW_KEY_COMMA;
            case 0xBD -> GLFW.GLFW_KEY_MINUS;
            case 0xBE -> GLFW.GLFW_KEY_PERIOD;
            case 0xBF -> GLFW.GLFW_KEY_SLASH;
            case 0xC0 -> GLFW.GLFW_KEY_GRAVE_ACCENT;
            case 0xDB -> GLFW.GLFW_KEY_LEFT_BRACKET;
            case 0xDC -> GLFW.GLFW_KEY_BACKSLASH;
            case 0xDD -> GLFW.GLFW_KEY_RIGHT_BRACKET;
            case 0xDE -> GLFW.GLFW_KEY_APOSTROPHE;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };
    }

    public static int windowsVkToScanCode(int vk) {
        return switch (vk) {
            case 0x1B -> 1;
            case 0x31 -> 2;
            case 0x32 -> 3;
            case 0x33 -> 4;
            case 0x34 -> 5;
            case 0x35 -> 6;
            case 0x36 -> 7;
            case 0x37 -> 8;
            case 0x38 -> 9;
            case 0x39 -> 10;
            case 0x30 -> 11;
            case 0xBD -> 12;
            case 0xBB -> 13;
            case 0x08 -> 14;
            case 0x09 -> 15;
            case 0x51 -> 16;
            case 0x57 -> 17;
            case 0x45 -> 18;
            case 0x52 -> 19;
            case 0x54 -> 20;
            case 0x59 -> 21;
            case 0x55 -> 22;
            case 0x49 -> 23;
            case 0x4F -> 24;
            case 0x50 -> 25;
            case 0xDB -> 26;
            case 0xDD -> 27;
            case 0x0D -> 28;
            case 0x11 -> 29;
            case 0x41 -> 30;
            case 0x53 -> 31;
            case 0x44 -> 32;
            case 0x46 -> 33;
            case 0x47 -> 34;
            case 0x48 -> 35;
            case 0x4A -> 36;
            case 0x4B -> 37;
            case 0x4C -> 38;
            case 0xBA -> 39;
            case 0xDE -> 40;
            case 0xC0 -> 41;
            case 0x10 -> 42;
            case 0xDC -> 43;
            case 0x5A -> 44;
            case 0x58 -> 45;
            case 0x43 -> 46;
            case 0x56 -> 47;
            case 0x42 -> 48;
            case 0x4E -> 49;
            case 0x4D -> 50;
            case 0xBC -> 51;
            case 0xBE -> 52;
            case 0xBF -> 53;
            case 0x12 -> 56;
            case 0x20 -> 57;
            case 0x70 -> 59;
            case 0x71 -> 60;
            case 0x72 -> 61;
            case 0x73 -> 62;
            case 0x74 -> 63;
            case 0x75 -> 64;
            case 0x76 -> 65;
            case 0x77 -> 66;
            case 0x78 -> 67;
            case 0x79 -> 68;
            case 0x7A -> 87;
            case 0x7B -> 88;
            case 0x26 -> 72;
            case 0x25 -> 75;
            case 0x27 -> 77;
            case 0x28 -> 80;
            case 0x2D -> 82;
            case 0x2E -> 83;
            default -> 0;
        };
    }

}
