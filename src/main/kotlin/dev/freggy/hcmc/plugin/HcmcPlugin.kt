package dev.freggy.hcmc.plugin

import dev.freggy.hcmc.hcloud.HetznerCloud
import dev.freggy.hcmc.hcloud.model.Action
import dev.freggy.hcmc.hcloud.model.ActionCommand
import dev.freggy.hcmc.hcloud.model.ActionStatus
import dev.freggy.hcmc.hcloud.model.Server
import dev.freggy.hcmc.plugin.command.SetApiKeyCommand
import dev.freggy.hcmc.plugin.command.StartCommand
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap


class HcmcPlugin : JavaPlugin() {
    // pool of current servers mapped id -> server
    private val serverPool = ConcurrentHashMap<Int, PlacedServer>()
    private val serverStatusEvents = Channel<Action>()
    private val serverPresenceEvents = Channel<Action>()

    private var hcloud = HetznerCloud("")

    private val monitor = ActionMonitor(hcloud)
    private val fetcher = ActionFetcher(hcloud, monitor)

    override fun onEnable() {
        // only for test purposes
        hcloud.servers.getAllServers().let {
            placeServers(it).forEach { placed ->
                serverPool[placed.inner.id] = placed
            }
        }

        monitor.startMonitoring()
        fetcher.startFetching()

        getCommand("apikey")?.setExecutor(SetApiKeyCommand(this))
        getCommand("start")?.setExecutor(StartCommand(this))

        GlobalScope.launch {
            while (true) {
                val action = monitor.updateChannel.receive()
                Bukkit.getLogger().info(action.toString())
                if (action.command.isServerPresence) {
                    serverPresenceEvents.send(action)
                }
                if (action.command.isServerStatus) {
                    serverStatusEvents.send(action)
                }
            }
        }

        GlobalScope.launch(PluginContext(this)) {
            while (true) {
                val event = serverStatusEvents.receive()
                when (event.command) {
                    ActionCommand.STOP_SERVER -> {
                        event.resources.forEach {
                            // entry can be absent because STOP_SERVER is triggered after DELETE_SERVER
                            serverPool[it.id]?.let { placed ->
                                Bukkit.broadcastMessage("STOPPED server ${it.id} at ${placed.location}")
                                val ctx = coroutineContext[PluginContext]!!
                                Bukkit.getScheduler().callSyncMethod(ctx.plugin) {
                                    placed.stop(event.status)
                                }
                                placed.updateInner(hcloud)
                            }
                        }
                    }
                }
            }
        }

        GlobalScope.launch(PluginContext(this)) {
            while (true) {
                val event = serverPresenceEvents.receive()
                when (event.command) {
                    ActionCommand.DELETE_SERVER -> {
                        event.resources.forEach {
                            serverPool[it.id]?.let { server ->
                                Bukkit.broadcastMessage("REMOVED server ${it.id} at ${server.location}")
                                val ctx = coroutineContext[PluginContext]!!
                                Bukkit.getScheduler().callSyncMethod(ctx.plugin) {
                                    server.remove(event.status)
                                }
                                serverPool.remove(it.id)
                            }
                        }
                    }
                    ActionCommand.CREATE_SERVER -> {
                    }
                }
            }
        }
    }

    override fun onDisable() {
        this.serverStatusEvents.close()
        this.serverPresenceEvents.close()
    }

    override fun getDefaultWorldGenerator(worldName: String, id: String?): ChunkGenerator? {
        return GirdChunkGenerator()
    }

    fun setApiKey(key: String) {
        this.hcloud = HetznerCloud(key)
    }

    fun start() {
        // TODO: fetch current state and place it
        monitor.startMonitoring()
        fetcher.startFetching()
    }

    fun placeServers(servers: List<Server>): MutableList<PlacedServer> {
        val world = Bukkit.getWorld("world")
        val placed = mutableListOf<PlacedServer>()
        var x = 0
        var z = 0

        servers.forEach {
            val chunk = world!!.getChunkAt(x, z)
            // draw chunk borders
            for (i in 0..15) {
                chunk.getBlock(i, 3, 0).type = Material.BEDROCK
                chunk.getBlock(15, 3, i).type = Material.BEDROCK
                chunk.getBlock(0, 3, i).type = Material.BEDROCK
                chunk.getBlock(i, 3, 15).type = Material.BEDROCK
            }
            z++
            x++
            val place = PlacedServer(it, chunk.getBlock(8, 5, 8).location)
            place.start(ActionStatus.RUNNING)
            placed.add(place)
        }
        return placed
    }
}
