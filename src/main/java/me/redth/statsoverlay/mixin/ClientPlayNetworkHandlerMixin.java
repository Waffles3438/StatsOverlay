package me.redth.statsoverlay.mixin;

import me.redth.statsoverlay.EventListener;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.text.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands chat that came from the server to StatsOverlay. Messages other mods print locally
 * (such as Additional's /bw output) never pass through here, so they can't be mistaken
 * for players chatting.
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    // Runs on the client thread, only for chat (not action bar) messages.
    @Inject(
        method = "handleChatMessage",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/chat/ChatGui;addMessage(Lnet/minecraft/text/Text;)V")
    )
    private void statsoverlay$onServerChat(ChatMessageS2CPacket packet, CallbackInfo ci) {
        EventListener.INSTANCE.onServerChat(Formatting.strip(packet.getMessage().getString()));
    }
}
