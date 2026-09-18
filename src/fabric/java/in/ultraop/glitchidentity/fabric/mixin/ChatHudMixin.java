package in.ultraop.glitchidentity.fabric.mixin;

import in.ultraop.glitchidentity.fabric.GlitchIdentityFabricClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Inject(
        method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V",
        at = @At("TAIL")
    )
    private void glitchIdentity$renderAnimatedDeath(
        DrawContext draw,
        TextRenderer textRenderer,
        int currentTick,
        int mouseX,
        int mouseY,
        boolean interactable,
        boolean bool,
        CallbackInfo ci
    ) {
        GlitchIdentityFabricClient.renderChatLine(draw, textRenderer);
    }
}
