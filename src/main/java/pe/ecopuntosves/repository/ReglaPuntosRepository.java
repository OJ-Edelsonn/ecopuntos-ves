package pe.ecopuntosves.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.ecopuntosves.model.ReglaPuntos;

public interface ReglaPuntosRepository extends JpaRepository<ReglaPuntos, Integer> {

    Optional<ReglaPuntos> findByConceptoAndMaterial_IdAndVigenteTrue(String concepto, Integer idMaterial);

    Optional<ReglaPuntos> findByConceptoAndMaterialIsNullAndVigenteTrue(String concepto);

    List<ReglaPuntos> findAllByOrderByVigenteDescFechaInicioDescIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReglaPuntos r where r.concepto = 'RECICLAJE' and r.material.id = :materialId and r.vigente = true")
    Optional<ReglaPuntos> bloquearReciclajeVigente(@Param("materialId") Integer materialId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReglaPuntos r where r.concepto = :concepto and r.material is null and r.vigente = true")
    Optional<ReglaPuntos> bloquearGlobalVigente(@Param("concepto") String concepto);
}