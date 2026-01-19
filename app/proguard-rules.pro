# Copyright 2024 anyone-Hub

# 1. FIX: Corrected plural syntax for attributes
-keepattributes SourceFile, LineNumberTable, Signature, *Annotation*

# 2. JNI / Native Bridge Protection
# Prevents R8 from renaming the Kotlin methods that your Python/C++ code calls
-keepclasseswithmembernames class * {
    native <methods>;
}

# 3. Python Pricing Engine Protection
# This ensures your JNI calls to the Python libs remain intact
-keep class com.anyonehub.barpos.util.PricingEngine { *; }
-keepclassmembers class com.anyonehub.barpos.util.PricingEngine { *; }

# 4. Room Database & SQL Integrity
# Prevents R8 from renaming your table columns (which causes SQL crashes)
-keepclassmembers class com.anyonehub.barpos.data.** { *; }
-keep class com.anyonehub.barpos.data.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# 5. JSON / Python Bridge Data
# Keeps the JSON objects used for the specialsJson logic
-keep class org.json.** { *; }

# 6. Hilt / Dagger DI (Critical for ViewModel injection)
-keep class androidx.hilt.** { *; }
-keep class com.google.dagger.** { *; }
-dontwarn com.google.errorprone.annotations.**

# 7. WorkManager / Hilt Worker Protection
# Critical: This prevents the 'NoSuchMethodException' during worker instantiation.
# WorkManager needs to reflectively access the constructor.
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep @androidx.hilt.work.HiltWorker class * extends androidx.work.ListenableWorker { *; }

