-dontwarn javax.annotation.Nullable
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.OpenSSLProvider

# Spatial systems use runtime class identity and native/reflection registration.
# Preserve the SDK boundary: the minified release failed during registration
# with "n1 already registered ... Duplicate Systems are not allowed".
-keep,allowshrinking class com.meta.spatial.** { *; }
