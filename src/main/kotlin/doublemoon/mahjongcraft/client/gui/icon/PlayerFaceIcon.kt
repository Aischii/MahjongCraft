package doublemoon.mahjongcraft.client.gui.icon

import doublemoon.mahjongcraft.util.RenderHelper
import io.github.cottonmc.cotton.gui.widget.icon.Icon
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.component.ResolvableProfile
import java.util.*

class PlayerFaceIcon(
    val uuid: UUID,
    val name: String,
) : Icon {
    /** Minecraft resolves the skin in the background; the default face shows until then. */
    private val profile = ResolvableProfile.createUnresolved(uuid)

    @Environment(EnvType.CLIENT)
    override fun paint(context: GuiGraphicsExtractor, x: Int, y: Int, size: Int) {
        RenderHelper.renderPlayerFace(context, profile, x, y, size)
    }
}
