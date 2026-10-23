package pe.edu.kidcheck.porteria.data

import org.junit.Assert.assertEquals
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
}
