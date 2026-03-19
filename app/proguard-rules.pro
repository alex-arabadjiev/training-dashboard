# Training Dashboard ProGuard Rules

# Keep Room entities and DAOs
-keep class com.example.trainingdashboard.data.db.** { *; }

# Keep Compose runtime
-dontwarn androidx.compose.**
