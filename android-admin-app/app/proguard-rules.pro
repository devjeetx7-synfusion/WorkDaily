# Keep Gson and Model classes
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
