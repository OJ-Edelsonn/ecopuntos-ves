package pe.ecopuntosves.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.ecopuntosves.model.Rol;
public interface RolRepository extends JpaRepository<Rol, Integer> {
    Optional<Rol> findByNombre(String nombre);
}
