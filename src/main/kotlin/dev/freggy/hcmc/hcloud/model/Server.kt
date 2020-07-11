package dev.freggy.hcmc.hcloud.model

import com.google.gson.annotations.SerializedName

data class Server(
    val id: Int,
    val name: String,
    val status: Status,
    val created: String
)

data class ServerPage(val servers: Array<Server>, val meta: Meta)

data class Meta(val pagination: Pagination)

data class Pagination(
    val page: Int,
    @SerializedName("last_page") val lastPage: Int,
    @SerializedName("next_page") val nextPage: Int
)

enum class Status {
    @SerializedName("running")
    RUNNING,

    @SerializedName("initializing")
    INITIALIZING,

    @SerializedName("starting")
    STARTING,

    @SerializedName("stopping")
    STOPPING,

    @SerializedName("off")
    OFF,

    @SerializedName("deleting")
    DELETING,

    @SerializedName("migrating")
    MIGRATING,

    @SerializedName("rebuilding")
    REBUILDING,

    @SerializedName("unknown")
    UNKNOWN
}
