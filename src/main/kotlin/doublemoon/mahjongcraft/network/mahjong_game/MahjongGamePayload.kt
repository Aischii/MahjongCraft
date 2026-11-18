package doublemoon.mahjongcraft.network.mahjong_game

import doublemoon.mahjongcraft.game.mahjong.riichi.model.ClaimTarget
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongGameBehavior
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTile
import doublemoon.mahjongcraft.id
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class MahjongGamePayload(
    val behavior: MahjongGameBehavior,
    val hands: List<MahjongTile> = listOf(),
    val target: ClaimTarget = ClaimTarget.SELF,
    val extraData: String = "",
) : CustomPacketPayload {
    constructor(byteBuf: RegistryFriendlyByteBuf) : this(
        behavior = byteBuf.readEnum(MahjongGameBehavior::class.java),
        hands = Json.decodeFromString<MutableList<MahjongTile>>(byteBuf.readUtf(Short.MAX_VALUE.toInt())),
        target = byteBuf.readEnum(ClaimTarget::class.java),
        extraData = byteBuf.readUtf(Short.MAX_VALUE.toInt())
    )

    fun writeByteBuf(byteBuf: RegistryFriendlyByteBuf) {
        with(byteBuf) {
            writeEnum(behavior)
            writeUtf(Json.encodeToString(hands), Short.MAX_VALUE.toInt())
            writeEnum(target)
            writeUtf(extraData, Short.MAX_VALUE.toInt())
        }
    }

    override fun type(): CustomPacketPayload.Type<MahjongGamePayload> = ID

    companion object {
        val ID = CustomPacketPayload.Type<MahjongGamePayload>(id("mahjong_game_payload"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, MahjongGamePayload> =
            StreamCodec.ofMember(MahjongGamePayload::writeByteBuf, ::MahjongGamePayload)
    }
}