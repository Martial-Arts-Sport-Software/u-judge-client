# AndroidX / standard libs
-keep class androidx.** { *; }
-dontwarn androidx.**

# Kotlin Coroutines/Reflect
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Compose Multiplatform
-keep class org.mass.** { *; }
-keepattributes **Annotation**
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

-keepclassmembers class * implements android.os.Parcelable {
    static *** CREATOR;
}