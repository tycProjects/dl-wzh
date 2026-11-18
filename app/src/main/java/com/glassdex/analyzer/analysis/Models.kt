package com.glassdex.analyzer.analysis

enum class FileKind { APK, XAPK, EXE, UNKNOWN }

enum class Risk(val label: String) {
    CLEAN("Looks clean"),
    LOW("Low risk"),
    MEDIUM("Suspicious"),
    HIGH("High risk")
}

data class Finding(
    val title: String,
    val detail: String,
    val points: Int // contribution to risk score
)

data class Hashes(val md5: String, val sha1: String, val sha256: String)

data class CertInfo(
    val subject: String,
    val issuer: String,
    val validFrom: String,
    val validTo: String,
    val sha256: String,
    val selfSigned: Boolean,
    val expired: Boolean
)

data class PermissionInfo(val name: String, val dangerous: Boolean, val note: String)

data class AppInfo(
    val label: String?,
    val packageName: String?,
    val versionName: String?,
    val versionCode: String?,
    val minSdk: String?,
    val targetSdk: String?,
    val permissions: List<PermissionInfo>,
    val activities: List<String>,
    val services: List<String>,
    val receivers: List<String>,
    val providers: List<String>,
    val debuggable: Boolean,
    val allowBackup: Boolean,
    val cleartextTraffic: Boolean,
    val certs: List<CertInfo>,
    val nativeLibs: List<String>,
    val abis: List<String>,
    val dexCount: Int,
    val classCount: Int,
    val methodCount: Int,
    val stringCount: Int,
    val resourceFileCount: Int,
    val iconPath: String?
)

data class PeInfo(
    val is64: Boolean,
    val machine: String,
    val subsystem: String,
    val isDll: Boolean,
    val timestamp: String,
    val entryPoint: String,
    val sections: List<PeSection>,
    val imports: Map<String, List<String>>,
    val suspiciousImports: List<String>,
    val packer: String?,
    val signed: Boolean,
    val dotNet: Boolean
)

data class PeSection(
    val name: String,
    val virtualSize: Long,
    val rawSize: Long,
    val entropy: Double,
    val executable: Boolean,
    val writable: Boolean
)

data class VtResult(
    val state: String, // "not-configured", "unknown", "found", "error"
    val malicious: Int = 0,
    val suspicious: Int = 0,
    val harmless: Int = 0,
    val undetected: Int = 0,
    val permalink: String? = null,
    val message: String? = null
)

data class AnalysisReport(
    val fileName: String,
    val kind: FileKind,
    val sizeBytes: Long,
    val hashes: Hashes,
    val appInfo: AppInfo?,
    val peInfo: PeInfo?,
    val findings: List<Finding>,
    val riskScore: Int,
    val risk: Risk,
    val vt: VtResult,
    val outputDir: String?,
    val fileCount: Int,
    val tookMs: Long,
    val subApks: List<String> = emptyList(),
    val urls: List<String> = emptyList()
)

data class Progress(val stage: String, val fraction: Float)
