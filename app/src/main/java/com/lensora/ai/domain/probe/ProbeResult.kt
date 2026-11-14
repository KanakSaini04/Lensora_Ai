package com.lensora.ai.domain.probe

enum class ProbeStatus {
    PASS,
    FAIL,
    UNSUPPORTED,
    ERROR
}

data class ProbeResult(
    val probeId: String,
    val title: String,
    val status: ProbeStatus,
    val details: String,
    val executionTimeMs: Long = 0L,
)
