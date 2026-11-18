package doublemoon.mahjongcraft.network.mahjong_tile_code

import doublemoon.mahjongcraft.id
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload

data class MahjongTileCodePayload(
    val id: Int,
    val code: Int = 0,
) : CustomPacketPayload {
    constructor(byteBuf: FriendlyByteBuf) : this(
        id = byteBuf.readVarInt(),
        code = byteBuf.readVarInt()
    )

    fun writeByteBuf(byteBuf: FriendlyByteBuf) {
        with(byteBuf) {
            writeVarInt(id)
            writeVarInt(code)
        }
    }

    override fun type(): CustomPacketPayload.Type<MahjongTileCodePayload> = ID

    companion object {
        val ID = CustomPacketPayload.Type<MahjongTileCodePayload>(id("mahjong_tile_code_payload"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, MahjongTileCodePayload> =
            StreamCodec.ofMember(MahjongTileCodePayload::writeByteBuf, ::MahjongTileCodePayload)
    }
}