package doublemoon.mahjongcraft.game.mahjong.riichi.model

import doublemoon.mahjongcraft.MOD_ID
import doublemoon.mahjongcraft.util.TextFormatting
import net.minecraft.network.chat.Component
import org.mahjong4j.tile.Tile

enum class Wind(
    val tile: Tile
) : TextFormatting {
    EAST(Tile.TON),
    SOUTH(Tile.NAN),
    WEST(Tile.SHA),
    NORTH(Tile.PEI);

    override fun toText(): Component = Component.translatable("$MOD_ID.game.wind.${name.lowercase()}")
}