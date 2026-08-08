# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/m-abidi/AndroidStudioProjects/Keeftalk2/gradle/proguard-android-optimize.txt
# You can edit the include path and syntax by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools-proguard.html

# Add any custom keep rules here.

-dontwarn com.microsoft.device.display.DisplayMask
-dontwarn edu.umd.cs.findbugs.annotations.NonNull
-dontwarn edu.umd.cs.findbugs.annotations.Nullable
-dontwarn edu.umd.cs.findbugs.annotations.SuppressFBWarnings
-dontwarn io.micrometer.context.ContextAccessor
-dontwarn reactor.blockhound.integration.BlockHoundIntegration
