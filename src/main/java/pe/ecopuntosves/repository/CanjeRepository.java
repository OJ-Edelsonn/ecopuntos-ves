package pe.ecopuntosves.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.ecopuntosves.model.Canje;

public interface CanjeRepository extends JpaRepository<Canje, Integer> {
    Page<Canje> findByVecino_Id(Integer vecinoId, Pageable pageable);
    Optional<Canje> findByCodigoConstancia(String codigoConstancia);
}
