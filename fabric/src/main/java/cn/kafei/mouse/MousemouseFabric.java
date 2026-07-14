package cn.kafei.mouse;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class MousemouseFabric implements ClientModInitializer {
    private static boolean notified = false;

    @Override
    public void onInitializeClient() {
        MousemouseCommon.CONFIG_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.mouse.config", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc")
        );

        ClientLifecycleEvents.CLIENT_STARTED.register(mc -> MousemouseCommon.initClient());
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> MousemouseCommon.onShutdown());
        ClientTickEvents.START_CLIENT_TICK.register(mc -> MousemouseCommon.onTickStart());
        ClientTickEvents.END_CLIENT_TICK.register(mc -> MousemouseCommon.onTickEnd());

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (!SplitterLauncher.startFailed || notified) return;
            notified = true;
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendSystemMessage(Component.translatable("mouse.chat.splitter_failed"));
                }
            });
        });
    }
}
