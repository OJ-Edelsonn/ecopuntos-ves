package pe.ecopuntosves.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.ecopuntosves.model.Entrega;

public interface EntregaRepository extends JpaRepository<Entrega,Integer> {
    Page<Entrega> findByOrderByFechaHoraDesc(Pageable pagina);
    Page<Entrega> findByVecino_DniContaining(String dni, Pageable pagina);
    Page<Entrega> findByVecino_DniContainingOrderByFechaHoraDesc(String dni, Pageable pagina);
    Page<Entrega> findByVecino_IdOrderByFechaHoraDesc(Integer idVecino, Pageable pagina);
}
