# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# Moshi
-keep class com.musicstream.app.data.remote.dto.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
