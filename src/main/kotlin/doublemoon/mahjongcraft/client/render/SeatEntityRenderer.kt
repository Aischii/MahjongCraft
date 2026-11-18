package doublemoon.mahjongcraft.client.render

import doublemoon.mahjongcraft.entity.SeatEntity
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceLocation

@Environment(EnvType.CLIENT)
class SeatEntityRenderer(
    context: EntityRendererProvider.Context
) : EntityRenderer<SeatEntity>(context) {

    override fun getTextureLocation(entity: SeatEntity): ResourceLocation? = null

}