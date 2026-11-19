package doublemoon.mahjongcraft.network.mahjong_stats

import doublemoon.mahjongcraft.id
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

/**
 * Client → server: an empty [json] asks for the sender's stats.
 * Server → client: [json] is the sender's `PlayerStats`.
 */
data class MahjongStatsPayload(val json: String = "") : CustomPacketPayload {
    constructor(byteBuf: FriendlyByteBuf) : this(byteBuf.readUtf())

    fun writeByteBuf(byteBuf: FriendlyByteBuf) {
        byteBuf.writeUtf(json)
    }

    override fun type(): CustomPacketPayload.Type<MahjongStatsPayload> = ID

    companion object {
        val ID = CustomPacketPayload.Type<MahjongStatsPayload>(id("mahjong_stats_payload"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, MahjongStatsPayload> =
            StreamCodec.ofMember(MahjongStatsPayload::writeByteBuf, ::MahjongStatsPayload)
    }
}
