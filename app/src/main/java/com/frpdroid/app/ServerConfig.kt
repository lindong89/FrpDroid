package com.frpdroid.app

import org.json.JSONArray
import org.json.JSONObject

/** 服务器配置（一个 frp 服务器 + 其代理/访问者列表） */
data class ServerConfig(
    val id: Int = 0,
    val name: String = "",
    val addr: String = "",
    val port: String = "7000",
    val tok: String = "",
    val user: String = "",
    val metadatas: String = "",
    val proxies: List<ProxyConfig> = emptyList(),
    val visitors: List<ProxyConfig> = emptyList(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("addr", addr)
        put("port", port)
        put("tok", tok)
        put("user", user)
        put("metadatas", metadatas)
        put("proxies", listToJson(proxies))
        put("visitors", listToJson(visitors))
    }

    companion object {
        fun fromJson(o: JSONObject): ServerConfig = ServerConfig(
            id = o.optInt("id", 0),
            name = o.optString("name", ""),
            addr = o.optString("addr", ""),
            port = o.optString("port", "7000"),
            tok = o.optString("tok", ""),
            user = o.optString("user", ""),
            metadatas = o.optString("metadatas", ""),
            proxies = parseList(o.optString("proxies", "[]")),
            visitors = parseList(o.optString("visitors", "[]")),
        )

        private fun parseList(json: String): List<ProxyConfig> = try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { ProxyConfig.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
