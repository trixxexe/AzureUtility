# Add project specific ProGuard rules here.

# Room Database keep rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * {
    *;
}
-keep @androidx.room.Entity class * {
    *;
}
-keep class com.example.data.db.** { *; }

# Markwon keep rules
-keep class io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**

# ZXing Core keep rules
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# MLKit Barcode Scanning keep rules
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Jetpack Compose and ViewModels keep rules
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep Composable annotated classes/methods
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# DataStore keep rules
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Gson / Moshi keep rules if used
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# General debugging
-keepattributes Signature,Exception,InnerClasses,EnclosingMethod,Deprecated,SourceFile,LineNumberTable,*Annotation*
