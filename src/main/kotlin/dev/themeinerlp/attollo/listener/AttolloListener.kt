package dev.themeinerlp.attollo.listener

import dev.themeinerlp.attollo.Attollo
import dev.themeinerlp.attollo.USE_PERMISSION
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerToggleSneakEvent

class AttolloListener(private val attollo: Attollo) : Listener {

    @EventHandler
    fun onUp(event: PlayerMoveEvent) {
        val player = event.player
        if (event.to.y <= event.from.y) return
        if (event.to.y - event.from.y <= 0.125) return
        handleElevator(player, true)
    }

    @EventHandler
    fun onDown(event: PlayerToggleSneakEvent) {
        val player = event.player
        if (!player.isSneaking) return
        handleElevator(player)
    }

    private fun handleElevator(player: Player, up: Boolean = false) {
        if (!player.hasPermission(USE_PERMISSION)) return

        val location = player.location
        val block = location.block

        if (block.type != attollo.elevatorBlock) return

        val world = block.world
        val height = world.maxHeight
        val depth = world.minHeight
        val blockLocation = block.location
        val found = if (up) {
            ((blockLocation.blockY + 1)..height).map {
                world.getBlockAt(blockLocation.blockX, it, blockLocation.blockZ)
            }
                .firstOrNull() { it.type == attollo.elevatorBlock }?.location ?: return
        } else {
            ((blockLocation.blockY - 1) downTo depth).map {
                world.getBlockAt(blockLocation.blockX, it, blockLocation.blockZ)
            }.firstOrNull() { it.type == attollo.elevatorBlock }?.location ?: return
        }
        found.yaw = location.yaw
        found.pitch = location.pitch

        /**
         * The modifiedLocation is needed for teleporting the player to the right spot on top of the elevator block
         */
        val modifiedLocation = found.clone().add(0.5, 1.0, 0.5)

        /**
         * isMaxHeight, isMinHeight and isAllowBedrockTeleport are config values the user can set to limit elevator usage to a certain height or block limit
         * This can be useful for the user to prevent players to teleport on top of the nether roof or other places
         */
        val isMaxHeight = !this.attollo.maxHeightOption
        val isMinHeight = !this.attollo.minHeightOption
        val isAllowBedrockTeleport = !this.attollo.allowBedrockTeleport

        /**
         * Prevent players to get stuck or to be trapped inside a block that is above the teleporter
         */
        if (modifiedLocation.block.type != Material.AIR) return
        if (modifiedLocation.blockY == height && isMaxHeight) {
            player.sendMessage(Component.text("You can't teleport to max build height!"))
            return
        }
        if (modifiedLocation.blockY == depth && isMinHeight) {
            player.sendMessage(Component.text("You can't teleport to min build height!"))
            return
        }
        val blockBelow = block.world.getBlockAt(blockLocation.clone().add(0.0,-1.0,0.0))
        if (blockBelow.type == Material.BEDROCK && isAllowBedrockTeleport) {
            player.sendMessage(Component.text("You can't teleport to the top of a bedrock block!"))
            return
        }
        player.teleportAsync(modifiedLocation)
    }
}
