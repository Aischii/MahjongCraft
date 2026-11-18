package doublemoon.mahjongcraft.client.render

import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.item.ItemModelResolver
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f

/** What one of the mod's item-shaped entities needs to be drawn: its item model and placement. */
@Environment(EnvType.CLIENT)
class ItemModelRenderState : EntityRenderState() {
    val item = ItemStackRenderState()
    val transform = Matrix4f()
}

/**
 * Draws an entity as an item model (tiles, dice, scoring sticks, bots).
 *
 * Minecraft 26.x renders entities in two steps: [extractRenderState] copies what is needed from the
 * entity on the game thread, and [submit] draws from that copy. Subclasses describe the item and
 * how to place it; the placement is baked into a matrix here so [submit] doesn't touch the entity.
 */
@Environment(EnvType.CLIENT)
abstract class ItemModelEntityRenderer<T : Entity>(
    context: EntityRendererProvider.Context,
) : EntityRenderer<T, ItemModelRenderState>(context) {

    private val itemModelResolver: ItemModelResolver = context.itemModelResolver

    /** The item model to draw for [entity]. */
    protected abstract fun itemFor(entity: T): ItemStack

    protected open val displayContext: ItemDisplayContext = ItemDisplayContext.GROUND

    /** Rotates, scales and offsets [pose] (starting at the entity's position) for [entity]. */
    protected abstract fun place(entity: T, pose: PoseStack)

    override fun shouldRender(
        entity: T,
        culler: Frustum,
        camX: Double,
        camY: Double,
        camZ: Double,
        partialTicks: Float,
    ): Boolean = !entity.isInvisible && super.shouldRender(entity, culler, camX, camY, camZ, partialTicks)

    override fun createRenderState(): ItemModelRenderState = ItemModelRenderState()

    override fun extractRenderState(entity: T, state: ItemModelRenderState, partialTicks: Float) {
        super.extractRenderState(entity, state, partialTicks)
        itemModelResolver.updateForNonLiving(state.item, itemFor(entity), displayContext, entity)
        val pose = PoseStack()
        place(entity, pose)
        state.transform.set(pose.last().pose())
    }

    override fun submit(
        state: ItemModelRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState,
    ) {
        poseStack.pushPose()
        poseStack.mulPose(state.transform)
        state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor)
        poseStack.popPose()
        super.submit(state, poseStack, submitNodeCollector, camera)
    }
}
