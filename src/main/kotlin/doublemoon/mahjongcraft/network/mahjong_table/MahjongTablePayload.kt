package doublemoon.mahjongcraft.network.mahjong_table

import doublemoon.mahjongcraft.game.mahjong.riichi.model.MahjongTableBehavior
import doublemoon.mahjongcraft.id
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.core.BlockPos

data class MahjongTablePayload(
    val behavior: MahjongTableBehavior,
    val pos: BlockPos,
    val extraData: String = "",
) : CustomPacketPayload {
    constructor(byteBuf: FriendlyByteBuf) : this(
        behavior = byteBuf.readEnum(MahjongTableBehavior::class.java),
        pos = byteBuf.readBlockPos(),
        extraData = byteBuf.readUtf(Short.MAX_VALUE.toInt())
    )

    fun writeByteBuf(byteBuf: RegistryFriendlyByteBuf) {
        with(byteBuf) {
            writeEnum(behavior)
            writeBlockPos(pos)
            writeUtf(extraData, Short.MAX_VALUE.toInt())
        }
    }

    override fun type(): CustomPacketPayload.Type<MahjongTablePayload> = ID

    companion object {
        val ID = CustomPacketPayload.Type<MahjongTablePayload>(id("mahjong_table_payload"))
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, MahjongTablePayload> =
            StreamCodec.ofMember(MahjongTablePayload::writeByteBuf, ::MahjongTablePayload)
    }
}