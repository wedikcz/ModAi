package com.automod.ai.r2frida

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class R2FridaBridge(private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
    
    companion object {
        private const val R2FRIDA_BINARY = "r2frida"
        private const val DEFAULT_TIMEOUT_MS = 30_000L
    }

    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    private val activeSessions = ConcurrentHashMap<String, R2FridaSession>()
    private var deviceConnected = false

    data class R2FridaSession(
        val id: String,
        val processId: Int,
        val processName: String,
        val architecture: String,
        val startedAt: Long
    )

    @Serializable
    data class MethodInfo(
        val className: String,
        val methodName: String,
        val signature: String,
        val isNative: Boolean,
        val isStatic: Boolean,
        val address: Long? = null
    )

    @Serializable
    data class ClassInfo(
        val name: String,
        val superclass: String?,
        val methods: List<MethodInfo>,
        val fields: List<String>,
        val isInterface: Boolean
    )

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(
                R2FRIDA_BINARY, "-q", "-c", "frida://spawn?device=usb"
            ).start()
            
            deviceConnected = process.waitFor(5, TimeUnit.SECONDS)
            deviceConnected
        } catch (e: Exception) {
            false
        }
    }

    suspend fun spawnPackage(packageName: String): R2FridaSession = withContext(Dispatchers.IO) {
        val result = executeR2FridaCommand("frida://spawn/$packageName")
        val sessionId = extractSessionId(result)
        
        R2FridaSession(
            id = sessionId,
            processId = extractProcessId(result),
            processName = packageName,
            architecture = "aarch64",
            startedAt = System.currentTimeMillis()
        ).also { activeSessions[sessionId] = it }
    }

    suspend fun enumerateClasses(sessionId: String, filter: String? = null): List<ClassInfo> = 
        withContext(Dispatchers.IO) {
            val cmd = if (filter != null) {
                "frida://$sessionId/classes?filter=$filter"
            } else {
                "frida://$sessionId/classes"
            }
            
            val result = executeR2FridaCommand(cmd)
            parseClasses(result)
        }

    suspend fun hookMethod(
        sessionId: String, 
        className: String, 
        methodName: String,
        onEnter: String? = null,
        onLeave: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val hookScript = buildHookScript(className, methodName, onEnter, onLeave)
        val cmd = "frida://$sessionId/inject?code=${hookScript.encodeURL()}"
        
        try {
            executeR2FridaCommand(cmd)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun traceFunction(
        sessionId: String,
        className: String,
        methodName: String,
        depth: Int = 3
    ): String = withContext(Dispatchers.IO) {
        val cmd = "frida://$sessionId/trace?class=$className&method=$methodName&depth=$depth"
        executeR2FridaCommand(cmd)
    }

    suspend fun readMemory(
        sessionId: String,
        address: Long,
        size: Int = 256
    ): ByteArray = withContext(Dispatchers.IO) {
        val cmd = "frida://$sessionId/memory?address=0x${address.toString(16)}&size=$size"
        val result = executeR2FridaCommand(cmd)
        result.hexToBytes()
    }

    suspend fun writeMemory(
        sessionId: String,
        address: Long,
        data: ByteArray
    ): Boolean = withContext(Dispatchers.IO) {
        val hexData = data.toHex()
        val cmd = "frida://$sessionId/memory_write?address=0x${address.toString(16)}&data=$hexData"
        
        try {
            executeR2FridaCommand(cmd)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun callMethod(
        sessionId: String,
        className: String,
        methodName: String,
        args: List<Any> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val argsJson = json.encodeToString(args)
        val cmd = "frida://$sessionId/call?class=$className&method=$methodName&args=${argsJson.encodeURL()}"
        executeR2FridaCommand(cmd)
    }

    suspend fun bypassSSLPinning(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        val script = """
            Java.perform(function() {
                var TrustManager = Java.registerClass({
                    name: 'javax.net.ssl.TrustManager',
                    methods: {
                        checkClientTrusted: function() {},
                        checkServerTrusted: function() {},
                        getAcceptedIssuers: function() { return []; }
                    }
                });
                
                var SSLContext = Java.use('javax.net.ssl.SSLContext');
                SSLContext.init.overload(
                    '[Ljavax.net.ssl.KeyManager;',
                    '[Ljavax.net.ssl.TrustManager;',
                    'java.security.SecureRandom'
                ).implementation = function(keyManager, trustManager, secureRandom) {
                    var tm = [TrustManager.$new()];
                    this.init(keyManager, tm, secureRandom);
                };
                
                var HttpsURLConnection = Java.use('javax.net.ssl.HttpsURLConnection');
                HttpsURLConnection.setDefaultHostnameVerifier.implementation = function(v) {};
                
                // Bypass common SSL pinning libs
                try {
                    var OkHttpClient = Java.use('okhttp3.OkHttpClient');
                    OkHttpClient.newCall.implementation = function(request) {
                        return this.newCall(request);
                    };
                } catch(e) {}
                
                try {
                    var TrustKit = Java.use('com.datatheorem.android.trustkit.TrustKit');
                    TrustKit.initializeWithNetworkSecurityConfiguration.implementation = function(c) {};
                } catch(e) {}
            });
        """.trimIndent()
        
        val cmd = "frida://$sessionId/inject?code=${script.encodeURL()}"
        try {
            executeR2FridaCommand(cmd)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun bypassRootDetection(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        val script = """
            Java.perform(function() {
                var RootBeer = Java.use('com.scottyab.rootbeer.RootBeer');
                RootBeer.isRooted.implementation = function() { return false; };
                RootBeer.detectRootManagementApps.implementation = function() { return false; };
                RootBeer.detectPotentiallyDangerousApps.implementation = function() { return false; };
                RootBeer.detectTestKeys.implementation = function() { return false; };
                RootBeer.checkForBusyBoxBinary.implementation = function() { return false; };
                RootBeer.checkForSuBinary.implementation = function() { return false; };
                RootBeer.checkForMagiskBinary.implementation = function() { return false; };
                
                try {
                    var SafetyNet = Java.use('com.google.android.gms.safetynet.SafetyNet');
                    SafetyNet.isSupported.implementation = function() { return false; };
                } catch(e) {}
            });
        """.trimIndent()
        
        val cmd = "frida://$sessionId/inject?code=${script.encodeURL()}"
        try {
            executeR2FridaCommand(cmd)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun bypassEmulatorDetection(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        val script = """
            Java.perform(function() {
                var Build = Java.use('android.os.Build');
                Build.FINGERPRINT.value = 'google/raven/raven:15/AP41.240823.009/12345678:user/release-keys';
                Build.MODEL.value = 'Pixel 7 Pro';
                Build.MANUFACTURER.value = 'Google';
                Build.BRAND.value = 'google';
                Build.DEVICE.value = 'raven';
                Build.HARDWARE.value = 'raven';
                Build.PRODUCT.value = 'raven';
                
                var File = Java.use('java.io.File');
                File.exists.overload().implementation = function() {
                    var path = this.getAbsolutePath();
                    if (path.contains('qemu') || path.contains('emulator')) return false;
                    return this.exists();
                };
            });
        """.trimIndent()
        
        val cmd = "frida://$sessionId/inject?code=${script.encodeURL()}"
        try {
            executeR2FridaCommand(cmd)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun findLicenseValidation(sessionId: String): List<String> = withContext(Dispatchers.IO) {
        val patterns = listOf(
            "license", "licence", "licensing", "validate", "verif",
            "signature", "checksum", "crc", "hash", "md5", "sha",
            "purchase", "receipt", "playstore", "inapp", "billing",
            "subscription", "unlock", "pro", "premium", "trial",
            "expir", "limit", "restrict", "block", "denied",
            "pirate", "crack", "hack", "mod", "patched"
        )
        
        val results = mutableListOf<String>()
        for (pattern in patterns) {
            val cmd = "frida://$sessionId/search?pattern=$pattern&scope=all"
            try {
                val result = executeR2FridaCommand(cmd)
                if (result.isNotEmpty() && !result.contains("not found")) {
                    results.addAll(parseSearchResults(result, pattern))
                }
            } catch (_: Exception) {}
        }
        results.distinct()
    }

    suspend fun findServerEndpoints(sessionId: String): List<String> = withContext(Dispatchers.IO) {
        val patterns = listOf(
            "https://", "http://", "api.", ".com", ".io", ".net",
            "server", "endpoint", "validate", "auth", "token",
            "register", "login", "check", "verif"
        )
        
        val results = mutableListOf<String>()
        for (pattern in patterns) {
            try {
                val cmd = "frida://$sessionId/search?pattern=$pattern&scope=strings"
                val result = executeR2FridaCommand(cmd)
                if (result.isNotEmpty()) {
                    results.addAll(extractUrls(result))
                }
            } catch (_: Exception) {}
        }
        results.distinct()
    }

    suspend fun bypassServerValidation(
        sessionId: String,
        endpoints: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        val endpointsJson = json.encodeToString(endpoints)
        val script = """
            Java.perform(function() {
                var endpoints = $endpointsJson;
                
                var URL = Java.use('java.net.URL');
                URL.openConnection.overload().implementation = function() {
                    var url = this.toString();
                    for (var i = 0; i < endpoints.length; i++) {
                        if (url.contains(endpoints[i])) {
                            console.log('[BYPASS] Intercepted: ' + url);
                            return Java.use('java.net.HttpURLConnection').$new(
                                Java.use('java.net.URL').$new('http://localhost/valid')
                            );
                        }
                    }
                    return this.openConnection();
                };
                
                var HttpURLConnection = Java.use('java.net.HttpURLConnection');
                HttpURLConnection.getResponseCode.implementation = function() {
                    var url = this.getURL().toString();
                    for (var i = 0; i < endpoints.length; i++) {
                        if (url.contains(endpoints[i])) {
                            return 200;
                        }
                    }
                    return this.getResponseCode();
                };
                
                var OkHttpClient = Java.use('okhttp3.OkHttpClient');
                var originalNewCall = OkHttpClient.newCall;
                OkHttpClient.newCall.implementation = function(request) {
                    var url = request.url().toString();
                    for (var i = 0; i < endpoints.length; i++) {
                        if (url.contains(endpoints[i])) {
                            var Response = Java.use('okhttp3.Response');
                            var ResponseBody = Java.use('okhttp3.ResponseBody');
                            var fakeBody = ResponseBody.create(
                                Java.use('okhttp3.MediaType').parse('application/json'),
                                JSON.stringify({success: true, valid: true})
                            );
                            return originalNewCall.call(this, request);
                        }
                    }
                    return originalNewCall.call(this, request);
                };
            });
        """.trimIndent()
        
        val cmd = "frida://$sessionId/inject?code=${script.encodeURL()}"
        try {
            executeR2FridaCommand(cmd)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun killSession(sessionId: String) = withContext(Dispatchers.IO) {
        try {
            executeR2FridaCommand("frida://$sessionId/kill")
            activeSessions.remove(sessionId)
        } catch (_: Exception) {}
    }

    private suspend fun executeR2FridaCommand(command: String): String = withContext(Dispatchers.IO) {
        val process = ProcessBuilder("sh", "-c", "$R2FRIDA_BINARY -q -c '$command'")
            .redirectErrorStream(true)
            .start()
        
        val output = process.inputStream.bufferedReader().readText()
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            throw TimeoutException("R2Frida command timed out: $command")
        }
        output
    }

    private fun buildHookScript(
        className: String,
        methodName: String,
        onEnter: String?,
        onLeave: String?
    ): String {
        return """
            Java.perform(function() {
                var target = Java.use('$className');
                var overloads = target.$methodName.overloads;
                for (var i = 0; i < overloads.length; i++) {
                    overloads[i].implementation = function() {
                        console.log('[HOOK] $className.$methodName called');
                        ${onEnter ?: "// no onEnter"}
                        var result = this.$methodName.apply(this, arguments);
                        ${onLeave ?: "// no onLeave"}
                        return result;
                    };
                }
            });
        """.trimIndent()
    }

    private fun extractSessionId(output: String): String {
        val regex = Regex("Session\\s+id\\s*=\\s*(\\w+)", RegexOption.IGNORE_CASE)
        return regex.find(output)?.groupValues?.getOrNull(1) ?: "unknown"
    }

    private fun extractProcessId(output: String): Int {
        val regex = Regex("PID:\\s*(\\d+)", RegexOption.IGNORE_CASE)
        return regex.find(output)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: -1
    }

    private fun parseClasses(output: String): List<ClassInfo> {
        // Parsování výstupu r2frida do strukturovaných dat
        val classes = mutableListOf<ClassInfo>()
        val classRegex = Regex("class\\s+([\\w.$]+)(?:\\s+extends\\s+([\\w.$]+))?", RegexOption.IGNORE_CASE)
        
        classRegex.findAll(output).forEach { match ->
            classes.add(
                ClassInfo(
                    name = match.groupValues[1],
                    superclass = match.groupValues.getOrNull(2),
                    methods = emptyList(),
                    fields = emptyList(),
                    isInterface = output.contains("interface")
                )
            )
        }
        return classes
    }

    private fun parseSearchResults(output: String, pattern: String): List<String> {
        return output.lines()
            .filter { it.contains(pattern, ignoreCase = true) }
            .map { it.trim() }
    }

    private fun extractUrls(output: String): List<String> {
        val urlRegex = Regex("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]+")
        return urlRegex.findAll(output).map { it.value }.distinct().toList()
    }
}

fun String.encodeURL(): String = 
    java.net.URLEncoder.encode(this, "UTF-8")

fun String.hexToBytes(): ByteArray {
    return this.split(" ").mapNotNull { it.toIntOrNull(16)?.toByte() }.toByteArray()
}

fun ByteArray.toHex(): String {
    return this.joinToString("") { "%02x".format(it) }
}
