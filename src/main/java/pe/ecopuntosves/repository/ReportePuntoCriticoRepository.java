package pe.ecopuntosves.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.ecopuntosves.model.ReportePuntoCritico;

public interface ReportePuntoCriticoRepository extends JpaRepository<ReportePuntoCritico, Integer> {

    Page<ReportePuntoCritico> findByVecino_Id(Integer idVecino, Pageable pageable);

    @Query("select r from ReportePuntoCritico r where " +
           "(:sectorId is null or r.sector.id = :sectorId) " +
           "and (:estado is null or r.estado = :estado) " +
           "and (:inicio is null or r.fechaHora >= :inicio) " +
           "and (:fin is null or r.fechaHora < :fin)")
    Page<ReportePuntoCritico> filtrar(@Param("sectorId") Integer sectorId,
                                      @Param("estado") String estado,
                                      @Param("inicio") LocalDateTime inicio,
                                      @Param("fin") LocalDateTime fin,
                                      Pageable pageable);

    @Query("select r.estado, count(r) from ReportePuntoCritico r where " +
           "(:sectorId is null or r.sector.id = :sectorId) " +
           "and (:estado is null or r.estado = :estado) " +
           "and (:inicio is null or r.fechaHora >= :inicio) " +
           "and (:fin is null or r.fechaHora < :fin) " +
           "group by r.estado")
    List<Object[]> contarPorEstado(@Param("sectorId") Integer sectorId,
                                   @Param("estado") String estado,
                                   @Param("inicio") LocalDateTime inicio,
                                   @Param("fin") LocalDateTime fin);

    @Query("select count(r) from ReportePuntoCritico r where r.vecino.id = :id " +
           "and r.fechaHora >= :inicio and r.fechaHora < :fin")
    long contarEnDia(@Param("id") Integer id, @Param("inicio") LocalDateTime inicio,
                     @Param("fin") LocalDateTime fin);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReportePuntoCritico r where r.id = :id")
    Optional<ReportePuntoCritico> bloquear(@Param("id") Integer id);
}