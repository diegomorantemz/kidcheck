package pe.edu.kidcheck.porteria.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PorteriaHashTest {

    @Test
    fun `hashQr coincide con SHA-256 del backend`() {
        assertEquals(
            "b221d9dbb083a7f33428d7c2a3c3198ae925614d70210e28716ccaa7cd4ddb79",
            PorteriaRepository.hashQr("hola")
        )
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            PorteriaRepository.hashQr("")
        )
    }

    @Test
    fun `hashQr recorta espacios y saltos de linea`() {
        assertEquals(
            PorteriaRepository.hashQr("token-qr"),
            PorteriaRepository.hashQr("  token-qr\n")
        )
    }

    @Test
    fun `el token semilla de Camila coincide con su credencial`() {
        // SHA-256 de 'tok-camila-0031', el token de la semilla V2.
        assertEquals(
            "ce3b12487fdab9c7ebf24dee46fbcb8cff0e111a4a9e1e238f2012a9b710b1b8",
            PorteriaRepository.hashQr("tok-camila-0031")
        )
    }

    @Test
    fun `un token invalido no coincide con ninguna credencial semilla`() {
        val credenciales = setOf(
            PorteriaRepository.hashQr("tok-ana-0017"),
            PorteriaRepository.hashQr("tok-mateo-0022"),
            PorteriaRepository.hashQr("tok-camila-0031")
        )

        assertFalse(credenciales.contains(PorteriaRepository.hashQr("tok-invalido-9999")))
    }
}
