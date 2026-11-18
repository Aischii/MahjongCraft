package doublemoon.mahjongcraft.client.gui.widget

import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTile
import io.github.cottonmc.cotton.gui.client.ScreenDrawing
import io.github.cottonmc.cotton.gui.widget.WWidget
import io.github.cottonmc.cotton.gui.widget.data.Color
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.GuiGraphics
import com.mojang.math.Axis

class WMahjongTile(
    var mahjongTile: MahjongTile,
    var direction: TileDirection = TileDirection.NORMAL,
) : WWidget() {

    override fun canResize(): Boolean = true

    override fun setSize(x: Int, y: Int) {
        if (direction == TileDirection.NORMAL) {
            super.setSize(x, y)
        } else {
            this.width = y
            this.height = x
        }
    }

    @Environment(EnvType.CLIENT)
    override fun paint(context: GuiGraphics, x: Int, y: Int, mouseX: Int, mouseY: Int) {
        if (direction == TileDirection.NORMAL) {
            ScreenDrawing.texturedRect(
                context,
                x,
                y,
                width,
                height,
                mahjongTile.surfaceIdentifier,
                Color.WHITE.toRgb()
            )
        } else {
            val matrices = context.pose()
            //這裡的 height 跟 width 都是反著用的, 因為我們把 matrices 轉成橫的才把直的印上去, 所以應該套用原本直的高跟寬
            matrices.pushPose()
            when (direction) {
                TileDirection.RIGHT -> {
                    matrices.translate((x + width).toDouble(), y.toDouble(), 0.0)
                    matrices.mulPose(Axis.ZP.rotationDegrees(90f))
                }
                TileDirection.LEFT -> {
                    matrices.translate(x.toDouble(), (y + height).toDouble(), 0.0)
                    matrices.mulPose(Axis.ZN.rotationDegrees(90f))
                }
                else -> return //這種情況不會發生
            }
            ScreenDrawing.texturedRect(
                context,
                0, //x, y 在前面 when 的時候使用 translate 偏移過了, 直接用 0
                0,
                height,
                width,
                mahjongTile.surfaceIdentifier,
                Color.WHITE.toRgb()
            )
            matrices.popPose()
        }
    }

    enum class TileDirection {
        LEFT, NORMAL, RIGHT
    }
}