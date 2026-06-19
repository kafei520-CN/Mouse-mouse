package cn.kafei.mouse;

import net.minecraft.client.Minecraft;

public final class WorldMouseService {
    private WorldMouseService() {
    }

    // Apply Raw Input deltas with Minecraft's own sensitivity curve.
    public static void handleRelativeLook(Minecraft mc, int dx, int dy) {
        if (mc == null || mc.player == null || (dx == 0 && dy == 0)) return;

        double sensitivity = mc.options.sensitivity().get();
        double base = sensitivity * 0.6D + 0.2D;
        double cubic = base * base * base;
        double factor = cubic * 8.0D;

        if (mc.options.getCameraType().isFirstPerson() && mc.player.isScoping()) {
            factor = cubic;
        }

        int invert = mc.options.invertYMouse().get() ? -1 : 1;
        double turnX = dx * factor;
        double turnY = dy * factor * invert;

        mc.getTutorial().onMouse(turnX, turnY);
        mc.player.turn(turnX, turnY);
    }
}
