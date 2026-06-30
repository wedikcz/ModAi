package com.automod.ai.r2ghidra

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

class R2GhidraBridge(private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {

    companion object {
        private const val R2GHIDRA_PLUGIN = "r2ghidra"
        private const val GHIDRA_HEADLESS = "analyzeHeadless"
    }

    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Serializable
    data class DecompiledFunction(
        val name: String,
        val address: Long,
        val size: Int,
        val signature: String,
        val pseudocode: String,
        val calls: List<String>,
        val strings: List<String>,
        val complexity: Int
    )

    @Serializable
    data class AnalysisResult(
        val binaryPath: String,
        val architecture: String,
        val functions: List<DecompiledFunction>,
        val totalFunctions: Int,
        val entryPoints: List<String>,
        val interestingPatterns: List<String>
    )

    suspend fun analyzeNativeLibrary(libPath: String): AnalysisResult = withContext(Dispatchers.IO) {
        val result = executeR2GhidraCommand("ag $$", libPath)
        parseAnalysisResult(result, libPath)
    }

    suspend fun decompileFunction(libPath: String, functionName: String): DecompiledFunction = 
        withContext(Dispatchers.IO) {
            val result = executeR2GhidraCommand("pdc @ $functionName", libPath)
            parseDecompiledFunction(result, functionName)
        }

    suspend fun findStrings(libPath: String, pattern: String? = null): List<String> = 
        withContext(Dispatchers.IO) {
            val cmd = if (pattern != null) "izz~$pattern" else "izz"
            val result = executeR2GhidraCommand(cmd, libPath)
            result.lines()
                .filter { it.contains(pattern ?: "", ignoreCase = true) }
                .toList()
        }

    suspend fun findCrossReferences(libPath: String, functionName: String): List<String> = 
        withContext(Dispatchers.IO) {
            val result = executeR2GhidraCommand("axt @ $functionName", libPath)
            result.lines()
                .filter { it.isNotBlank() }
                .toList()
        }

    suspend fun detectObfuscation(libPath: String): List<String> = withContext(Dispatchers.IO) {
        val patterns = mutableListOf<String>()
        
        // Kontrola na kontrolní součty
        val checksumResult = executeR2GhidraCommand("e io.cache = false; p8 32 @ 0", libPath)
        // Obfuscation detection logic
        
        // Kontrola na anti-debug
        val antiDebugResult = executeR2GhidraCommand("/ antidebug", libPath)
        if (antiDebugResult.isNotBlank()) patterns.add("anti_debug_detected")
        
        // Kontrola na packery
        val packerResult = executeR2GhidraCommand("iI~packer", libPath)
        if (packerResult.isNotBlank()) patterns.add("packer_detected")
        
        patterns
    }

    suspend fun generatePatch(libPath: String, original: String, replacement: String): ByteArray = 
        withContext(Dispatchers.IO) {
            // Nalezení adresy a generování patche
            val addrResult = executeR2GhidraCommand("/ $original", libPath)
            val address = extractAddress(addrResult)
            
            if (address != null) {
                executeR2GhidraCommand($"wa $replacement @ $address", libPath)
                val patchedBytes = executeR2GhidraCommand($"p8 1024 @ $address", libPath)
                patchedBytes.hexToBytes()
            } else {
                byteArrayOf()
            }
        }

    private suspend fun executeR2GhidraCommand(command: String, libPath: String): String = 
        withContext(Dispatchers.IO) {
            val process = ProcessBuilder(
                "r2", "-q", "-c", 
                "$R2GHIDRA_PLUGIN; $command", 
                libPath
            ).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().readText()
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly()
            }
            output
        }

    private fun parseAnalysisResult(output: String, libPath: String): AnalysisResult {
        val functions = mutableListOf<DecompiledFunction>()
        val entryPoints = mutableListOf<String>()
        val interestingPatterns = mutableListOf<String>()
        
        // Parsování r2ghidra výstupu
        val funcRegex = Regex("0x([0-9a-fA-F]+)\\s+(\\d+)\\s+(\\w+)")
        funcRegex.findAll(output).forEach { match ->
            val addr = match.groupValues[1].toLongOrNull(16) ?: return@forEach
            val size = match.groupValues[2].toIntOrNull() ?: 0
            val name = match.groupValues[3]
            
            functions.add(
                DecompiledFunction(
                    name = name,
                    address = addr,
                    size = size,
                    signature = "",
                    pseudocode = "",
                    calls = emptyList(),
                    strings = emptyList(),
                    complexity = size / 10
                )
            )
            
            if (name.contains("JNI_") || name.contains("Java_")) {
                entryPoints.add(name)
            }
            
            if (name.contains("check", true) || 
                name.contains("verify", true) || 
                name.contains("license", true)) {
                interestingPatterns.add(name)
            }
        }
        
        return AnalysisResult(
            binaryPath = libPath,
            architecture = "aarch64",
            functions = functions,
            totalFunctions = functions.size,
            entryPoints = entryPoints,
            interestingPatterns = interestingPatterns
        )
    }

    private fun parseDecompiledFunction(output: String, functionName: String): DecompiledFunction {
        return DecompiledFunction(
            name = functionName,
            address = 0,
            size = 0,
            signature = output.lines().firstOrNull() ?: "",
            pseudocode = output,
            calls = output.lines().filter { it.contains("call") || it.contains("invoke") },
            strings = output.lines().filter { it.contains("\"") },
            complexity = output.lines().size
        )
    }

    private fun extractAddress(output: String): Long? {
        val regex = Regex("0x([0-9a-fA-F]+)")
        return regex.find(output)?.groupValues?.getOrNull(1)?.toLongOrNull(16)
    }
}

private fun String.hexToBytes(): ByteArray {
    return this.trim()
        .split("\\s+".toRegex())
        .mapNotNull { it.toIntOrNull(16)?.toByte() }
        .toByteArray()
}
