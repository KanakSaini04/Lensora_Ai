# ProGuard / R8 Rules for Lensora_Ai

# Hilt / Dagger
-keep class * extends dagger.hilt.internal.UnstableApi
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**

# TensorFlow Lite & GPU Delegate
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-dontwarn org.tensorflow.lite.**

# Media3 Transformer, ExoPlayer & Effects
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Kotlinx Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    public static *** serializer(...);
}

# ZXing QR Code
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# OpenCV
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**
