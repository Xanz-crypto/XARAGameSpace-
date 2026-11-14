# Keep XARA Shizuku AIDL and UserService for Release builds
-keep class com.xara.gamespace.engine.XaraShellUserService { *; }
-keep class com.xara.gamespace.shizuku.** { *; }
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# Keep Kotlinx Serialization route classes for Navigation Compose
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.xara.gamespace.ui.navigation.** {
    *** Companion;
    public *;
}
