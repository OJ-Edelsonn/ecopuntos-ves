package pe.ecopuntosves.repository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.ecopuntosves.model.Vecino;
public interface VecinoRepository extends JpaRepository<Vecino, Integer> {
    boolean existsByDni(String dni);
    Optional<Vecino> findByUsuario_Username(String username);
    @Query("select v from Vecino v join v.usuario u " +
           "where v.dni like concat('%', :q, '%') " +
           "or lower(u.nombres) like lower(concat('%', :q, '%')) " +
           "or lower(u.apellidos) like lower(concat('%', :q, '%'))")
    Page<Vecino> buscar(@Param("q") String q, Pageable pageable);
}
