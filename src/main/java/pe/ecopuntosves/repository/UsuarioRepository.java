package pe.ecopuntosves.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.ecopuntosves.model.Usuario;
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByUsername(String username);
    boolean existsByUsername(String username);
    Page<Usuario> findByRol_NombreIn(List<String> nombres, Pageable pageable);
    long countByRol_NombreAndActivoTrue(String nombre);
}
