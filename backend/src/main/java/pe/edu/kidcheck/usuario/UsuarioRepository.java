package pe.edu.kidcheck.usuario;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = {"rol", "colegio"})
    Optional<Usuario> findByUsuarioLogin(String usuarioLogin);
}
