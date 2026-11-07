package pe.ecopuntosves.service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import pe.ecopuntosves.model.*;
import pe.ecopuntosves.repository.*;
import pe.ecopuntosves.web.form.ReportePuntoCriticoForm;

@Service
public class PuntosCriticosService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Set<String> ESTADOS = Set.of(
            "REPORTADO", "VALIDADO", "RECHAZADO", "EN_ATENCION", "RESUELTO");

    private final EntityManager em;
    private final UsuarioRepository usuarios;
    private final VecinoRepository vecinos;
    private final SectorRepository sectores;
    private final ReportePuntoCriticoRepository reportes;
    private final AtencionPuntoCriticoRepository atenciones;
    private final FotosPuntosCriticosService fotos;

    public PuntosCriticosService(EntityManager em, UsuarioRepository usuarios,
                                VecinoRepository vecinos, SectorRepository sectores,
                                ReportePuntoCriticoRepository reportes,
                                AtencionPuntoCriticoRepository atenciones,
                                FotosPuntosCriticosService fotos) {
        this.em = em; this.usuarios = usuarios; this.vecinos = vecinos;
        this.sectores = sectores; this.reportes = reportes;
        this.atenciones = atenciones; this.fotos = fotos;
    }

    private LocalDateTime ahora() { return LocalDateTime.now(LIMA); }
    private int pagina(int n) { return Math.max(0, Math.min(n, 10000)); }
    private Usuario actor(String nombre) {
        Usuario u = usuarios.findByUsername(nombre)
                .orElseThrow(() -> new IllegalArgumentException("Sesión sin usuario de base de datos."));
        if (!u.isActivo()) throw new IllegalArgumentException("Cuenta desactivada.");
        return u;
    }
    private Usuario funcionario(String nombre) {
        Usuario u = actor(nombre);
        if (!"FUNCIONARIO".equals(u.getRol().getNombre()))
            throw new SecurityException("Solo un funcionario puede gestionar la atención de reportes.");
        return u;
    }
    private Vecino vecino(String nombre) {
        Usuario u = actor(nombre);
        if (!"VECINO".equals(u.getRol().getNombre()))
            throw new SecurityException("Esta operación corresponde al rol vecino.");
        return vecinos.findByUsuario_Username(nombre)
                .orElseThrow(() -> new IllegalArgumentException("No tienes perfil de vecino."));
    }
    private static String texto(String valor, int maximo, String campo) {
        String limpio = valor == null ? "" : valor.strip().replaceAll("\\s+", " ");
        if (limpio.isBlank() || limpio.length() > maximo)
            throw new IllegalArgumentException(campo + " es obligatorio y admite hasta " + maximo + " caracteres.");
        return limpio;
    }
    private static String normalizar(String direccion) {
        String n = Normalizer.normalize(direccion.toUpperCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Z0-9 ]+", " ")
                .replaceAll("\\b(AV|AVENIDA)\\b", "AVENIDA")
                .replaceAll("\\b(JR|JIRON)\\b", "JIRON")
                .replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        if (n.isBlank() || n.length() > 200)
            throw new IllegalArgumentException("La ubicación normalizada debe tener entre 1 y 200 caracteres.");
        return n;
    }

    @Transactional(readOnly = true)
    public List<Sector> sectores() { return sectores.findAll(Sort.by("nombre")); }

    @Transactional(readOnly = true)
    public Page<ReportePuntoCritico> listaFuncionario(Integer sectorId, String estado, int page, String username) {
        Usuario u = actor(username);
        if (!Set.of("FUNCIONARIO", "ADMINISTRADOR").contains(u.getRol().getNombre()))
            throw new SecurityException("Acceso reservado al personal municipal.");
        String filtro = estado == null || estado.isBlank() ? null : estado.trim().toUpperCase(Locale.ROOT);
        if (filtro != null && !ESTADOS.contains(filtro))
            throw new IllegalArgumentException("Estado de búsqueda inválido.");
        if (sectorId != null && sectorId <= 0) throw new IllegalArgumentException("Sector inválido.");
        return reportes.filtrar(sectorId, filtro,
                PageRequest.of(pagina(page), 15, Sort.by(Sort.Direction.DESC, "fechaHora", "id")));
    }

    @Transactional(readOnly = true)
    public Page<ReportePuntoCritico> misReportes(String username, int page) {
        Vecino v = vecino(username);
        return reportes.findByVecino_Id(v.getId(),
                PageRequest.of(pagina(page), 15, Sort.by(Sort.Direction.DESC, "fechaHora", "id")));
    }

    @Transactional(readOnly = true)
    public ReportePuntoCritico verAutorizado(int id, String username) {
        Usuario u = actor(username);
        ReportePuntoCritico r = reportes.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado."));
        String rol = u.getRol().getNombre();
        if ("VECINO".equals(rol)) {
            if (!r.getVecino().getUsuario().getId().equals(u.getId()))
                throw new SecurityException("No puedes consultar reportes de otro vecino.");
        } else if (!"FUNCIONARIO".equals(rol) && !"ADMINISTRADOR".equals(rol)) {
            throw new SecurityException("No tienes permiso para consultar este reporte.");
        }
        return r;
    }

    @Transactional(readOnly = true)
    public AtencionPuntoCritico obtenerAtencion(int id, String username) {
        verAutorizado(id, username);
        return atenciones.findByReporte_Id(id).orElse(null);
    }

    @Transactional
    public Integer registrar(ReportePuntoCriticoForm f, MultipartFile foto, String username) {
        Vecino v = vecino(username);
        String direccion = texto(f.getDireccion(), 200, "Dirección");
        String descripcion = texto(f.getDescripcion(), 500, "Descripción");
        String residuo = texto(f.getTipoResiduo(), 60, "Tipo de residuo");
        String normalizada = normalizar(direccion);
        if (f.getSectorId() == null) throw new IllegalArgumentException("Selecciona un sector.");
        Sector sector = sectores.findById(f.getSectorId())
                .orElseThrow(() -> new IllegalArgumentException("Sector no encontrado."));
        // Serializa el límite diario por vecino, igual que el registro de entregas/canjes.
        em.lock(v, LockModeType.PESSIMISTIC_WRITE);
        LocalDateTime fecha = ahora();
        LocalDate dia = fecha.toLocalDate();
        if (reportes.contarEnDia(v.getId(), dia.atStartOfDay(), dia.plusDays(1).atStartOfDay()) >= 3)
            throw new IllegalArgumentException("Límite alcanzado: máximo tres reportes por día.");
        ReportePuntoCritico reporte = reportes.save(new ReportePuntoCritico(
                v, sector, direccion, descripcion, residuo, normalizada, fecha));
        // Flush hace que la restricción única de ubicación abierta se compruebe aquí.
        em.flush();
        String nombreFoto = fotos.guardar(foto);
        if (nombreFoto != null) {
            reporte.setFotoUrl(nombreFoto);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED)
                        fotos.borrarSilenciosamente(nombreFoto);
                }
            });
        }
        em.flush();
        return reporte.getId();
    }

    @Transactional
    public void decidir(int id, boolean aprobar, String motivo, String username) {
        Usuario f = funcionario(username);
        ReportePuntoCritico r = reportes.bloquear(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado."));
        if (!"REPORTADO".equals(r.getEstado()))
            throw new IllegalArgumentException("Este reporte ya fue evaluado y no puede volver a validarse.");
        String razon = aprobar ? null : texto(motivo, 300, "Motivo del rechazo");
        if (aprobar) {
            // Bloqueo usado por todos los movimientos de puntos para este vecino.
            em.lock(r.getVecino(), LockModeType.PESSIMISTIC_WRITE);
            List<ReglaPuntos> vigentes = em.createQuery(
                    "select x from ReglaPuntos x where x.concepto = 'REPORTE' and x.vigente = true", ReglaPuntos.class)
                    .setMaxResults(2).getResultList();
            if (vigentes.size() != 1)
                throw new IllegalArgumentException("No existe una única regla REPORTE vigente.");
            ReglaPuntos regla = vigentes.get(0);
            BigDecimal valor = regla.getValor();
            int puntos;
            try { puntos = valor.intValueExact(); } catch (ArithmeticException ex) {
                throw new IllegalArgumentException("La regla REPORTE requiere puntos enteros.");
            }
            if (puntos <= 0) throw new IllegalArgumentException("La regla REPORTE es inválida.");
            r.decidir(true, null, f, ahora());
            em.flush();
            // Inserción en la misma transacción. No requiere reemplazar MovimientoPuntos.java del Módulo 03.
            em.createNativeQuery("INSERT INTO movimiento_puntos " +
                    "(id_vecino, tipo, puntos, fecha_hora, id_reporte, descripcion, id_usuario, id_regla) " +
                    "VALUES (:vecino, 'REPORTE', :puntos, :fecha, :reporte, :descripcion, :usuario, :regla)")
                    .setParameter("vecino", r.getVecino().getId())
                    .setParameter("puntos", puntos)
                    .setParameter("fecha", java.sql.Timestamp.valueOf(ahora()))
                    .setParameter("reporte", id)
                    .setParameter("descripcion", "Reporte de punto crítico validado")
                    .setParameter("usuario", f.getId())
                    .setParameter("regla", regla.getId()).executeUpdate();
        } else {
            r.decidir(false, razon, f, ahora());
        }
        em.flush();
    }

    @Transactional
    public void iniciarAtencion(int id, String responsable, LocalDate fechaProgramada, String username) {
        Usuario f = funcionario(username);
        ReportePuntoCritico r = reportes.bloquear(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado."));
        if (!"VALIDADO".equals(r.getEstado()))
            throw new IllegalArgumentException("Solo pueden asignarse reportes VALIDADOS.");
        String nombre = texto(responsable, 100, "Responsable");
        if (fechaProgramada == null || fechaProgramada.isBefore(LocalDate.now(LIMA)))
            throw new IllegalArgumentException("La fecha programada no puede ser anterior a hoy.");
        if (atenciones.findByReporte_Id(id).isPresent())
            throw new IllegalArgumentException("El reporte ya tiene una atención asignada.");
        atenciones.save(new AtencionPuntoCritico(r, nombre, fechaProgramada, f));
        r.iniciarAtencion(ahora());
        em.flush();
    }

    @Transactional
    public void resolver(int id, String observacion, String username) {
        Usuario f = funcionario(username);
        ReportePuntoCritico r = reportes.bloquear(id)
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado."));
        if (!"EN_ATENCION".equals(r.getEstado()))
            throw new IllegalArgumentException("Solo pueden resolverse reportes EN_ATENCION.");
        AtencionPuntoCritico a = atenciones.findByReporte_Id(id)
                .orElseThrow(() -> new IllegalArgumentException("Falta la atención asignada."));
        a.resolver(ahora(), texto(observacion, 300, "Observación de resolución"), f);
        r.resolver();
        em.flush();
    }
}
