# ProGuard and R8 rules for htmlgow

# Keep Mozilla GeckoView and internal Gecko JNI bindings
-keep class org.mozilla.geckoview.** { *; }
-keep class org.mozilla.gecko.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Google Play Services Wearable APIs
-keep class com.google.android.gms.wearable.** { *; }

# Wear state, transfer manager, and internal utilities
-keep class com.absolutemango.htmlgow.WearState { *; }
-keep class com.absolutemango.htmlgow.WearTransferManager { *; }
-keep class com.absolutemango.htmlgow.FtpServerManager { *; }
-keep class com.absolutemango.htmlgow.ZipExtractor { *; }

# Suppress warnings for Apache FTPServer / MINA / SLF4J / Java desktop APIs
-dontwarn org.apache.ftpserver.**
-dontwarn org.apache.mina.**
-dontwarn org.slf4j.**
-dontwarn java.awt.**
-dontwarn javax.naming.**
-dontwarn javax.security.**
-dontwarn java.beans.**
-dontwarn javax.imageio.**
