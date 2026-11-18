package doublemoon.mahjongcraft.client.gui.widget

import io.github.cottonmc.cotton.gui.widget.TooltipBuilder
import io.github.cottonmc.cotton.gui.widget.WText
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.network.chat.Component


class WTooltipText(
    text: Component,
    var tooltip: Array<Component>
) : WText(text) {
    @Environment(EnvType.CLIENT)
    override fun addTooltip(tooltip: TooltipBuilder) {
        tooltip.add(*this.tooltip)
    }
}