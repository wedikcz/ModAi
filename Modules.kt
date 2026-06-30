package com.automod.ai.di

import com.automod.ai.metacognitive.MetacognitiveEngine
import com.automod.ai.r2frida.R2FridaBridge
import com.automod.ai.r2ghidra.R2GhidraBridge
import com.automod.ai.analyzer.DynamicAnalyzer
import com.automod.ai.modbuilder.ModBuilder
import com.automod.ai.tools.AndroidReTools
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val autoModModule = module {
    single { R2FridaBridge() }
    single { R2GhidraBridge() }
    single { DynamicAnalyzer(get(), get()) }
    single { ModBuilder() }
    single { AndroidReTools() }
    single { MetacognitiveEngine() }
    
    single {
        val tools = AndroidReTools()
        val signer = tools.ApkSigner()
        mapOf(
            "apktool" to tools.ApkTool(),
            "signer" to signer,
            "godmode" to tools.GodModeEngine(),
            "dex_converter" to tools.DexToSmaliConverter(),
            "smali_converter" to tools.SmaliToDexConverter(),
            "xml_parser" to tools.AndroidBinaryXmlParser()
        )
    }
}
