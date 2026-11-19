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
    var tooltip: Array<Component>,
    /** Shown in gold above the [tooltip] lines, which are wrapped to fit. */
    var tooltipTitle: Component? = null,
) : WButton(icon, label) {
    @Environment(EnvType.CLIENT)
    override fun addTooltip(tooltip: TooltipBuilder) {
        TooltipLines.addTo(tooltip, tooltipTitle, this.tooltip)
    }
}