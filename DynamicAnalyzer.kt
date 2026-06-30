package com.automod.ai.analyzer

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import com.automod.ai.r2frida.R2FridaBridge
import com.automod.ai.r2ghidra.R2GhidraBridge

class DynamicAnalyzer(
    private val r2frida: R2FridaBridge,
    private val r2ghidra: R2GhidraBridge,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val _analysisState = MutableStateFlow(AnalysisState.IDLE)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    private val _findings = MutableSharedFlow<Finding>(replay = 100)
    val findings: SharedFlow<Finding> = _findings.asSharedFlow()

    enum class AnalysisState { IDLE, SCANNING, ANALYZING, HOOKING, BYPASSING, COMPLETED, ERROR }

    @Serializable
    data class Finding(
        val type: FindingType,
        val severity: Severity,
        val description: String,
        val details: String,
        val recommendation: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    @Serializable
    enum class FindingType {
        LICENSE_CHECK, SERVER_VALIDATION, SSL_PINNING, ROOT_DETECTION,
        EMULATOR_DETECTION, ANTI_TAMPER, ANTI_DEBUG, OBFUSCATION,
        PACKER, NATIVE_CHECK, STRING_ENCRYPTION, ASSET_ENCRYPTION
    }

    @Serializable
    enum class Severity { INFO, LOW, MEDIUM, HIGH, CRITICAL }

    suspend fun analyzePackage(packageName: String): AnalysisResult = scope.async {
        _analysisState.value = AnalysisState.SCANNING
        
        try {
            // 1. Spawn procesu
            val session = r2frida.spawnPackage(packageName)
            _analysisState.value = AnalysisState.ANALYZING
            
            // 2. Bypass detekcí
            r2frida.bypassRootDetection(session.id)
            r2frida.bypassEmulatorDetection(session.id)
            
            // 3. Vyhledání licenčních validací
            val licenseFindings = r2frida.findLicenseValidation(session.id)
            licenseFindings.forEach { finding ->
                emitFinding(Finding(
                    type = FindingType.LICENSE_CHECK,
                    severity = Severity.HIGH,
                    description = "License validation detected",
                    details = finding,
                    recommendation = "Hook or NOP the license check function"
                ))
            }
            
            // 4. Vyhledání server endpointů
            val endpoints = r2frida.findServerEndpoints(session.id)
            endpoints.forEach { endpoint ->
                emitFinding(Finding(
                    type = FindingType.SERVER_VALIDATION,
                    severity = Severity.CRITICAL,
                    description = "Server validation endpoint detected",
                    details = endpoint,
                    recommendation = "Bypass server validation via Frida hook"
                ))
            }
            
            // 5. SSL pinning bypass
            _analysisState.value = AnalysisState.BYPASSING
            if (r2frida.bypassSSLPinning(session.id)) {
                emitFinding(Finding(
                    type = FindingType.SSL_PINNING,
                    severity = Severity.MEDIUM,
                    description = "SSL pinning bypassed successfully",
                    details = "SSL pinning was present and has been disabled",
                    recommendation = "Monitor for additional SSL checks"
                ))
            }
            
            // 6. Bypass server validací
            if (endpoints.isNotEmpty()) {
                r2frida.bypassServerValidation(session.id, endpoints)
            }
            
            // 7. Analýza native knihoven (pokud existují)
            val nativeLibs = findNativeLibraries(packageName)
            for (lib in nativeLibs) {
                val analysis = r2ghidra.analyzeNativeLibrary(lib)
                val obfuscations = r2ghidra.detectObfuscation(lib)
                
                obfuscations.forEach { obf ->
                    emitFinding(Finding(
                        type = FindingType.OBFUSCATION,
                        severity = Severity.HIGH,
                        description = "Obfuscation detected in $lib",
                        details = obf,
                        recommendation = "Use r2ghidra decompilation for analysis"
                    ))
                }
                
                analysis.interestingPatterns.forEach { pattern ->
                    emitFinding(Finding(
                        type = FindingType.NATIVE_CHECK,
                        severity = Severity.HIGH,
                        description = "Native check function found",
                        details = "$pattern in $lib",
                        recommendation = "Hook native function via Frida"
                    ))
                }
            }
            
            // 8. Enumrace tříd a metod
            _analysisState.value = AnalysisState.HOOKING
            val classes = r2frida.enumerateClasses(session.id)
            
            // Vyčištění
            r2frida.killSession(session.id)
            
            _analysisState.value = AnalysisState.COMPLETED
            
            AnalysisResult(
                packageName = packageName,
                findings = emptyList(), // findings are emitted via flow
                analysisTime = System.currentTimeMillis(),
                success = true
            )
            
        } catch (e: Exception) {
            _analysisState.value = AnalysisState.ERROR
            AnalysisResult(
                packageName = packageName,
                findings = emptyList(),
                analysisTime = System.currentTimeMillis(),
                success = false,
                error = e.message
            )
        }
    }.await()

    private suspend fun emitFinding(finding: Finding) {
        _findings.emit(finding)
    }

    private fun findNativeLibraries(packageName: String): List<String> {
        val libDir = File("/data/app/$packageName-*/lib/arm64")
        if (!libDir.exists()) return emptyList()
        
        return libDir.listFiles { file -> 
            file.name.endsWith(".so") 
        }?.map { it.absolutePath } ?: emptyList()
    }

    data class AnalysisResult(
        val packageName: String,
        val findings: List<Finding>,
        val analysisTime: Long,
        val success: Boolean,
        val error: String? = null
    )
}
