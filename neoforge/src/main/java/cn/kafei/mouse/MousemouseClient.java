package cn.kafei.mouse;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@Mod(value = MousemouseCommon.MODID, dist = Dist.CLIENT)
public class MousemouseClient {
    public MousemouseClient(IEventBus modEventBus, ModContainer container) {
        MousemouseCommon.CONFIG_KEY = new KeyMapping(
            "key.mouse.config", GLFW.GLFW_KEY_UNKNOWN, "key.categories.misc"
        );
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterKeys);
    }

    private void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(MousemouseCommon.CONFIG_KEY);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(MousemouseCommon::initClient);
    }
}
