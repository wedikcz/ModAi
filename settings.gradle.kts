pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://raw.githubusercontent.com/wedikcz/r2-frida/master") }
        maven { url = uri("https://raw.githubusercontent.com/wedikcz/r2ghidra/master") }
    }
}

rootProject.name = "AutoModAI"
include(":app")
include(":r2frida_bridge")
include(":r2ghidra_bridge")
include(":metacognitive_core")
include(":dynamic_analyzer")
include(":mod_builder")
include(":hack_menu_engine")
