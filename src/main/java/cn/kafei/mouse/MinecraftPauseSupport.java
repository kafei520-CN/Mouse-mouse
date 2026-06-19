package cn.kafei.mouse;

import net.minecraft.client.Minecraft;

public final class MinecraftPauseSupport {
    private MinecraftPauseSupport() {
    }

    // 关闭失焦暂停，避免非激活实例冻结游戏逻辑。
    public static void configure() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.pauseOnLostFocus = false;
    }
}
