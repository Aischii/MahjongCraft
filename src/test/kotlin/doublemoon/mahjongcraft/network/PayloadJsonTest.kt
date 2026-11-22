package doublemoon.mahjongcraft.network

import doublemoon.mahjongcraft.game.mahjong.riichi.model.DiscardRequest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PayloadJsonTest {
    @Test
    fun ignoresUnknownDiscardRequestFields() {
        val request = PayloadJson.decodeFromString<DiscardRequest>(
            """{"skippable":false,"auto":true,"futureField":42}"""
        )
        assertTrue(request.auto)
    }

    @Test
    fun oldDiscardRequestsDefaultToManualDiscard() {
        val request = PayloadJson.decodeFromString<DiscardRequest>(
            """{"skippable":true,"cannotDiscard":[]}"""
        )
        assertTrue(request.skippable)
        assertFalse(request.auto)
    }
}
