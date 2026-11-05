package pe.ecopuntosves.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.ecopuntosves.model.Sector;
public interface SectorRepository extends JpaRepository<Sector, Integer> { }
