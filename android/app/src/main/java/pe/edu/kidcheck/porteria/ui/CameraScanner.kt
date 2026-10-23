package pe.edu.kidcheck.porteria.ui

import android.annotation.SuppressLint
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraScanner(
    activo: Boolean,
    modifier: Modifier = Modifier,
    onQrDetectado: (String) -> Unit
) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(contexto).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val opciones = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    }
    val escaner = remember { BarcodeScanning.getClient(opciones) }
    val ejecutor = remember { Executors.newSingleThreadExecutor() }
    val filtro = remember { FiltroEscaneo() }
    val activoActual = rememberUpdatedState(activo)
    val alDetectar = rememberUpdatedState(onQrDetectado)
    var proveedorCamara by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            proveedorCamara?.unbindAll()
            escaner.close()
            ejecutor.shutdown()
        }
    }

    LaunchedEffect(ciclo) {
        val futuro = ProcessCameraProvider.getInstance(contexto)
        futuro.addListener({
            try {
                val proveedor = futuro.get()
                proveedorCamara = proveedor
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analisis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analisis.setAnalyzer(ejecutor) { imagen ->
                    procesar(imagen, escaner, filtro, activoActual.value, alDetectar.value)
                }

                proveedor.unbindAll()
                proveedor.bindToLifecycle(
                    ciclo,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analisis
                )
                error = null
            } catch (ex: Exception) {
                Log.e("KidCheckCamera", "No se pudo iniciar la cámara", ex)
                error = "No se encontró una cámara disponible. Usa la búsqueda manual."
            }
        }, ContextCompat.getMainExecutor(contexto))
    }

    Box(
        modifier = modifier
            .background(Color(0xFF101613), RoundedCornerShape(18.dp))
            .border(3.dp, Verde, RoundedCornerShape(18.dp))
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(14.dp)
                .background(Color(0xCC0B2417), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                error ?: "Acerca la credencial QR al centro de la cámara",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun procesar(
    imagen: ImageProxy,
    escaner: com.google.mlkit.vision.barcode.BarcodeScanner,
    filtro: FiltroEscaneo,
    activo: Boolean,
    alDetectar: (String) -> Unit
) {
    val media = imagen.image
    if (media == null) {
        imagen.close()
        return
    }
    val entrada = InputImage.fromMediaImage(media, imagen.imageInfo.rotationDegrees)
    escaner.process(entrada)
        .addOnSuccessListener { codigos ->
            if (!activo) return@addOnSuccessListener
            val valor = codigos.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
            if (valor != null && filtro.aceptar(valor)) {
                alDetectar(valor)
            }
        }
        .addOnFailureListener { ex ->
            Log.w("KidCheckCamera", "Error al leer QR", ex)
        }
        .addOnCompleteListener {
            imagen.close()
        }
}

private class FiltroEscaneo {
    private var ultimo = ""
    private var momento = 0L

    @Synchronized
    fun aceptar(valor: String): Boolean {
        val ahora = SystemClock.elapsedRealtime()
        if (valor == ultimo && ahora - momento < 4000L) return false
        ultimo = valor
        momento = ahora
        return true
    }
}
