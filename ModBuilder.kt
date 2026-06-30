package com.automod.ai.modbuilder

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import java.io.File
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.security.PrivateKey
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.SecureRandom
import java.util.Date
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry

class ModBuilder(private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _status = MutableStateFlow(BuildStatus.IDLE)
    val status: StateFlow<BuildStatus> = _status.asStateFlow()

    enum class BuildStatus { IDLE, DECOMPILING, PATCHING, RECOMPILING, SIGNING, COMPLETED, ERROR }

    @Serializable
    data class ModConfig(
        val removeLicenses: Boolean = true,
        val bypassServerValidation: Boolean = true,
        val removeAds: Boolean = true,
        val unlockPremium: Boolean = true,
        val addHackMenu: Boolean = false,
        val hackMenuFeatures: HackMenuConfig = HackMenuConfig(),
        val packageRename: String? = null,
        val customPatches: List<SmaliPatch> = emptyList()
    )

    @Serializable
    data class HackMenuConfig(
        val enable: Boolean = false,
        val godMode: Boolean = false,
        val oneHitKill: Boolean = false,
        val infiniteAmmo: Boolean = false,
        val noReload: Boolean = false,
        val speedHack: Boolean = false,
        val wallhack: Boolean = false,
        val aimbot: Boolean = false,
        val xray: Boolean = false,
        val moneyHack: Boolean = false,
        val xpMultiplier: Float = 1.0f,
        val customToggles: List<String> = emptyList()
    )

    @Serializable
    data class SmaliPatch(
        val filePath: String,
        val searchPattern: String,
        val replacementPattern: String,
        val description: String = ""
    )

    suspend fun buildMod(
        apkPath: String,
        config: ModConfig,
        outputPath: String
    ): File = scope.async {
        _status.value = BuildStatus.DECOMPILING
        _progress.value = 0.1f

        val workingDir = createWorkingDir()
        
        try {
            // 1. Decompilace APK
            val decompiledDir = decompileAPK(apkPath, workingDir)
            _progress.value = 0.3f
            
            // 2. Aplikace patchů
            _status.value = BuildStatus.PATCHING
            applyPatches(decompiledDir, config)
            _progress.value = 0.6f
            
            // 3. Vložení hack menu (pokud je požadováno)
            if (config.addHackMenu) {
                injectHackMenu(decompiledDir, config.hackMenuFeatures)
            }
            
            _progress.value = 0.8f
            
            // 4. Recompilace
            _status.value = BuildStatus.RECOMPILING
            val unsignedApk = recompileAPK(decompiledDir, workingDir)
            _progress.value = 0.9f
            
            // 5. Signování
            _status.value = BuildStatus.SIGNING
            val signedApk = signAPK(unsignedApk, outputPath)
            _progress.value = 1.0f
            
            _status.value = BuildStatus.COMPLETED
            signedApk
            
        } catch (e: Exception) {
            _status.value = BuildStatus.ERROR
            throw e
        } finally {
            workingDir.deleteRecursively()
        }
    }.await()

    private fun createWorkingDir(): File {
        val dir = File.createTempFile("modbuild", "")
        dir.delete()
        dir.mkdirs()
        return dir
    }

    private suspend fun decompileAPK(apkPath: String, workingDir: File): File = 
        withContext(Dispatchers.IO) {
            val outputDir = File(workingDir, "decompiled")
            outputDir.mkdirs()
            
            val process = ProcessBuilder(
                "java", "-jar", "apktool.jar",
                "d", "-f", "-o", outputDir.absolutePath,
                apkPath
            ).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().readText()
            if (process.waitFor() != 0) {
                throw RuntimeException("APKTool decompile failed: $output")
            }
            
            outputDir
        }

    private suspend fun applyPatches(decompiledDir: File, config: ModConfig) {
        withContext(Dispatchers.Default) {
            val smaliDirs = decompiledDir.walkTopDown()
                .filter { it.isDirectory && it.name.startsWith("smali") }
                .toList()
            
            // Automatické vyhledání licenčních kontrol
            if (config.removeLicenses) {
                for (smaliDir in smaliDirs) {
                    smaliDir.walkTopDown()
                        .filter { it.name.endsWith(".smali") }
                        .forEach { smaliFile ->
                            patchLicenseCheck(smaliFile)
                        }
                }
            }
            
            // Odebraní reklam
            if (config.removeAds) {
                removeAdCode(decompiledDir)
            }
            
            // Unlock premium
            if (config.unlockPremium) {
                unlockPremiumFeatures(decompiledDir)
            }
            
                        // Custom patche
            config.customPatches.forEach { patch ->
                applySmaliPatch(decompiledDir, patch)
            }
        }
    }

    private fun patchLicenseCheck(smaliFile: File) {
        val content = smaliFile.readText()
        val licensePatterns = listOf(
            Regex("invoke-.*->.*isLicensed\\(.*\\)Z"),
            Regex("invoke-.*->.*verifyLicense\\(.*\\)Z"),
            Regex("invoke-.*->.*checkLicense\\(.*\\)Z"),
            Regex("invoke-.*->.*validateLicense\\(.*\\)Z"),
            Regex("invoke-.*->.*hasValidLicense\\(.*\\)Z"),
            Regex("invoke-.*->.*isPurchased\\(.*\\)Z"),
            Regex("invoke-.*->.*isPremium\\(.*\\)Z")
        )

        var patched = content
        for (pattern in licensePatterns) {
            patched = patched.replace(pattern) { match ->
                // Nahradíme volání license checku za const/4 v0, 0x1 (vždy true)
                val indent = " ".repeat(match.value.length.coerceAtMost(4))
                "$indentconst/4 v0, 0x1"
            }
        }

        if (patched != content) {
            smaliFile.writeText(patched)
        }
    }

    private fun removeAdCode(decompiledDir: File) {
        val adPatterns = listOf(
            Regex("com\\.google\\.android\\.gms\\.ads"),
            Regex("com\\.facebook\\.ads"),
            Regex("com\\.startapp\\.android"),
            Regex("com\\.chartboost\\.sdk"),
            Regex("com\\.vungle\\.sdk"),
            Regex("com\\.unity3d\\.ads"),
            Regex("com\\.applovin\\.sdk"),
            Regex("com\\.ironsource\\.mediationsdk"),
            Regex("com\\.adcolony\\.sdk")
        )

        decompiledDir.walkTopDown()
            .filter { it.name.endsWith(".smali") }
            .forEach { smaliFile ->
                var content = smaliFile.readText()
                for (pattern in adPatterns) {
                    if (pattern.containsMatchIn(content)) {
                        // Nahradíme ad metody za no-op
                        content = content.replace(
                            Regex("invoke-.*$pattern.*"),
                            ""
                        )
                    }
                }
                smaliFile.writeText(content)
            }
    }

    private fun unlockPremiumFeatures(decompiledDir: File) {
        val premiumPatterns = listOf(
            Regex("isPremium\\s*\\(\\)Z"),
            Regex("isPro\\s*\\(\\)Z"),
            Regex("isUnlocked\\s*\\(\\)Z"),
            Regex("hasFullVersion\\s*\\(\\)Z"),
            Regex("isUserPremium\\s*\\(\\)Z")
        )

        decompiledDir.walkTopDown()
            .filter { it.name.endsWith(".smali") }
            .forEach { smaliFile ->
                var content = smaliFile.readText()
                for (pattern in premiumPatterns) {
                    content = content.replace(pattern) { match ->
                        val methodName = match.value
                        // Najdeme implementaci a nahradíme return false za return true
                        content.replace(
                            "$methodName\r\n    const/4 v0, 0x0\r\n    return v0",
                            "$methodName\r\n    const/4 v0, 0x1\r\n    return v0"
                        )
                    }
                }
                smaliFile.writeText(content)
            }
    }

    private fun applySmaliPatch(decompiledDir: File, patch: SmaliPatch) {
        val targetFile = File(decompiledDir, patch.filePath)
        if (!targetFile.exists()) return

        var content = targetFile.readText()
        if (content.contains(patch.searchPattern)) {
            content = content.replace(patch.searchPattern, patch.replacementPattern)
            targetFile.writeText(content)
        }
    }

    private suspend fun injectHackMenu(decompiledDir: File, config: HackMenuConfig) {
        withContext(Dispatchers.Default) {
            val smaliMainActivity = findMainActivity(decompiledDir) ?: return@withContext
            
            // Vytvoříme overlay hack menu
            val hackMenuSmali = generateHackMenuSmali(config)
            val hackMenuFile = File(decompiledDir, "smali/com/automod/hackmenu/HackMenu.smali")
            hackMenuFile.parentFile.mkdirs()
            hackMenuFile.writeText(hackMenuSmali)
            
            // Vložíme volání hack menu do MainActivity
            injectHackMenuCall(smaliMainActivity)
            
            // Přidáme permissions
            addPermissions(decompiledDir)
            
            // Vytvoříme layout
            createHackMenuLayout(decompiledDir, config)
        }
    }

    private fun findMainActivity(decompiledDir: File): File? {
        val androidManifest = File(decompiledDir, "AndroidManifest.xml")
        if (!androidManifest.exists()) return null

        val manifest = androidManifest.readText()
        val regex = Regex("android:name=\"([\\w.]+)\"")
        val activities = regex.findAll(manifest).toList()

        for (activity in activities) {
            val activityName = activity.groupValues[1]
            val smaliPath = "smali/" + activityName.replace('.', '/') + ".smali"
            val smaliFile = File(decompiledDir, smaliPath)
            if (smaliFile.exists()) return smaliFile
        }

        return null
    }

    private fun generateHackMenuSmali(config: HackMenuConfig): String {
        return """
.class public Lcom/automod/hackmenu/HackMenu;
.super Landroid/app/Activity;
.source "HackMenu.java"

# instance fields
.field public static instance:Lcom/automod/hackmenu/HackMenu;

# direct methods
.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Landroid/app/Activity;-><init>()V
    return-void
.end method

.method public static showMenu(Landroid/content/Context;)V
    .registers 3
    new-instance v0, Landroid/content/Intent;
    const-class v1, Lcom/automod/hackmenu/HackMenu;
    invoke-direct {v0, p0, v1}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V
    const/high16 v1, 0x10000000
    invoke-virtual {v0, v1}, Landroid/content/Intent;->addFlags(I)Landroid/content/Intent;
    invoke-virtual {p0, v0}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V
    return-void
.end method

.method protected onCreate(Landroid/os/Bundle;)V
    .registers 3
    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V
    
    const/high16 v0, 0x7f030000
    invoke-virtual {p0, v0}, Lcom/automod/hackmenu/HackMenu;->setContentView(I)V
    
    sput-object p0, Lcom/automod/hackmenu/HackMenu;->instance:Lcom/automod/hackmenu/HackMenu;
    return-void
.end method

.method public static toggleGodMode()V
    .registers 2
    sget-object v0, Lcom/automod/hackmenu/HackMenu;->instance:Lcom/automod/hackmenu/HackMenu;
    if-nez v0, :cond_0
    return-void
    :cond_0
    const-string v0, "HACK"
    const-string v1, "God Mode Toggled"
    invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    return-void
.end method

.method public static toggleOneHitKill()V
    .registers 2
    sget-object v0, Lcom/automod/hackmenu/HackMenu;->instance:Lcom/automod/hackmenu/HackMenu;
    if-nez v0, :cond_0
    return-void
    :cond_0
    const-string v0, "HACK"
    const-string v1, "One Hit Kill Toggled"
    invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    return-void
.end method

.method public static toggleWallhack()V
    .registers 2
    sget-object v0, Lcom/automod/hackmenu/HackMenu;->instance:Lcom/automod/hackmenu/HackMenu;
    if-nez v0, :cond_0
    return-void
    :cond_0
    const-string v0, "HACK"
    const-string v1, "Wallhack Toggled"
    invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    return-void
.end method

.method public static toggleAimbot()V
    .registers 2
    sget-object v0, Lcom/automod/hackmenu/HackMenu;->instance:Lcom/automod/hackmenu/HackMenu;
    if-nez v0, :cond_0
    return-void
    :cond_0
    const-string v0, "HACK"
    const-string v1, "Aimbot Toggled"
    invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    return-void
.end method

.method public static toggleXRay()V
    .registers 2
    sget-object v0, Lcom/automod/hackmenu/HackMenu;->instance:Lcom/automod/hackmenu/HackMenu;
    if-nez v0, :cond_0
    return-void
    :cond_0
    const-string v0, "HACK"
    const-string v1, "XRay Toggled"
    invoke-static {v0, v1}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    return-void
.end method

.method public static setSpeedMultiplier(F)V
    .registers 3
    sget-object v0, Lcom/automod/hackmenu/HackMenu;->instance:Lcom/automod/hackmenu/HackMenu;
    if-nez v0, :cond_0
    return-void
    :cond_0
    new-instance v0, Ljava/lang/StringBuilder;
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V
    const-string v1, "Speed Multiplier: "
    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(F)Ljava/lang/StringBuilder;
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0
    const-string v1, "HACK"
    invoke-static {v1, v0}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    return-void
.end method

.method public static setMoneyMultiplier(F)V
    .registers 3
    sget-object v0, Lcom/automod/hackmenu/HackMenu;->instance:Lcom/automod/hackmenu/HackMenu;
    if-nez v0, :cond_0
    return-void
    :cond_0
    new-instance v0, Ljava/lang/StringBuilder;
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V
    const-string v1, "Money Multiplier: "
    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(F)Ljava/lang/StringBuilder;
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0
    const-string v1, "HACK"
    invoke-static {v1, v0}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    return-void
.end method

.method public static setXPMultiplier(F)V
    .registers 3
    sget-object v0, Lcom/automod/hackmenu/HackMenu;->instance:Lcom/automod/hackmenu/HackMenu;
    if-nez v0, :cond_0
    return-void
    :cond_0
    new-instance v0, Ljava/lang/StringBuilder;
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V
    const-string v1, "XP Multiplier: "
    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(F)Ljava/lang/StringBuilder;
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0
    const-string v1, "HACK"
    invoke-static {v1, v0}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I
    return-void
.end method
        """.trimIndent()
    }

    private fun injectHackMenuCall(mainActivity: File) {
        var content = mainActivity.readText()
        
        // Najdeme onCreate a vložíme volání HackMenu
        val onCreateRegex = Regex("\.method protected onCreate\(Landroid/os/Bundle;\)V")
        val onCreateEndRegex = Regex("return-void\s*\n\.end method")
        
        val injection = """
    new-instance v0, Ljava/lang/Thread;
    new-instance v1, Lcom/automod/hackmenu/HackMenuStartup;
    invoke-direct {v1}, Lcom/automod/hackmenu/HackMenuStartup;-><init>()V
    invoke-direct {v0, v1}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V
    invoke-virtual {v0}, Ljava/lang/Thread;->start()V
        """.trimIndent()
        
        if (onCreateRegex.containsMatchIn(content)) {
            content = content.replace(onCreateRegex) {
                "$0\n$injection"
            }
        }
        
        mainActivity.writeText(content)
        
        // Vytvoříme startup runnable
        val startupSmali = """
.class public Lcom/automod/hackmenu/HackMenuStartup;
.super Ljava/lang/Object;
.implements Ljava/lang/Runnable;
.source "HackMenuStartup.java"

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public run()V
    .registers 4
    const-wide/16 v0, 0x1388
    invoke-static {v0, v1}, Ljava/lang/Thread;->sleep(J)V
    :try_start_0
    iget-object v0, p0, Lcom/automod/hackmenu/HackMenuStartup;->this$0:Ljava/lang/Object;
    check-cast v0, Landroid/content/Context;
    invoke-static {v0}, Lcom/automod/hackmenu/HackMenu;->showMenu(Landroid/content/Context;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0
    :catch_0
    return-void
.end method
        """.trimIndent()
        
        val startupFile = File(mainActivity.parentFile, "../../hackmenu/HackMenuStartup.smali")
        startupFile.parentFile.mkdirs()
        startupFile.writeText(startupSmali)
    }

    private fun addPermissions(decompiledDir: File) {
        val manifestFile = File(decompiledDir, "AndroidManifest.xml")
        if (!manifestFile.exists()) return

        var manifest = manifestFile.readText()
        
        val permissionsToAdd = listOf(
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.SYSTEM_OVERLAY_WINDOW"
        )
        
        for (permission in permissionsToAdd) {
            if (!manifest.contains(permission)) {
                manifest = manifest.replace(
                    "</manifest>",
                    "    <uses-permission android:name=\"$permission\" />\n</manifest>"
                )
            }
        }
        
        manifestFile.writeText(manifest)
    }

    private fun createHackMenuLayout(decompiledDir: File, config: HackMenuConfig) {
        val layoutDir = File(decompiledDir, "res/layout")
        layoutDir.mkdirs()
        
        val layout = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
            appendLine("<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"")
            appendLine("    android:layout_width=\"match_parent\"")
            appendLine("    android:layout_height=\"match_parent\"")
            appendLine("    android:orientation=\"vertical\"")
            appendLine("    android:gravity=\"center\"")
            appendLine("    android:background=\"#CC000000\">")
            
            appendLine("    <TextView")
            appendLine("        android:layout_width=\"wrap_content\"")
            appendLine("        android:layout_height=\"wrap_content\"")
            appendLine("        android:text=\"HACK MENU\"")
            appendLine("        android:textColor=\"#00FF00\"")
            appendLine("        android:textSize=\"24sp\"")
            appendLine("        android:textStyle=\"bold\"")
            appendLine("        android:layout_marginBottom=\"20dp\"/>")
            
            if (config.godMode) {
                appendToggleButton("GOD MODE", "toggleGodMode")
            }
            if (config.oneHitKill) {
                appendToggleButton("ONE HIT KILL", "toggleOneHitKill")
            }
            if (config.aimbot) {
                appendToggleButton("AIMBOT", "toggleAimbot")
            }
            if (config.wallhack) {
                appendToggleButton("WALLHACK", "toggleWallhack")
            }
            if (config.xray) {
                appendToggleButton("XRAY", "toggleXRay")
            }
            
            appendLine("</LinearLayout>")
        }
        
        File(layoutDir, "hack_menu.xml").writeText(layout)
    }

    private fun StringBuilder.appendToggleButton(text: String, method: String) {
        appendLine("    <Button")
        appendLine("        android:layout_width=\"match_parent\"")
        appendLine("        android:layout_height=\"wrap_content\"")
        appendLine("        android:text=\"$text\"")
        appendLine("        android:onClick=\"$method\"")
        appendLine("        android:layout_margin=\"5dp\"/>")
    }

    private suspend fun recompileAPK(decompiledDir: File, workingDir: File): File = 
        withContext(Dispatchers.IO) {
            val outputApk = File(workingDir, "unsigned.apk")
            
            val process = ProcessBuilder(
                "java", "-jar", "apktool.jar",
                "b", "-o", outputApk.absolutePath,
                decompiledDir.absolutePath
            ).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().readText()
            if (process.waitFor() != 0) {
                throw RuntimeException("APKTool recompile failed: $output")
            }
            
            outputApk
        }

    private suspend fun signAPK(unsignedApk: File, outputPath: String): File = 
        withContext(Dispatchers.IO) {
            val signedApk = File(outputPath)
            
            // Generování keystore za běhu
            val keystore = generateKeystore()
            val keystoreFile = File.createTempFile("keystore", ".jks")
            keystore.store(keystoreFile.outputStream(), "android".toCharArray())
            
            val process = ProcessBuilder(
                "java", "-jar", "uber-apk-signer.jar",
                "--ks", keystoreFile.absolutePath,
                "--ks-pass", "pass:android",
                "--ks-key-alias", "modkey",
                "--key-pass", "pass:android",
                "-a", unsignedApk.absolutePath,
                "-o", signedApk.absolutePath
            ).redirectErrorStream(true).start()
            
            val output = process.inputStream.bufferedReader().readText()
            if (process.waitFor() != 0) {
                throw RuntimeException("APK signing failed: $output")
            }
            
            keystoreFile.delete()
            signedApk
        }

    private fun generateKeystore(): KeyStore {
        val keyStore = KeyStore.getInstance("JKS")
        keyStore.load(null, "android".toCharArray())
        
        val keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048, SecureRandom())
        val keyPair = keyPairGenerator.generateKeyPair()
        
        val issuer = X500Name("CN=AutoMod AI, OU=Development, O=AutoMod, C=US")
        val serial = BigInteger(64, SecureRandom())
        val notBefore = Date()
        val notAfter = Date(notBefore.time + 365 * 24 * 3600 * 1000L)
        
        val builder = X509v3CertificateBuilder(
            issuer, serial, notBefore, notAfter, issuer, keyPair.public
        )
        
        val signer = JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.private)
        val cert = JcaX509CertificateConverter().getCertificate(builder.build(signer))
        
        keyStore.setKeyEntry("modkey", keyPair.private, "android".toCharArray(), arrayOf(cert))
        
        return keyStore
    }
}

$$
