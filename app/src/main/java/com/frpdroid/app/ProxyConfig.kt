package com.frpdroid.app

import org.json.JSONArray
import org.json.JSONObject

/** 单个代理/访问者配置（对应 frp.toml 的一个 [[proxies]] / [[visitors]] 条目） */
data class ProxyConfig(
    var id: Int = 0,
    var name: String = "",
    var type: String = "tcp",
    var localIP: String = "127.0.0.1",
    var localPort: String = "",
    var remotePort: String = "",
    var customDomains: String = "",
    var multiplexer: String = "httpconnect",
    var secretKey: String = "",
    var pluginType: String = "",
    var unixPath: String = "",
    var localPath: String = "",
    var stripPrefix: String = "",
    var httpUser: String = "",
    var httpPassword: String = "",
    var localAddr: String = "",
    var crtPath: String = "",
    var keyPath: String = "",
    var hostHeaderRewrite: String = "",
    var requestHeaders: String = "",
    var isVisitor: Boolean = false,
    var serverName: String = "",
    var bindAddr: String = "127.0.0.1",
    var bindPort: String = "",
    var fallbackTo: String = "",
    var fallbackTimeoutMs: String = "",
    var keepTunnelOpen: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("type", type)
        put("localIP", localIP)
        put("localPort", localPort)
        put("remotePort", remotePort)
        put("customDomains", customDomains)
        put("multiplexer", multiplexer)
        put("secretKey", secretKey)
        put("pluginType", pluginType)
        put("unixPath", unixPath)
        put("localPath", localPath)
        put("stripPrefix", stripPrefix)
        put("httpUser", httpUser)
        put("httpPassword", httpPassword)
        put("localAddr", localAddr)
        put("crtPath", crtPath)
        put("keyPath", keyPath)
        put("hostHeaderRewrite", hostHeaderRewrite)
        put("requestHeaders", requestHeaders)
        put("isVisitor", isVisitor)
        put("serverName", serverName)
        put("bindAddr", bindAddr)
        put("bindPort", bindPort)
        put("fallbackTo", fallbackTo)
        put("fallbackTimeoutMs", fallbackTimeoutMs)
        put("keepTunnelOpen", keepTunnelOpen)
    }

    companion object {
        fun fromJson(o: JSONObject): ProxyConfig = ProxyConfig(
            id = o.optInt("id", 0),
            name = o.optString("name", ""),
            type = o.optString("type", "tcp"),
            localIP = o.optString("localIP", "127.0.0.1"),
            localPort = o.optString("localPort", ""),
            remotePort = o.optString("remotePort", ""),
            customDomains = o.optString("customDomains", ""),
            multiplexer = o.optString("multiplexer", "httpconnect"),
            secretKey = o.optString("secretKey", ""),
            pluginType = o.optString("pluginType", ""),
            unixPath = o.optString("unixPath", ""),
            localPath = o.optString("localPath", ""),
            stripPrefix = o.optString("stripPrefix", ""),
            httpUser = o.optString("httpUser", ""),
            httpPassword = o.optString("httpPassword", ""),
            localAddr = o.optString("localAddr", ""),
            crtPath = o.optString("crtPath", ""),
            keyPath = o.optString("keyPath", ""),
            hostHeaderRewrite = o.optString("hostHeaderRewrite", ""),
            requestHeaders = o.optString("requestHeaders", ""),
            isVisitor = o.optBoolean("isVisitor", false),
            serverName = o.optString("serverName", ""),
            bindAddr = o.optString("bindAddr", "127.0.0.1"),
            bindPort = o.optString("bindPort", ""),
            fallbackTo = o.optString("fallbackTo", ""),
            fallbackTimeoutMs = o.optString("fallbackTimeoutMs", ""),
            keepTunnelOpen = o.optBoolean("keepTunnelOpen", false),
        )
    }
}
