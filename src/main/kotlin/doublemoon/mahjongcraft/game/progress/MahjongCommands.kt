package doublemoon.mahjongcraft.game.progress

import com.mojang.brigadier.arguments.StringArgumentType
import doublemoon.mahjongcraft.MOD_ID
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.network.chat.Component

/** `/mahjong stats [player]`: anyone can read anyone's record in this world. */
object MahjongCommands {
    fun register() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                Commands.literal("mahjong").then(
                    Commands.literal("stats")
                        .executes { show(it.source, null) }
                        .then(
                            Commands.argument("player", StringArgumentType.word())
                                .suggests { ctx, builder ->
                                    val names = MahjongStatsData.of(ctx.source.server).book.players.values.map { it.name } +
                                        ctx.source.onlinePlayerNames
                                    SharedSuggestionProvider.suggest(names.distinct(), builder)
                                }
                                .executes { show(it.source, StringArgumentType.getString(it, "player")) }
                        )
                )
            )
        }
    }

    private fun show(source: CommandSourceStack, name: String?): Int {
        val book = MahjongStatsData.of(source.server).book
        val stats = if (name == null) {
            val player = source.playerOrException
            book.players[player.stringUUID] ?: PlayerStats(name = player.name.string)
        } else {
            book.findByName(name) ?: run {
                source.sendFailure(Component.translatable("$MOD_ID.stats.unknown_player", name))
                return 0
            }
        }
        source.sendSystemMessage(
            Component.translatable("$MOD_ID.stats.title", stats.name).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
        )
        StatsText.lines(stats).forEach { source.sendSystemMessage(it) }
        return 1
    }
}
