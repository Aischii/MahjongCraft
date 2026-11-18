package doublemoon.mahjongcraft.client.gui.widget

import doublemoon.mahjongcraft.util.RenderHelper
import io.github.cottonmc.cotton.gui.widget.WWidget
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.component.ResolvableProfile
import java.util.*
import kotlin.math.min

class WPlayerFace(
    uuid: UUID,
    name: String,
) : WWidget() {

    // constructor 不能含有 private set 只好這樣用
    var uuid: UUID = uuid
        private set
    var name: String = name
        private set

    /** Minecraft resolves the skin in the background; the default face shows until then. */
    private var profile = ResolvableProfile.createUnresolved(uuid)

    fun setUuidAndName(uuid: UUID, name: String) {
        this.uuid = uuid
        this.name = name
        profile = ResolvableProfile.createUnresolved(uuid)
    }

    override fun canResize(): Boolean = true

    @Environment(EnvType.CLIENT)
    override fun paint(context: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        RenderHelper.renderPlayerFace(context, profile, x, y, min(width, height))
    }
}
