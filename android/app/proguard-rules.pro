# Reglas de R8 para la app de portería.
-keep class pe.edu.kidcheck.porteria.net.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Room ya incluye sus propias reglas; SQLCipher necesita conservar JNI.
-keep class net.zetetic.database.** { *; }
