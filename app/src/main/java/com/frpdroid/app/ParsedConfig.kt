package com.frpdroid.app

/** 解析 frpc.toml 后得到的配置摘要（用于导入） */
data class ParsedConfig(
    val addr: String,
    val port: String,
    val tok: String,
    val user: String,
    val metadatas: String,
    val proxies: List<ProxyConfig>,
    val visitors: List<ProxyConfig>,
)
