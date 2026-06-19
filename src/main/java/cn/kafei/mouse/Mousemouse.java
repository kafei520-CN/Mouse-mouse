package cn.kafei.mouse;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(Mousemouse.MODID)
public class Mousemouse {
    public static final String MODID = "mouse";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Mousemouse(IEventBus modEventBus, ModContainer modContainer) {
    }
}
