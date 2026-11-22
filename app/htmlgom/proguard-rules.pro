# Preserve JNI entry points and NativeThermalCore methods
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.absolutemango.htmlgom.core.NativeThermalCore { *; }

# Preserve GeckoView native runtime and reflection interfaces (Standard flavor)
-keep class org.mozilla.geckoview.** { *; }
-keep class org.mozilla.gecko.** { *; }
-keep class mozilla.components.** { *; }
-dontwarn org.mozilla.geckoview.**
-dontwarn org.mozilla.gecko.**
-dontwarn mozilla.components.**

# Keep GeckoView native annotations and annotated classes/members
-keep @interface org.mozilla.gecko.annotation.JNITarget
-keep @org.mozilla.gecko.annotation.JNITarget class *
-keepclassmembers @org.mozilla.gecko.annotation.JNITarget class * {
    *;
}
-keepclassmembers class * {
    @org.mozilla.gecko.annotation.JNITarget *;
}

-keep @interface org.mozilla.gecko.annotation.WebRTCJNITarget
-keep @org.mozilla.gecko.annotation.WebRTCJNITarget class *
-keepclassmembers @org.mozilla.gecko.annotation.WebRTCJNITarget class * {
    *;
}
-keep class org.mozilla.gecko.mozglue.JNIObject { *; }
-keep class * extends org.mozilla.gecko.mozglue.JNIObject { *; }

# Preserve Kyant Backdrop AGSL reflection and shaders
-keep class com.kyant.backdrop.** { *; }

# Preserve data serialization models for history persistence
-keepclassmembers class com.absolutemango.htmlgom.LastOpenedFileItem { *; }
-keepclassmembers class com.absolutemango.expressiveui.LastOpenedFileItem { *; }

# Sora Editor & SnakeYAML desktop reflection suppression
-dontwarn io.github.rosemoe.sora.util.ArrayList
-dontwarn java.beans.**
-dontwarn org.yaml.snakeyaml.**
