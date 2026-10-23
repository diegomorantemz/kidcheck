package pe.edu.kidcheck.shared.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashesTest {

    @Test
    void calculaSha256EnHexadecimalMinusculas() {
        assertThat(Hashes.sha256Hex(""))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        assertThat(Hashes.sha256Hex("hola"))
                .isEqualTo("b221d9dbb083a7f33428d7c2a3c3198ae925614d70210e28716ccaa7cd4ddb79");
    }

    @Test
    void siempreDevuelveSesentaYCuatroCaracteres() {
        assertThat(Hashes.sha256Hex("token-de-prueba")).hasSize(64);
    }
}
