package dev.limeroot.research

enum class CheckSeverity {
    GOOD, INFO, WARNING, CRITICAL
}

data class DiagnosticCheck(
    val category: String,
    val title: String,
    val value: String,
    val detail: String,
    val severity: CheckSeverity
)

data class DeviceDetails(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val product: String,
    val board: String,
    val hardware: String,
    val android: String,
    val sdk: Int,
    val build: String,
    val buildType: String,
    val buildTags: String,
    val securityPatch: String,
    val kernel: String,
    val architecture: String,
    val abis: String,
    val fingerprint: String,
    val bootloaderState: String,
    val verifiedBootState: String,
    val vbmetaState: String,
    val verityMode: String,
    val selinuxState: String,
    val selinuxContext: String,
    val capEff: String,
    val noNewPrivs: String,
    val seccomp: String,
    val appDebuggable: Boolean,
    val runtimeMaxMemoryMb: Long,
    val rootPathsFound: List<String>,
    val kernelConfig: Map<String, String> = emptyMap(),
    val userNamespacesMax: String = "Не удалось прочитать"
)

data class DiagnosticSnapshot(
    val generatedAt: String,
    val device: DeviceDetails,
    val checks: List<DiagnosticCheck>,
    val notes: List<String>
)
