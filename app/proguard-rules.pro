# AllerPaw ProGuard / R8 Regeln

# ── Hilt ──────────────────────────────────────────────────────────────────────
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# ── Moshi ─────────────────────────────────────────────────────────────────────
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepnames class com.squareup.moshi.** { *; }

# ── Retrofit ──────────────────────────────────────────────────────────────────
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── AllerPaw DTOs ─────────────────────────────────────────────────────────────
-keep class com.allerpaw.app.data.remote.dto.** { *; }

# ── DataStore ─────────────────────────────────────────────────────────────────
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }

# ── Vico Charts ───────────────────────────────────────────────────────────────
-keep class com.patrykandpatrick.vico.** { *; }

# ── Allgemein ─────────────────────────────────────────────────────────────────
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
