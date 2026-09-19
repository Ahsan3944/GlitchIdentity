package in.ultraop.glitchidentity.fabric.mixin;

import in.ultraop.glitchidentity.fabric.GlitchIdentityFabricClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(
        method = "onDeathMessage(Lnet/minecraft/network/packet/s2c/play/DeathMessageS2CPacket;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void glitchidentity$replaceDeathPacket(
        DeathMessageS2CPacket packet,
        CallbackInfo ci
    ) {
        Text replacement = GlitchIdentityFabricClient.consumeDeathMessage(
            packet.message(),
            packet.playerId()
        );

        if (replacement == null) {
            return;
        }

        ci.cancel();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(replacement);
        }
    }
}
