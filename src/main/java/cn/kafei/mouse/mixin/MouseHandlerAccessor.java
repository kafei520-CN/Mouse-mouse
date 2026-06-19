package cn.kafei.mouse.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("xpos")
    void mouse$setXpos(double xpos);

    @Accessor("ypos")
    void mouse$setYpos(double ypos);

    @Accessor("activeButton")
    void mouse$setActiveButton(int activeButton);

    @Accessor("mousePressedTime")
    void mouse$setMousePressedTime(double mousePressedTime);

    @Accessor("mouseGrabbed")
    void mouse$setMouseGrabbed(boolean mouseGrabbed);

    @Invoker("setIgnoreFirstMove")
    void mouse$invokeSetIgnoreFirstMove();
}
