package in.ultraop.glitchidentity.fabric.mixin;

import in.ultraop.glitchidentity.fabric.GlitchIdentityFabricClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.client.gui.hud.MessageIndicator;
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
        cancellable = true,
        require = 1
    )
    private void glitchIdentity$replaceSimpleMessage(Text message, CallbackInfo ci) {
        Text replacement = GlitchIdentityFabricClient.consumeDeathMessage(message);
        if (replacement != null) {
            ci.cancel();
            GlitchIdentityFabricClient.addReplacement((ChatHud) (Object) this, replacement);
        }
    }

    @Inject(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 1
    )
    private void glitchIdentity$replaceSignedMessage(
        Text message,
        MessageSignatureData signatureData,
        MessageIndicator indicator,
        CallbackInfo ci
    ) {
        Text replacement = GlitchIdentityFabricClient.consumeDeathMessage(message);
        if (replacement != null) {
            ci.cancel();
            GlitchIdentityFabricClient.addReplacement((ChatHud) (Object) this, replacement);
        }
    }
}
