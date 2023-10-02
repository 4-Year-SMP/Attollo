package dev.themeinerlp.attollo

import dev.themeinerlp.attollo.commands.DebugPaste
import dev.themeinerlp.attollo.listener.AttolloListener
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

open class Attollo : JavaPlugin() {


    lateinit var elevatorBlock: Material
    val minHeightOption: Boolean by lazy {
        config.getBoolean("minHeightTeleport")
    }
    val maxHeightOption: Boolean by lazy {
        config.getBoolean("maxHeightTeleport")
    }
    val allowBedrockTeleport: Boolean by lazy {
        config.getBoolean("allowBedrockTeleport")
    }
    override fun onLoad() {
        saveDefaultConfig()
    }

    override fun onEnable() {
        elevatorBlock = Material.matchMaterial(config.getString("elevatorBlock") ?: "DAYLIGHT_DETECTOR")
            ?: Material.DAYLIGHT_DETECTOR

        server.pluginManager.registerEvents(AttolloListener(this), this)
        val debugPasteCommand = DebugPaste(this)
        val command = getCommand("attollo")
        command?.setExecutor(debugPasteCommand)
        command?.tabCompleter = debugPasteCommand
    }
}