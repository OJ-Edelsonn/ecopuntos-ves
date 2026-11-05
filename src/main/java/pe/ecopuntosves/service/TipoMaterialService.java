package pe.ecopuntosves.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.ecopuntosves.model.TipoMaterial;
import pe.ecopuntosves.repository.TipoMaterialRepository;

@Service
public class TipoMaterialService {

    private final TipoMaterialRepository repository;

    public TipoMaterialService(TipoMaterialRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TipoMaterial> listarActivos() {
        return repository.findByActivoTrueOrderByNombreAsc();
    }
}
