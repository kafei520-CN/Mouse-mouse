package cn.kafei.mouse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class MouseConfig {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 19091;
    private static final int CONNECT_TIMEOUT_MS = 500;

    private MouseConfig() {
    }

    // 通过 splitter 运行时连接拉取当前设备列表，不再依赖任何本地缓存文件。
    public static List<String> readDeviceList() {
        List<String> result = new ArrayList<>();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PORT), CONNECT_TIMEOUT_MS);
            try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                writer.println("LIST");
                String line;
                while ((line = reader.readLine()) != null) {
                    if ("END".equals(line)) {
                        break;
                    }
                    if (!line.isBlank()) {
                        result.add(line.trim());
                    }
                }
            }
        } catch (IOException e) {
            Mousemouse.LOGGER.warn("Failed to fetch device list from splitter: {}", e.getMessage());
        }
        return result;
    }
}
