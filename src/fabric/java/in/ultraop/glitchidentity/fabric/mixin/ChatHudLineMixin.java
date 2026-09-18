package in.ultraop.glitchidentity.fabric.mixin;

import in.ultraop.glitchidentity.fabric.AnimatedGlitchText;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ChatHudLine.class)
public abstract class ChatHudLineMixin {
    @Inject(method = "breakLines", at = @At("HEAD"), cancellable = true)
    private void glitchidentity$keepAnimatedText(
        TextRenderer textRenderer,
        int width,
        CallbackInfoReturnable<List<OrderedText>> cir
    ) {
        ChatHudLine line = (ChatHudLine) (Object) this;

        if (line.content() instanceof AnimatedGlitchText animated) {
            cir.setReturnValue(List.of(animated.asOrderedText()));
        }
    }
}
