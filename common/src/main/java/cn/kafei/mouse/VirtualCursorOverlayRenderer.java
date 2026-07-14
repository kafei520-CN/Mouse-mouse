package cn.kafei.mouse;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class VirtualCursorOverlayRenderer {
    private static final int CROSS_ARM_LENGTH = 4;
    private static final int CROSS_CENTER_GAP = 1;
    private static final int OUTLINE_COLOR = 0xFF000000;

    private VirtualCursorOverlayRenderer() {
    }

    public static void renderFrameEndCursor(Minecraft mc, float partialTick) {
        if (!VirtualMouseService.shouldRenderOverlay(mc)) {
            return;
        }

        GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        RenderSystem.disableScissor();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, GuiGraphics.MAX_GUI_Z);
        int cursorX = (int) Math.round(VirtualMouseService.getRenderGuiX(mc, partialTick));
        int cursorY = (int) Math.round(VirtualMouseService.getRenderGuiY(mc, partialTick));
        int tint = VirtualMouseService.getCursorFillColor();
        drawCrosshair(graphics, cursorX, cursorY, OUTLINE_COLOR, 3);
        drawCrosshair(graphics, cursorX, cursorY, tint, 1);
        graphics.flush();
        graphics.pose().popPose();
    }

    // 手动画空心准星，避免贴图缩放和资源依赖。
    private static void drawCrosshair(GuiGraphics graphics, int centerX, int centerY, int color, int thickness) {
        int halfThickness = thickness / 2;
        int leftArmStartX = centerX - CROSS_CENTER_GAP - CROSS_ARM_LENGTH;
        int leftArmEndX = centerX - CROSS_CENTER_GAP;
        int rightArmStartX = centerX + CROSS_CENTER_GAP + 1;
        int rightArmEndX = centerX + CROSS_CENTER_GAP + CROSS_ARM_LENGTH + 1;
        int topArmStartY = centerY - CROSS_CENTER_GAP - CROSS_ARM_LENGTH;
        int topArmEndY = centerY - CROSS_CENTER_GAP;
        int bottomArmStartY = centerY + CROSS_CENTER_GAP + 1;
        int bottomArmEndY = centerY + CROSS_CENTER_GAP + CROSS_ARM_LENGTH + 1;

        graphics.fill(leftArmStartX, centerY - halfThickness, leftArmEndX, centerY + halfThickness + 1, color);
        graphics.fill(rightArmStartX, centerY - halfThickness, rightArmEndX, centerY + halfThickness + 1, color);
        graphics.fill(centerX - halfThickness, topArmStartY, centerX + halfThickness + 1, topArmEndY, color);
        graphics.fill(centerX - halfThickness, bottomArmStartY, centerX + halfThickness + 1, bottomArmEndY, color);
    }
}
