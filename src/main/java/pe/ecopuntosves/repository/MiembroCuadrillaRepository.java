package pe.ecopuntosves.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.ecopuntosves.model.MiembroCuadrilla;

public interface MiembroCuadrillaRepository extends JpaRepository<MiembroCuadrilla,Integer> {
    List<MiembroCuadrilla> findByActivoTrueOrderByApellidosAsc();
    List<MiembroCuadrilla> findAllByOrderByIdAsc();
    boolean existsByCuadrilla_IdAndNombresIgnoreCaseAndApellidosIgnoreCase(
        Integer idCuadrilla, String nombres, String apellidos);
}
