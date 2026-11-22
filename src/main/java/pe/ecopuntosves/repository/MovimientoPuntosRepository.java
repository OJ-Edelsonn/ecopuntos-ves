package pe.ecopuntosves.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.ecopuntosves.model.MovimientoPuntos;

public interface MovimientoPuntosRepository extends JpaRepository<MovimientoPuntos, Integer> {

    @Query("select coalesce(sum(m.puntos), 0L) from MovimientoPuntos m where m.vecino.id = :vecinoId")
    Long saldo(@Param("vecinoId") Integer vecinoId);

    @Query("select coalesce(sum(m.puntos), 0L) from MovimientoPuntos m where m.vecino.id = :vecinoId and m.tipo = :tipo")
    Long totalPorTipo(@Param("vecinoId") Integer vecinoId, @Param("tipo") String tipo);

    Page<MovimientoPuntos> findByVecino_IdOrderByFechaHoraDescIdDesc(Integer vecinoId, Pageable pageable);

    Page<MovimientoPuntos> findByVecino_IdAndTipoOrderByFechaHoraDescIdDesc(Integer vecinoId, String tipo, Pageable pageable);
}