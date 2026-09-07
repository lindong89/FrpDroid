package com.frpdroid.app

/** 流量统计信息（会话/今日/实时速度） */
data class TrafficInfo(
    val sessionRx: Long = 0L,
    val sessionTx: Long = 0L,
    val todayRx: Long = 0L,
    val todayTx: Long = 0L,
    val speedRx: Long = 0L,
    val speedTx: Long = 0L,
)
