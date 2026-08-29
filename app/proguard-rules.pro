# Keep main application class
-keep class com.MerWare.DaysSincePro.** { *; }

# Keep Activities
-keep class * extends android.app.Activity

# Keep Fragments
-keep class * extends android.app.Fragment
-keep class * extends androidx.fragment.app.Fragment

# Keep custom views
-keep class * extends android.view.View { *; }

# Keep anything referenced by reflection (if you know any)
