package pe.edu.kidcheck.porteria.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Verde = Color(0xFF198754)
val VerdeOscuro = Color(0xFF0F5132)
val VerdeClaro = Color(0xFFE7F4EC)
val Fondo = Color(0xFFF3F6F4)
val Texto = Color(0xFF14211A)
val TextoSuave = Color(0xFF5D6B63)
val Borde = Color(0xFFD7E0D9)
val Rojo = Color(0xFFB42318)
val RojoClaro = Color(0xFFFDECEA)
val Ambar = Color(0xFFB54708)
val AmbarClaro = Color(0xFFFFF4E5)
val Azul = Color(0xFF1F4FB8)

private val colores = lightColorScheme(
    primary = Verde,
    onPrimary = Color.White,
    primaryContainer = VerdeClaro,
    onPrimaryContainer = VerdeOscuro,
    secondary = VerdeOscuro,
    onSecondary = Color.White,
    background = Fondo,
    onBackground = Texto,
    surface = Color.White,
    onSurface = Texto,
    surfaceVariant = Color(0xFFF7F9F7),
    onSurfaceVariant = TextoSuave,
    outline = Borde,
    error = Rojo,
    onError = Color.White,
    errorContainer = RojoClaro,
    onErrorContainer = Rojo
)

private val tipografia = Typography(
    headlineLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontSize = 27.sp, fontWeight = FontWeight.Bold, lineHeight = 33.sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
)

private val formas = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun KidCheckTheme(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colores,
        typography = tipografia,
        shapes = formas,
        content = contenido
    )
}
