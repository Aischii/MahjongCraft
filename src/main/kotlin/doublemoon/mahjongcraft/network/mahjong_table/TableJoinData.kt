package doublemoon.mahjongcraft.network.mahjong_table

import doublemoon.mahjongcraft.MOD_ID
import kotlinx.serialization.Serializable
import net.fabricmc.loader.api.FabricLoader

@Serializable
internal data class TableJoinData(val version: String? = null)

internal val mahjongCraftVersion: String by lazy {
    FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().metadata.version.friendlyString
}
