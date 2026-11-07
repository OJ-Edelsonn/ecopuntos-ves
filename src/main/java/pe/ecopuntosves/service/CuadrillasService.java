package pe.ecopuntosves.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.ecopuntosves.model.*;
import pe.ecopuntosves.repository.*;

@Service
public class CuadrillasService {
    private final CuadrillaRepository cuadrillas;
    private final MiembroCuadrillaRepository miembros;
    private final SectorRepository sectores;
    public CuadrillasService(CuadrillaRepository cuadrillas, MiembroCuadrillaRepository miembros,
                             SectorRepository sectores) {
        this.cuadrillas=cuadrillas;this.miembros=miembros;this.sectores=sectores;
    }
    private String texto(String s,int max,String nombre) {
        String t=s==null?"":s.trim().replaceAll("\\s+"," ");
        if(t.isBlank() || t.length()>max) throw new IllegalArgumentException(nombre+" es obligatorio (max. "+max+" caracteres).");
        return t;
    }
    @Transactional(readOnly=true)
    public List<Cuadrilla> cuadrillas() { return cuadrillas.findAllByOrderByNombreAsc(); }
    @Transactional(readOnly=true)
    public List<Cuadrilla> cuadrillasActivas() { return cuadrillas.findByActivoTrueOrderByNombreAsc(); }
    @Transactional(readOnly=true)
    public List<MiembroCuadrilla> miembros() { return miembros.findAllByOrderByIdAsc(); }
    @Transactional(readOnly=true)
    public List<MiembroCuadrilla> miembrosActivos() { return miembros.findByActivoTrueOrderByApellidosAsc(); }
    @Transactional(readOnly=true)
    public List<Sector> sectores() { return sectores.findAll(Sort.by("nombre")); }
    @Transactional
    public void crearCuadrilla(String nombre, Integer sectorId) {
        String n=texto(nombre,100,"Nombre de cuadrilla");
        if(cuadrillas.existsByNombreIgnoreCase(n)) throw new IllegalArgumentException("Ya existe una cuadrilla con este nombre.");
        Sector s=sectorId==null ? null : sectores.findById(sectorId)
             .orElseThrow(()->new IllegalArgumentException("Sector no encontrado."));
        cuadrillas.save(new Cuadrilla(n,s));
    }
    @Transactional
    public void crearMiembro(Integer cuadrillaId,String nombres,String apellidos,String cargo) {
        if(cuadrillaId==null) throw new IllegalArgumentException("Selecciona una cuadrilla.");
        Cuadrilla c=cuadrillas.findById(cuadrillaId)
             .orElseThrow(()->new IllegalArgumentException("Cuadrilla no encontrada."));
        if(!c.isActivo()) throw new IllegalArgumentException("No puedes agregar personal a una cuadrilla inactiva.");
        String n=texto(nombres,80,"Nombres");String a=texto(apellidos,80,"Apellidos");
        String puesto=texto(cargo,70,"Cargo");
        if(miembros.existsByCuadrilla_IdAndNombresIgnoreCaseAndApellidosIgnoreCase(cuadrillaId,n,a))
            throw new IllegalArgumentException("La persona ya existe en esta cuadrilla.");
        miembros.save(new MiembroCuadrilla(c,n,a,puesto));
    }
    @Transactional
    public void cambiarCuadrilla(int id,boolean activo) {
        Cuadrilla c=cuadrillas.findById(id).orElseThrow(()->new IllegalArgumentException("Cuadrilla inexistente."));
        c.setActivo(activo);
    }
    @Transactional
    public void cambiarMiembro(int id,boolean activo) {
        MiembroCuadrilla m=miembros.findById(id).orElseThrow(()->new IllegalArgumentException("Integrante inexistente."));
        if(activo && !m.getCuadrilla().isActivo())
            throw new IllegalArgumentException("Primero activa la cuadrilla correspondiente.");
        m.setActivo(activo);
    }
}
