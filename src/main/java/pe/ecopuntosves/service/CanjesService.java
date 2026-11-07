package pe.ecopuntosves.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.ecopuntosves.model.*;
import pe.ecopuntosves.repository.*;

@Service
public class CanjesService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private final EntityManager em;
    private final CanjeRepository canjes;
    private final VecinoRepository vecinos;
    private final UsuarioRepository usuarios;
    private final MovimientoPuntosRepository movimientos;

    public CanjesService(EntityManager em, CanjeRepository canjes, VecinoRepository vecinos,
                         UsuarioRepository usuarios, MovimientoPuntosRepository movimientos) {
        this.em = em; this.canjes = canjes; this.vecinos = vecinos;
        this.usuarios = usuarios; this.movimientos = movimientos;
    }

    private LocalDateTime ahora() { return LocalDateTime.now(LIMA); }

    private Usuario actor(String username) {
        Usuario usuario = usuarios.findByUsername(username)
                .orElseThrow(() -> new SecurityException("No existe el usuario autenticado."));
        if (!usuario.isActivo()) throw new SecurityException("Cuenta desactivada.");
        return usuario;
    }

    private Usuario funcionario(String username) {
        Usuario u = actor(username);
        if (!"FUNCIONARIO".equals(u.getRol().getNombre()))
            throw new SecurityException("Esta operación corresponde al funcionario municipal.");
        return u;
    }

    private Vecino vecino(String username) {
        Usuario u = actor(username);
        if (!"VECINO".equals(u.getRol().getNombre()))
            throw new SecurityException("Solo un vecino puede emitir su propio canje.");
        return vecinos.findByUsuario_Username(username)
                .orElseThrow(() -> new IllegalArgumentException("No tienes un perfil de vecino."));
    }

    private int equivalenciaActual() {
        List<ReglaPuntos> reglas = em.createQuery(
                "select r from ReglaPuntos r where r.concepto = 'CANJE' and r.vigente = true", ReglaPuntos.class)
                .setMaxResults(2).getResultList();
        if (reglas.size() != 1) throw new IllegalArgumentException("Debe existir una única regla CANJE vigente.");
        try {
            int valor = reglas.get(0).getValor().intValueExact();
            if (valor <= 0) throw new ArithmeticException();
            return valor;
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("La equivalencia vigente debe ser un entero positivo.");
        }
    }

    private ReglaPuntos reglaActual() {
        List<ReglaPuntos> reglas = em.createQuery(
                "select r from ReglaPuntos r where r.concepto = 'CANJE' and r.vigente = true", ReglaPuntos.class)
                .setMaxResults(2).getResultList();
        if (reglas.size() != 1) throw new IllegalArgumentException("Debe existir una única regla CANJE vigente.");
        equivalenciaActual();
        return reglas.get(0);
    }

    private long saldo(Integer idVecino) {
        Long total = movimientos.saldo(idVecino);
        return total == null ? 0L : total;
    }

    private static String codigoUnico() {
        byte[] bytes = new byte[12];
        ALEATORIO.nextBytes(bytes);
        return "ECV-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Transactional(readOnly = true)
    public ResumenVecino resumen(String username, int page) {
        Vecino v = vecino(username);
        return new ResumenVecino(v, saldo(v.getId()), equivalenciaActual(),
                canjes.findByVecino_Id(v.getId(), PageRequest.of(Math.max(0, Math.min(page, 10000)),
                        10, Sort.by(Sort.Direction.DESC, "fechaHora", "id"))));
    }

    public record ResumenVecino(Vecino vecino, long saldo, int equivalencia, Page<Canje> historial) {}

    @Transactional
    public Integer emitir(String username, Integer puntosSolicitados) {
        Vecino existente = vecino(username);
        if (puntosSolicitados == null || puntosSolicitados <= 0)
            throw new IllegalArgumentException("Ingresa un número entero positivo de puntos.");
        // La fila de vecino se bloquea ANTES de consultar el saldo; compartido con entregas y reportes.
        Vecino v = em.find(Vecino.class, existente.getId(), LockModeType.PESSIMISTIC_WRITE);
        if (!v.getUsuario().isActivo())
            throw new IllegalArgumentException("Tu cuenta se encuentra desactivada.");
        String contribuyente = v.getCodigoContribuyente() == null ? "" : v.getCodigoContribuyente().trim();
        if (contribuyente.isBlank())
            throw new IllegalArgumentException("Tu perfil no tiene código de contribuyente de prueba. Solicita registrarlo antes de canjear.");
        ReglaPuntos regla = reglaActual();
        int equivalente;
        try { equivalente = regla.getValor().intValueExact(); }
        catch (ArithmeticException ex) { throw new IllegalArgumentException("Regla CANJE inválida."); }
        if (equivalente <= 0 || puntosSolicitados % equivalente != 0)
            throw new IllegalArgumentException("Los puntos deben ser múltiplos de " + equivalente + ".");
        if (saldo(v.getId()) < puntosSolicitados)
            throw new IllegalArgumentException("Saldo insuficiente para realizar el canje.");

        BigDecimal descuento = BigDecimal.valueOf(puntosSolicitados)
                .divide(BigDecimal.valueOf(equivalente), 2, RoundingMode.UNNECESSARY);
        LocalDateTime fecha = ahora();
        Canje c = canjes.save(new Canje(v, puntosSolicitados, descuento, fecha,
                codigoUnico(), regla, equivalente, contribuyente));
        em.flush();
        em.createNativeQuery("INSERT INTO movimiento_puntos " +
                "(id_vecino, tipo, puntos, fecha_hora, id_canje, descripcion, id_usuario) " +
                "VALUES (:vecino, 'CANJE', :puntos, :fecha, :canje, :descripcion, :usuario)")
                .setParameter("vecino", v.getId())
                .setParameter("puntos", -puntosSolicitados)
                .setParameter("fecha", java.sql.Timestamp.valueOf(fecha))
                .setParameter("canje", c.getId())
                .setParameter("descripcion", "Canje simulado de puntos por descuento de arbitrios")
                .setParameter("usuario", v.getUsuario().getId())
                .executeUpdate();
        em.flush();
        return c.getId();
    }

    @Transactional(readOnly = true)
    public Canje detalleAutorizado(int id, String username) {
        Usuario u = actor(username);
        Canje c = canjes.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Constancia no encontrada."));
        String rol = u.getRol().getNombre();
        if ("VECINO".equals(rol)) {
            if (!c.getVecino().getUsuario().getId().equals(u.getId()))
                throw new SecurityException("Solo puedes consultar tus propios canjes.");
        } else if (!"FUNCIONARIO".equals(rol)) {
            throw new SecurityException("No tienes autorización para consultar esta constancia.");
        }
        return c;
    }

    @Transactional(readOnly = true)
    public boolean esFuncionario(String username) {
        return "FUNCIONARIO".equals(actor(username).getRol().getNombre());
    }

    @Transactional(readOnly = true)
    public Canje buscarFuncionario(String username, String codigo) {
        funcionario(username);
        if (codigo == null || codigo.isBlank()) return null;
        String limpio = codigo.strip().toUpperCase(java.util.Locale.ROOT);
        if (limpio.length() > 20) throw new IllegalArgumentException("Código demasiado largo.");
        return canjes.findByCodigoConstancia(limpio).orElse(null);
    }

    @Transactional
    public void anular(int id, String motivo, String username) {
        Usuario f = funcionario(username);
        String razon = motivo == null ? "" : motivo.strip().replaceAll("\\s+", " ");
        if (razon.isBlank() || razon.length() > 300)
            throw new IllegalArgumentException("Indica el motivo de anulación (máximo 300 caracteres).");
        Canje c = canjes.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Constancia no encontrada."));
        // Siempre bloquear primero al vecino, igual que el resto de movimientos de puntos.
        em.find(Vecino.class, c.getVecino().getId(), LockModeType.PESSIMISTIC_WRITE);
        // Releer el canje bajo bloqueo para impedir doble anulación en peticiones simultáneas.
        em.refresh(c, LockModeType.PESSIMISTIC_WRITE);
        if (!"EMITIDO".equals(c.getEstado()))
            throw new IllegalArgumentException("Esta constancia ya fue anulada.");
        LocalDateTime fecha = ahora();
        c.anular(f, fecha, razon);
        em.flush();
        em.createNativeQuery("INSERT INTO movimiento_puntos " +
                "(id_vecino, tipo, puntos, fecha_hora, id_canje, descripcion, id_usuario) " +
                "VALUES (:vecino, 'REVERSO', :puntos, :fecha, :canje, :descripcion, :usuario)")
                .setParameter("vecino", c.getVecino().getId())
                .setParameter("puntos", c.getPuntosCanjeados())
                .setParameter("fecha", java.sql.Timestamp.valueOf(fecha))
                .setParameter("canje", c.getId())
                .setParameter("descripcion", "Reverso por anulación de canje simulado")
                .setParameter("usuario", f.getId())
                .executeUpdate();
        em.flush();
    }
}
