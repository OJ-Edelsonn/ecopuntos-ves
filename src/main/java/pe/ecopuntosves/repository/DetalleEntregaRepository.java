package pe.ecopuntosves.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.ecopuntosves.model.DetalleEntrega;

public interface DetalleEntregaRepository extends JpaRepository<DetalleEntrega,Integer> {
    List<DetalleEntrega> findByEntrega_IdOrderByIdAsc(Integer idEntrega);
}
