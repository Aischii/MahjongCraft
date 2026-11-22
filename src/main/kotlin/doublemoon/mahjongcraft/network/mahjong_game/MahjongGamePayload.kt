package doublemoon.mahjongcraft.network.mahjong_game

import doublemoon.mahjongcraft.game.mahjong.riichi.model.ClaimTarget
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongGameBehavior
import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTile
import doublemoon.mahjongcraft.id
import kotlinx.serialization.encodeToString
import doublemoon.mahjongcraft.network.PayloadJson
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class MahjongGamePayload(
    val behavior: MahjongGameBehavior,
    val hands: List<MahjongTile> = listOf(),
    val target: ClaimTarget = ClaimTarget.SELF,
    val extraData: String = "",
    private val encodedHands: String? = null,
) : CustomPacketPayload {
    constructor(byteBuf: RegistryFriendlyByteBuf) : this(
        behavior = byteBuf.readEnum(MahjongGameBehavior::class.java),
        encodedHands = byteBuf.readUtf(Short.MAX_VALUE.toInt()),
        target = byteBuf.readEnum(ClaimTarget::class.java),
        extraData = byteBuf.readUtf(Short.MAX_VALUE.toInt())
    )

    // Decode inside the guarded listener so malformed JSON cannot abort packet decoding.
    fun decodeHands(): List<MahjongTile> = encodedHands?.let { PayloadJson.decodeFromString(it) } ?: hands

    fun writeByteBuf(byteBuf: RegistryFriendlyByteBuf) {
        with(byteBuf) {
            writeEnum(behavior)
            writeUtf(encodedHands ?: PayloadJson.encodeToString(hands), Short.MAX_VALUE.toInt())
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