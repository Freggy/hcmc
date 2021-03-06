package dev.freggy.hcmc.hcloud

import com.google.gson.Gson
import java.net.http.HttpClient
import java.net.http.HttpRequest

class HetznerCloud(token: String) {
    val client = HttpClient.newHttpClient()
    val gson = Gson()
    val basePath = "https://api.hetzner.cloud/v1"
    val baseRequest = HttpRequest.newBuilder().header("Authorization", "bearer $token")

    val actions = ActionClient(this)
    val servers = ServerClient(this)
}
