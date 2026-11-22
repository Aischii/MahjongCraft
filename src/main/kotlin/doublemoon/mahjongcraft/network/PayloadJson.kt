package doublemoon.mahjongcraft.network

import kotlinx.serialization.json.Json

val PayloadJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
