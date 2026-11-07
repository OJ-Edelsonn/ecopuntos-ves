package pe.ecopuntosves.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.ecopuntosves.model.Cuadrilla;

public interface CuadrillaRepository extends JpaRepository<Cuadrilla,Integer> {
    List<Cuadrilla> findByActivoTrueOrderByNombreAsc();
    List<Cuadrilla> findAllByOrderByNombreAsc();
    boolean existsByNombreIgnoreCase(String nombre);
}
