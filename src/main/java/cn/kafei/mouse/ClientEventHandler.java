package cn.kafei.mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = Mousemouse.MODID, value = Dist.CLIENT)
public class ClientEventHandler {
    private static boolean notified = false;
    private static boolean safetyChordHeld = false;

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!SplitterLauncher.startFailed || notified) return;
        notified = true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.sendSystemMessage(Component.literal(
            "[DualMouse] Raw Input splitter failed to start. Check latest.log for details."));
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        VirtualMouseService.savePrevPosition();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        boolean chordDown = MousemouseClient.consumeSafetyConfigChord();
        WindowFocusService.updateNativeMouseCapture(mc, chordDown); // Keep the native mouse captured unless the safety chord is held.
        VirtualMouseService.refreshForCurrentScreen(mc);
        if (chordDown && !safetyChordHeld) {
            mc.setScreen(new DeviceSelectScreen(mc.screen));
        }
        safetyChordHeld = chordDown;
    }

    @SubscribeEvent
    public static void onShutdown(GameShuttingDownEvent event) {
        InjectedKeyboardState.clear();
        IPCClient.getInstance().disconnect();
        SplitterLauncher.stop();
    }
}
