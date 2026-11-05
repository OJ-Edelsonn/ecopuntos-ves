package pe.ecopuntosves.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.ecopuntosves.model.TipoMaterial;

public interface TipoMaterialRepository extends JpaRepository<TipoMaterial, Integer> {
    List<TipoMaterial> findByActivoTrueOrderByNombreAsc();
}
