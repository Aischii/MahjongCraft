package doublemoon.mahjongcraft.game.progress

import doublemoon.mahjongcraft.id
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.gamerules.GameRule
import net.minecraft.world.level.gamerules.GameRuleCategory

object MahjongGameRules {
    /** `/gamerule mahjongcraft:require_tile_set false` lets any table start without a loaded Tile Set. */
    val requireTileSet: GameRule<Boolean> = GameRuleBuilder.forBoolean(true)
        .category(GameRuleCategory.MISC)
        .buildAndRegister(id("require_tile_set"))

    fun register() {
        // Touching the object registers the rules above.
    }

    fun tileSetRequired(level: ServerLevel): Boolean = level.gameRules.get(requireTileSet)
}
