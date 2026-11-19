package doublemoon.mahjongcraft.client.gui.widget

import io.github.cottonmc.cotton.gui.widget.TooltipBuilder
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence

/**
 * Tooltips laid out like vanilla item tooltips: an optional gold title, then the body
 * wrapped to [MAX_WIDTH] so a long description never runs off the screen.
 */
@Environment(EnvType.CLIENT)
object TooltipLines {
    const val MAX_WIDTH = 200

    fun build(title: Component?, body: Array<out Component>): List<FormattedCharSequence> {
        val font = Minecraft.getInstance().font
        return buildList {
            if (title != null) add(title.copy().withStyle(ChatFormatting.GOLD).visualOrderText)
            body.forEach { line ->
                val styled = if (line.style.color == null) line.copy().withStyle(ChatFormatting.GRAY) else line
                addAll(font.split(styled, MAX_WIDTH))
            }
        }
    }

    fun addTo(builder: TooltipBuilder, title: Component?, body: Array<out Component>) {
        if (title == null && body.isEmpty()) return
        builder.add(*build(title, body).toTypedArray())
    }
}
