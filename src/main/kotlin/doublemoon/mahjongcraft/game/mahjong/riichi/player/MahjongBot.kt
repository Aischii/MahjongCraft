package doublemoon.mahjongcraft.game.mahjong.riichi.player

import doublemoon.mahjongcraft.entity.MahjongBotEntity
import doublemoon.mahjongcraft.game.mahjong.riichi.MahjongGame
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTile
import net.minecraft.server.level.ServerLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

/**
 * 麻將機器人,
 * 使用 [MahjongGame.addBot] 的時候就會生成 [entity] 了 (並不會遊戲開始才生成)
 * TODO 待玩家功能完成後再回來補全電腦的 AI
 * @param world 預設生成的世界
 * @param pos 預設生成的位置
 * @param gamePos 生成這個機器人的遊戲的方塊座標,
 * */
class MahjongBot(
    val world: ServerLevel,
    pos: Vec3,
    gamePos: BlockPos,
) : MahjongPlayerBase() {

    override val entity: MahjongBotEntity = MahjongBotEntity(world = world).apply {
        code = MahjongTile.random().code //隨機決定外觀
        isSpawnedByGame = true
        gameBlockPos = gamePos
        isInvisible = true //先隱形, 等開始遊戲才解除隱形
        moveTo(pos)  //再傳送到預設位置上
        world.addFreshEntity(this) //最後才在世界上生成
    }

    override var ready: Boolean = true

    override fun teleport(targetWorld: ServerLevel, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        with(entity) {
            if (this.level() != targetWorld) changeDimension(targetWorld)
            this.yRot = yaw
            this.xRot = pitch
            teleportTo(x, y, z)
        }
    }
}