package cn.kafei.mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.regex.*;

public class DeviceSelectScreen extends Screen {
    private static final int PANEL_WIDTH = 760;
    private static final int PANEL_PADDING = 12;

    private final Screen parent;
    private DeviceList deviceList;
    private final List<String> lines;
    private final Set<Integer> selected;

    public DeviceSelectScreen(Screen parent) {
        super(Component.literal("Mouse Device Selection"));
        this.parent = parent;
        this.lines = MouseConfig.readDeviceList();
        this.selected = new HashSet<>(InstanceDeviceSelectionService.getSelectedDeviceIds());
    }

    // ★★★ 关键修复：禁用父类的模糊背景，只用自己的半透明覆盖 ★★★
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 什么都不做，避免双重半透明叠加
    }

    @Override
    protected void init() {
        if (lines.isEmpty()) {
            addRenderableWidget(Button.builder(Component.literal("Refresh"), btn ->
                    minecraft.setScreen(new DeviceSelectScreen(parent))
            ).bounds(width / 2 - 50, height / 2 - 10, 100, 20).build());
        } else {
            int listWidth = Math.min(width - 40, PANEL_WIDTH);
            deviceList = new DeviceList(minecraft, listWidth, height - 76, 38, 20);
            deviceList.setX((width - listWidth) / 2);
            for (String line : lines) deviceList.add(new DeviceEntry(line));
            addWidget(deviceList);
        }

        addRenderableWidget(Button.builder(Component.literal("Save"), btn -> {
            ArrayList<Integer> selectedIds = new ArrayList<>(selected);
            InstanceDeviceSelectionService.setSelectedDeviceIds(selectedIds);
            InputIsolationService.setIsolationEnabled(!selectedIds.isEmpty());
            InjectedKeyboardState.clear();
            VirtualMouseService.reset();
            IPCClient.getInstance().reconnect(selectedIds);
            minecraft.setScreen(parent);
        }).bounds(width / 2 - 100, height - 28, 98, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), btn ->
                minecraft.setScreen(parent)
        ).bounds(width / 2 + 2, height - 28, 98, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        // 先画自己的半透明背景
        renderSoftBackground(g);
        // 再画组件
        if (deviceList != null) deviceList.render(g, mx, my, pt);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        if (lines.isEmpty()) {
            int color = SplitterLauncher.startFailed ? 0xFF5555 : 0xAAAAAA;
            g.drawCenteredString(font, SplitterLauncher.startFailed
                    ? "Raw Input splitter failed to start."
                    : "No Raw Input devices found yet.", width / 2, height / 2 - 30, color);
            g.drawCenteredString(font, "Move a mouse or press a key, then refresh.", width / 2, height / 2 - 18, 0xAAAAAA);
        }
        // ★★★ super.render 会调用 renderBackground，但我们已覆盖为空 ★★★
        super.render(g, mx, my, pt);
    }

    // 只压暗世界画面，不触发 Screen 默认的模糊后处理。
    private void renderSoftBackground(GuiGraphics g) {
        g.fill(0, 0, width, height, 0x99000000);
        g.fill(0, 0, width, 30, 0x77000000);
        g.fill(0, height - 42, width, height, 0x77000000);

        if (deviceList != null) {
            int left = deviceList.getX() - PANEL_PADDING;
            int top = deviceList.getY() - PANEL_PADDING;
            int right = deviceList.getRight() + PANEL_PADDING;
            int bottom = deviceList.getBottom() + PANEL_PADDING;
            g.fill(left, top, right, bottom, 0xAA111111);
            g.fill(left, top, right, top + 1, 0x55FFFFFF);
            g.fill(left, bottom - 1, right, bottom, 0x66000000);
        }
    }

    // 从行文本解析设备 ID，格式: "Device 3 [Mouse]: ..."
    private static final Pattern ID_PAT = Pattern.compile("Device\\s+(\\d+)");
    private static final Pattern DISPLAY_PAT = Pattern.compile("^Device\\s+\\d+\\s+(\\[[^\\]]+\\]:\\s*.+)$");
    private int parseId(String line) {
        Matcher m = ID_PAT.matcher(line);
        return m.find() ? Integer.parseInt(m.group(1)) : -1;
    }

    private String parseDisplayText(String line) {
        Matcher matcher = DISPLAY_PAT.matcher(line);
        return matcher.matches() ? matcher.group(1) : line;
    }

    class DeviceEntry extends ObjectSelectionList.Entry<DeviceEntry> {
        final String text;
        final String displayText;
        final int deviceId;

        DeviceEntry(String text) {
            this.text = text;
            this.displayText = parseDisplayText(text);
            this.deviceId = parseId(text);
        }

        @Override
        public boolean mouseClicked(double x, double y, int btn) {
            if (deviceId < 0) return false;
            if (selected.contains(deviceId)) selected.remove(deviceId);
            else selected.add(deviceId);
            return true;
        }

        @Override
        public void render(GuiGraphics g, int idx, int top, int left, int w, int h,
                           int mx, int my, boolean hovered, float pt) {
            boolean on = selected.contains(deviceId);
            g.drawString(Minecraft.getInstance().font,
                    (on ? "[x] " : "[ ] ") + displayText, left + 4, top + 4, on ? 0x55FF55 : 0xAAAAAA);
        }

        @Override public Component getNarration() { return Component.literal(displayText); }
    }

    class DeviceList extends ObjectSelectionList<DeviceEntry> {
        DeviceList(Minecraft mc, int w, int h, int y, int itemH) {
            super(mc, w, h, y, itemH);
        }
        void add(DeviceEntry e) { super.addEntry(e); }
        @Override protected void renderListBackground(GuiGraphics g) {}
        @Override protected void renderListSeparators(GuiGraphics g) {}
        @Override protected int getScrollbarPosition() { return getRight() - 8; }
        @Override public int getRowWidth() { return getWidth() - 24; }
    }
}
