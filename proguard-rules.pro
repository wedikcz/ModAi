# Keep AutoMod AI main classes
-keep class com.automod.ai.** { *; }
-keep class com.automod.ai.metacognitive.** { *; }
-keep class com.automod.ai.tools.** { *; }
-keep class com.automod.ai.r2frida.** { *; }
-keep class com.automod.ai.r2ghidra.** { *; }
-keep class com.automod.ai.analyzer.** { *; }
-keep class com.automod.ai.modbuilder.** { *; }
-keep class com.automod.ai.service.** { *; }
-keep class com.automod.ai.ui.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.automod.ai.**$$serializer { *; }
-keepclassmembers class com.automod.ai.** {
    *** Companion;
}
-keepclasseswithmembers class com.automod.ai.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep BouncyCastle
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep radare2
-keep class io.radare.** { *; }
-dontwarn io.radare.**

# Keep Frida
-keep class com.frida.** { *; }
-dontwarn com.frida.**

# Keep OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep signatures
-keepattributes Signature
-keepattributes *Annotation*, EnclosingMethod, InnerClasses

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Keep serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Optimize
-optimizationpasses 10
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively
-repackageclasses 'com.automod.ai.obf'
-flattenpackagehierarchy
