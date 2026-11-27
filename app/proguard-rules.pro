# ProGuard/R8 rules for Namazgâh.

# --- Gson (Aladhan DTO'ları yansıma ile çözülür) ---
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.anttech.prayertimes.core.network.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- Retrofit ---
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# --- Hilt / Dagger ---
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# --- OSMDroid (yansıma ile yüklenen parçalar) ---
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
