package doublemoon.mahjongcraft.client.render

import doublemoon.mahjongcraft.entity.SeatEntity
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.EntityRenderState

/** Seats are invisible; this renderer draws nothing. */
@Environment(EnvType.CLIENT)
class SeatEntityRenderer(
    context: EntityRendererProvider.Context,
) : EntityRenderer<SeatEntity, EntityRenderState>(context) {

    override fun createRenderState(): EntityRenderState = EntityRenderState()
}
