package doublemoon.mahjongcraft.client.gui.widget

import io.github.cottonmc.cotton.gui.widget.TooltipBuilder
import io.github.cottonmc.cotton.gui.widget.WButton
import io.github.cottonmc.cotton.gui.widget.icon.Icon
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.network.chat.Component


class WTooltipButton(
    icon: Icon? = null,
    label: Component? = null,
    var tooltip: Array<Component>
) : WButton(icon, label) {
    @Environment(EnvType.CLIENT)
    override fun addTooltip(tooltip: TooltipBuilder) {
        tooltip.add(*this.tooltip)
    }
}