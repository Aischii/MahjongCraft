package doublemoon.mahjongcraft.mixin.client;

import doublemoon.mahjongcraft.client.game.ClientMahjongState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While seated at a mahjong game with no screen open, the mouse wheel moves the selection in the
 * hand HUD instead of the hotbar.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void mahjongcraft$scrollHand(long handle, double xoffset, double yoffset, CallbackInfo ci) {
		if (handle != minecraft.getWindow().handle() || minecraft.gui.screen() != null) return;
		if (!ClientMahjongState.INSTANCE.getActive() || yoffset == 0) return;
		ClientMahjongState.INSTANCE.scroll(yoffset > 0 ? -1 : 1);
		ci.cancel();
	}
}
