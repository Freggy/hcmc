package dev.freggy.hcmc

import dev.freggy.hcmc.hcloud.HetznerCloud
import kotlinx.coroutines.runBlocking

// for testing purposes only

fun main(args: Array<String>) {
    val h = HetznerCloud("")


    //println(h.actions.getAction())

    val monitor = ActionMonitor(h)
    monitor.start()

    val fetcher = ActionFetcher(h, monitor)
    fetcher.start()

    while (true) {
        runBlocking {
            val event = monitor.updateChannel.receive()
            println(event)
        }
    }
}