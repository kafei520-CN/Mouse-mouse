package cn.kafei.mouse;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class IPCClient {
    private static final IPCClient INSTANCE = new IPCClient();
    private final List<Connection> connections = new CopyOnWriteArrayList<>();

    public static volatile int lastActiveDeviceId = -1;

    public static IPCClient getInstance() { return INSTANCE; }

    public synchronized void connect(List<Integer> deviceIds) {
        for (int id : deviceIds) {
            Connection c = new Connection(id);
            connections.add(c);
            new Thread(c::run, "ipc-" + id).start();
        }
    }

    public synchronized void reconnect(List<Integer> deviceIds) {
        connections.forEach(Connection::close);
        connections.clear();
        connect(deviceIds);
    }

    public synchronized void disconnect() {
        connections.forEach(Connection::close);
        connections.clear();
    }

    private static class Connection {
        private final int deviceId;
        private volatile boolean running = true;
        private Socket socket;

        Connection(int deviceId) { this.deviceId = deviceId; }

        void run() {
            for (int i = 0; i < 40 && running; i++) {
                try {
                    socket = new Socket("127.0.0.1", 19091);
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                    out.println("CLAIM:" + deviceId);
                    String ack = readLine(in);
                    if (!ack.startsWith("ACK:")) {
                        MousemouseCommon.LOGGER.warn("Device {} rejected: {}", deviceId, ack);
                        socket.close();
                        return;
                    }
                    MousemouseCommon.LOGGER.info("Claimed device {}", deviceId);
                    receiveLoop(in);
                    return;
                } catch (ConnectException e) {
                    try { Thread.sleep(250); } catch (InterruptedException ie) { return; }
                } catch (IOException e) {
                    if (running) MousemouseCommon.LOGGER.warn("IPC error device {}: {}", deviceId, e.getMessage());
                    return;
                }
            }
        }

        private void receiveLoop(DataInputStream in) throws IOException {
            while (running) {
                int type = in.readUnsignedByte();
                IPCClient.lastActiveDeviceId = deviceId;
                if (type == 0x01) {
                    int vk = in.readUnsignedByte();
                    int state    = in.readUnsignedByte();
                    in.readUnsignedByte();
                    handleKey(vk, (state & 0x01) == 0);
                } else if (type == 0x02) {
                    short mouseState = readLittleShort(in);
                    short flags      = readLittleShort(in);
                    short rolling    = readLittleShort(in);
                    int   dx         = readLittleInt(in);
                    int   dy         = readLittleInt(in);
                    handleMouse(mouseState, flags, rolling, dx, dy);
                }
            }
        }

        private static String readLine(DataInputStream in) throws IOException {
            StringBuilder sb = new StringBuilder();
            int b;
            while ((b = in.read()) != -1 && b != '\n')
                if (b != '\r') sb.append((char) b);
            return sb.toString();
        }

        private static short readLittleShort(DataInputStream in) throws IOException {
            int b0 = in.readUnsignedByte();
            int b1 = in.readUnsignedByte();
            return (short) ((b1 << 8) | b0);
        }

        private static int readLittleInt(DataInputStream in) throws IOException {
            int b0 = in.readUnsignedByte();
            int b1 = in.readUnsignedByte();
            int b2 = in.readUnsignedByte();
            int b3 = in.readUnsignedByte();
            return (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
        }

        void close() {
            running = false;
            try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        }
    }

    private static void handleKey(int windowsVk, boolean pressed) {
        KeyboardInjectionService.handleKey(windowsVk, pressed);
    }

    private static void handleMouse(short state, short flags, short rolling, int dx, int dy) {
        Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (!WindowFocusService.shouldAcceptRoutedInput(mc)) {
                return;
            }
            InputIsolationService.RoutedInputMode routedInputMode = InputIsolationService.getRoutedInputMode(mc);
            if (routedInputMode.isScreen()) {
                VirtualMouseService.enqueueRelativeMove(dx, dy);
                handleVirtualMouseButtons(mc, state);
                VirtualMouseService.handleScroll(mc, rolling);
                return;
            }

            if (routedInputMode.isWorld() && (flags & 0x0001) == 0) {
                WorldMouseService.handleRelativeLook(mc, dx, dy);
            }
            setMouseButton(GLFW.GLFW_MOUSE_BUTTON_LEFT, (state & 0x0001) != 0, (state & 0x0002) != 0);
            setMouseButton(GLFW.GLFW_MOUSE_BUTTON_RIGHT, (state & 0x0004) != 0, (state & 0x0008) != 0);
            setMouseButton(GLFW.GLFW_MOUSE_BUTTON_MIDDLE, (state & 0x0010) != 0, (state & 0x0020) != 0);
            if (routedInputMode.isWorld() && rolling != 0 && mc.player != null) {
                int delta = rolling > 0 ? -1 : 1;
                mc.player.getInventory().selected =
                    (mc.player.getInventory().selected + delta + 9) % 9;
            }
        });
    }

    private static void handleVirtualMouseButtons(Minecraft mc, short state) {
        VirtualMouseService.handleButtonState(mc, GLFW.GLFW_MOUSE_BUTTON_LEFT,
            (state & 0x0001) != 0, (state & 0x0002) != 0);
        VirtualMouseService.handleButtonState(mc, GLFW.GLFW_MOUSE_BUTTON_RIGHT,
            (state & 0x0004) != 0, (state & 0x0008) != 0);
        VirtualMouseService.handleButtonState(mc, GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            (state & 0x0010) != 0, (state & 0x0020) != 0);
    }

    private static void setMouseButton(int btn, boolean down, boolean up) {
        InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(btn);
        if (down) { KeyMapping.set(key, true);  KeyMapping.click(key); }
        if (up)   { KeyMapping.set(key, false); }
    }
}
