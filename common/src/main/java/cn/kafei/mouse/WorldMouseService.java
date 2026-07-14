package cn.kafei.mouse;

import net.minecraft.client.Minecraft;

public final class WorldMouseService {
    private WorldMouseService() {
    }

    public static volatile double lookSensitivity = 1.0;
    public static volatile double screenSensitivity = 1.0;

    // Speed (in raw input units) below which no acceleration is applied.
    private static final double ACCEL_THRESHOLD = 20.0;
    // Exponent of the acceleration curve above the threshold.
    private static final double ACCEL_CURVE = 1.15;

    // Apply Raw Input deltas with Minecraft's own sensitivity curve and a
    // real-mouse-style speed-dependent acceleration curve.
    public static void handleRelativeLook(Minecraft mc, int dx, int dy) {
        if (mc == null || mc.player == null || (dx == 0 && dy == 0)) return;

        double sensitivity = mc.options.sensitivity().get();
        double base = sensitivity * 0.6D + 0.2D;
        double cubic = base * base * base;
        double factor = cubic * 8.0D;

        if (mc.options.getCameraType().isFirstPerson() && mc.player.isScoping()) {
            factor = cubic;
        }

        double speed = Math.sqrt((double) dx * dx + (double) dy * dy);
        double accelGain = speed > ACCEL_THRESHOLD
                ? Math.pow(speed / ACCEL_THRESHOLD, ACCEL_CURVE)
                : 1.0;

        int invert = mc.options.invertYMouse().get() ? -1 : 1;
        double turnX = dx * factor * accelGain * lookSensitivity;
        double turnY = dy * factor * invert * accelGain * lookSensitivity;

        mc.getTutorial().onMouse(turnX, turnY);
        mc.player.turn(turnX, turnY);
    }
}
