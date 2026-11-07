package pe.ecopuntosves.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.ecopuntosves.model.ReglaPuntos;

public interface ReglaPuntosRepository extends JpaRepository<ReglaPuntos,Integer> {
    Optional<ReglaPuntos> findByConceptoAndMaterial_IdAndVigenteTrue(String concepto, Integer idMaterial);
}
