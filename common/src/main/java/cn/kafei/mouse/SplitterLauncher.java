package cn.kafei.mouse;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;

public class SplitterLauncher {
    private static final int PORT = 19091;

    public static volatile boolean startFailed = false;

    private static Process process;
    private static boolean started = false;
    private static boolean ownsProcess = false;

    public static synchronized void start(Path gameDir) {
        if (started) return;
        started = true;
        startFailed = false;

        Path referenceExe = resolveExe(gameDir);
        if (referenceExe == null) {
            startFailed = true;
            return;
        }

        if (isServerAvailable()) {
            if (restartExistingServerIfOutdated(referenceExe)) {
                MousemouseCommon.LOGGER.info("Replaced outdated Raw Input splitter on port {}", PORT);
            } else {
                MousemouseCommon.LOGGER.info("Using existing Raw Input splitter on port {}", PORT);
                return;
            }
        }

        try {
            process = new ProcessBuilder(referenceExe.toString()).redirectErrorStream(true).start();
            ownsProcess = true;
            new Thread(() -> {
                try (var r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) MousemouseCommon.LOGGER.info("[splitter] {}", line);
                } catch (IOException ignored) {}
            }, "splitter-log").start();
            Thread.sleep(1500);
            startFailed = !isServerAvailable();
        } catch (Exception e) {
            startFailed = true;
            MousemouseCommon.LOGGER.error("Failed to start splitter", e);
        }
    }

    private static Path resolveExe(Path gameDir) {
        Path modsExe = gameDir.resolve("mods").resolve("splitter.exe");
        if (Files.exists(modsExe)) {
            MousemouseCommon.LOGGER.info("Using external splitter.exe from mods folder: {}", modsExe);
            return modsExe;
        }
        return extractBundledExeToTemp();
    }

    private static Path extractBundledExeToTemp() {
        try {
            Path exe = Files.createTempFile("splitter", ".exe");
            exe.toFile().deleteOnExit();
            try (InputStream in = SplitterLauncher.class.getResourceAsStream("/assets/mouse/splitter.exe")) {
                if (in == null) {
                    MousemouseCommon.LOGGER.error("splitter.exe missing from jar");
                    return null;
                }
                Files.copy(in, exe, StandardCopyOption.REPLACE_EXISTING);
            }
            return exe;
        } catch (IOException e) {
            MousemouseCommon.LOGGER.error("Failed to extract splitter", e);
            return null;
        }
    }

    private static boolean restartExistingServerIfOutdated(Path bundledExe) {
        Path runningExe = findListeningSplitterExe();
        if (runningExe == null) {
            return false;
        }

        try {
            if (Files.isSameFile(bundledExe, runningExe) || fileHash(bundledExe).equals(fileHash(runningExe))) {
                return false;
            }
        } catch (Exception e) {
            MousemouseCommon.LOGGER.warn("Failed to compare splitter binaries: {}", e.getMessage());
            return false;
        }

        long pid = findListeningSplitterPid();
        if (pid <= 0) {
            return false;
        }

        ProcessHandle.of(pid).ifPresent(handle -> {
            MousemouseCommon.LOGGER.info("Stopping outdated Raw Input splitter pid {}", pid);
            handle.destroy();
            try {
                if (!handle.onExit().get().isAlive()) {
                    return;
                }
            } catch (Exception ignored) {}
            handle.destroyForcibly();
        });

        for (int i = 0; i < 10; i++) {
            if (!isServerAvailable()) {
                return true;
            }
            try {
                Thread.sleep(150);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return !isServerAvailable();
    }

    private static long findListeningSplitterPid() {
        try {
            ProcessHandle current = ProcessHandle.current();
            return ProcessHandle.allProcesses()
                .filter(ProcessHandle::isAlive)
                .filter(handle -> handle.pid() != current.pid())
                .filter(handle -> isListeningSplitterProcess(handle, PORT))
                .mapToLong(ProcessHandle::pid)
                .findFirst()
                .orElse(-1L);
        } catch (Exception e) {
            MousemouseCommon.LOGGER.warn("Failed to locate listening splitter pid: {}", e.getMessage());
            return -1L;
        }
    }

    private static Path findListeningSplitterExe() {
        long pid = findListeningSplitterPid();
        if (pid <= 0) {
            return null;
        }

        return ProcessHandle.of(pid)
            .flatMap(handle -> handle.info().command().map(Path::of))
            .orElse(null);
    }

    private static boolean isListeningSplitterProcess(ProcessHandle handle, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 250);
            String command = handle.info().command().orElse("");
            return command.toLowerCase().contains("splitter");
        } catch (IOException e) {
            return false;
        }
    }

    private static String fileHash(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static boolean isServerAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", PORT), 250);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static synchronized void stop() {
        if (ownsProcess && process != null && process.isAlive()) {
            MousemouseCommon.LOGGER.info("Leaving shared Raw Input splitter running for other instances.");
        }
        process = null;
        started = false;
    }
}
