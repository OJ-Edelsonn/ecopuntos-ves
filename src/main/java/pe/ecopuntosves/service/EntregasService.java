package pe.ecopuntosves.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.ecopuntosves.model.*;
import pe.ecopuntosves.repository.*;
import pe.ecopuntosves.web.form.EntregaForm;

@Service
public class EntregasService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final EntityManager em;
    private final UsuarioRepository usuarios;
    private final TipoMaterialRepository materiales;
    private final ReglaPuntosRepository reglas;
    private final EntregaRepository entregas;
    private final DetalleEntregaRepository detalles;
    private final MovimientoPuntosRepository movimientos;
    private final VecinoRepository vecinos;

    public EntregasService(EntityManager em, UsuarioRepository usuarios,
        TipoMaterialRepository materiales, ReglaPuntosRepository reglas,
        EntregaRepository entregas, DetalleEntregaRepository detalles,
        MovimientoPuntosRepository movimientos, VecinoRepository vecinos) {
        this.em=em; this.usuarios=usuarios; this.materiales=materiales;
        this.reglas=reglas; this.entregas=entregas;
        this.detalles=detalles; this.movimientos=movimientos; this.vecinos=vecinos;
    }

    @Transactional(readOnly = true)
    public List<TipoMaterial> materialesActivos() {
        return materiales.findByActivoTrueOrderByNombreAsc();
    }
    @Transactional(readOnly = true)
    public Page<Entrega> listar(String dni, int page, String orden) {
        // Por defecto: numero de entrega ascendente. Alternativa: mas recientes primero.
        Sort sort = "recientes".equals(orden)
                ? Sort.by(Direction.DESC, "fechaHora").and(Sort.by(Direction.DESC, "id"))
                : Sort.by(Direction.ASC, "id");
        Pageable p=PageRequest.of(Math.max(0,Math.min(10000,page)),15,sort);
        String filtro=dni==null?"":dni.trim();
        return filtro.isEmpty()?entregas.findAll(p)
                :entregas.findByVecino_DniContaining(filtro,p);
    }
    @Transactional(readOnly = true)
    public Entrega obtener(int id) {
        return entregas.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("No existe la entrega solicitada."));
    }
    @Transactional(readOnly = true)
    public List<DetalleEntrega> detalle(int id) {
        obtener(id);
        return detalles.findByEntrega_IdOrderByIdAsc(id);
    }
    @Transactional(readOnly = true)
    public Page<Entrega> misEntregas(String usuario, int page) {
        Vecino v=vecinos.findByUsuario_Username(usuario)
            .orElseThrow(() -> new IllegalArgumentException("Este usuario no tiene perfil de vecino."));
        return entregas.findByVecino_IdOrderByFechaHoraDesc(v.getId(),
            PageRequest.of(Math.max(0,Math.min(10000,page)),15));
    }
    @Transactional(readOnly = true)
    public long miSaldo(String usuario) {
        Vecino v=vecinos.findByUsuario_Username(usuario)
            .orElseThrow(() -> new IllegalArgumentException("Este usuario no tiene perfil de vecino."));
        return saldo(v.getId());
    }
    @Transactional(readOnly = true)
    public long saldo(Integer idVecino) {
        Long s=movimientos.saldo(idVecino);
        return s==null?0L:s;
    }

    @Transactional
    public Integer registrar(EntregaForm form, String username) {
        if(form==null || form.getDniVecino()==null
            || !form.getDniVecino().trim().matches("[0-9]{8}"))
            throw new IllegalArgumentException("Escribe un DNI de 8 dígitos.");
        List<Integer> ids=form.getMaterialIds();
        List<BigDecimal> pesos=form.getKilos();
        if(ids==null || pesos==null || ids.isEmpty() || ids.size()!=pesos.size() || ids.size()>50)
            throw new IllegalArgumentException("Formulario de materiales inválido.");
        Usuario operador=usuarios.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Usuario operador no encontrado."));
        if(!operador.isActivo() || !("OPERADOR".equals(operador.getRol().getNombre())
                || "ADMINISTRADOR".equals(operador.getRol().getNombre())))
            throw new IllegalArgumentException("No tienes permiso para registrar entregas.");

        // Un mismo vecino se bloquea durante cualquier movimiento de puntos.
        // Se utiliza la misma fila bloqueada para impedir carreras al canjear.
        List<Vecino> encontrados=em.createQuery(
            "select v from Vecino v where v.dni = :dni",Vecino.class)
            .setParameter("dni",form.getDniVecino().trim())
            .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        if(encontrados.isEmpty()) throw new IllegalArgumentException("DNI no registrado. Crea primero el vecino.");
        Vecino vecino=encontrados.get(0);
        if(!vecino.getUsuario().isActivo())
            throw new IllegalArgumentException("La cuenta del vecino está desactivada.");

        // Un item por material; omite entradas vacías (sin kilos).
        record Item(TipoMaterial material,BigDecimal peso,int puntos,ReglaPuntos regla) {}
        List<Item> items=new ArrayList<>();
        Set<Integer> usados=new HashSet<>();
        BigDecimal kilosTotal=BigDecimal.ZERO;
        int puntosTotal=0;
        for(int i=0;i<ids.size();i++) {
            Integer id=ids.get(i); BigDecimal kilos=pesos.get(i);
            if(id==null) throw new IllegalArgumentException("Hay un material sin identificar.");
            if(!usados.add(id)) throw new IllegalArgumentException("Hay materiales repetidos.");
            if(kilos==null) continue;
            if(kilos.signum()<=0 || kilos.scale()>2 || kilos.compareTo(new BigDecimal("99999.99"))>0)
                throw new IllegalArgumentException("Los kilos deben ser positivos, con hasta 2 decimales.");
            TipoMaterial m=materiales.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Material no encontrado."));
            if(!m.isActivo()) throw new IllegalArgumentException("Material inactivo: "+m.getNombre());
            ReglaPuntos regla=reglas.findByConceptoAndMaterial_IdAndVigenteTrue("RECICLAJE",id)
                .orElseThrow(() -> new IllegalArgumentException("Sin regla vigente para "+m.getNombre()));
            int puntos;
            try {
                puntos=kilos.multiply(regla.getValor()).setScale(0,RoundingMode.HALF_UP).intValueExact();
                puntosTotal=Math.addExact(puntosTotal,puntos);
            } catch(ArithmeticException ex) {
                throw new IllegalArgumentException("La cantidad supera el máximo permitido.");
            }
            kilosTotal=kilosTotal.add(kilos);
            if(kilosTotal.compareTo(new BigDecimal("99999999.99"))>0)
                throw new IllegalArgumentException("Peso total demasiado alto.");
            items.add(new Item(m,kilos,puntos,regla));
        }
        if(items.isEmpty()) throw new IllegalArgumentException("Ingresa kilos para al menos un material.");
        LocalDateTime fecha=LocalDateTime.now(LIMA);
        Entrega entrega=entregas.save(new Entrega(vecino,operador,fecha,kilosTotal.setScale(2),puntosTotal));
        for(Item i:items)
            detalles.save(new DetalleEntrega(entrega,i.material(),i.peso(),i.puntos(),i.regla()));
        movimientos.save(new MovimientoPuntos(entrega,vecino,operador,puntosTotal,fecha));
        // Valida las restricciones SQL en la misma transaccion.
        em.flush();
        return entrega.getId();
    }
}
