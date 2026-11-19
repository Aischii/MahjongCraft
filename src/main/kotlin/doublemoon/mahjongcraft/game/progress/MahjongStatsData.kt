package doublemoon.mahjongcraft.game.progress

import com.mojang.serialization.Codec
import doublemoon.mahjongcraft.id
import kotlinx.serialization.json.Json
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.saveddata.SavedData
import net.minecraft.world.level.saveddata.SavedDataType
import net.minecraft.util.datafix.DataFixTypes

/** Saves the world's [StatsBook] with the world (data/mahjongcraft/stats.dat), as JSON inside NBT. */
class MahjongStatsData(val book: StatsBook = StatsBook()) : SavedData() {

    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        private val CODEC: Codec<MahjongStatsData> = Codec.STRING.fieldOf("json").codec().xmap(
            { MahjongStatsData(runCatching { json.decodeFromString<StatsBook>(it) }.getOrElse { StatsBook() }) },
            { json.encodeToString(StatsBook.serializer(), it.book) },
        )

        private val TYPE = SavedDataType(id("stats"), ::MahjongStatsData, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE)

        fun of(server: MinecraftServer): MahjongStatsData = server.dataStorage.computeIfAbsent(TYPE)

        /** Changes [server]'s stats with [block] and marks them for saving. */
        fun update(server: MinecraftServer, block: StatsBook.() -> Unit) {
            val data = of(server)
            data.book.block()
            data.setDirty()
        }
    }
}
