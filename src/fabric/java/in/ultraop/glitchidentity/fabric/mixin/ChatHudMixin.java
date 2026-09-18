package in.ultraop.glitchidentity.fabric.mixin;

import in.ultraop.glitchidentity.fabric.AnimatedGlitchText;
import in.ultraop.glitchidentity.fabric.GlitchIdentityFabricClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Inject(
        method = "addMessage(Lnet/minecraft/text/Text;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void glitchidentity$replaceSimpleDeathMessage(
        Text message,
        CallbackInfo ci
    ) {
        replace(message, ci);
    }

    @Inject(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void glitchidentity$replaceSignedDeathMessage(
        Text message,
        MessageSignatureData signatureData,
        MessageIndicator indicator,
        CallbackInfo ci
    ) {
        replace(message, ci);
    }

    private void replace(Text message, CallbackInfo ci) {
        if (message instanceof AnimatedGlitchText) {
            return;
        }

        Text replacement = GlitchIdentityFabricClient.consumeDeathMessage(message);
        if (replacement == null) {
            return;
        }

        ci.cancel();
        ((ChatHud) (Object) this).addMessage(replacement);
    }
}
