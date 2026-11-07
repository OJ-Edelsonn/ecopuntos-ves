package pe.ecopuntosves.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.ecopuntosves.model.MovimientoPuntos;

public interface MovimientoPuntosRepository extends JpaRepository<MovimientoPuntos,Integer> {
    @Query("select coalesce(sum(m.puntos), 0L) from MovimientoPuntos m where m.vecino.id = :vecinoId")
    Long saldo(@Param("vecinoId") Integer vecinoId);
}
