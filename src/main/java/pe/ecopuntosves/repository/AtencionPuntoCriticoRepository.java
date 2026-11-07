package pe.ecopuntosves.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.ecopuntosves.model.AtencionPuntoCritico;

public interface AtencionPuntoCriticoRepository extends JpaRepository<AtencionPuntoCritico, Integer> {
    Optional<AtencionPuntoCritico> findByReporte_Id(Integer reporteId);
}
