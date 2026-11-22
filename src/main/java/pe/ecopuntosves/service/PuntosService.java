package pe.ecopuntosves.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.ecopuntosves.model.MovimientoPuntos;
import pe.ecopuntosves.model.Vecino;
import pe.ecopuntosves.repository.MovimientoPuntosRepository;
import pe.ecopuntosves.repository.VecinoRepository;

@Service
public class PuntosService {

    private static final Set<String> TIPOS = Set.of("ENTREGA", "REPORTE", "CANJE", "REVERSO");

    private final VecinoRepository vecinos;
    private final MovimientoPuntosRepository movimientos;

    public PuntosService(VecinoRepository vecinos, MovimientoPuntosRepository movimientos) {
        this.vecinos = vecinos;
        this.movimientos = movimientos;
    }

    public record MovimientoItem(
            Integer id,
            LocalDateTime fechaHora,
            String tipo,
            Integer puntos,
            String descripcion,
            Integer idEntrega,
            Integer idReporte,
            Integer idCanje) {}

    public record ResumenPuntos(
            Vecino vecino,
            long saldo,
            long puntosEntregas,
            long puntosReportes,
            long puntosCanjeados,
            long puntosReversos,
            String tipoFiltro,
            Page<MovimientoItem> movimientos) {}

    private Vecino vecinoAutenticado(String username) {
        Vecino v = vecinos.findByUsuario_Username(username)
                .orElseThrow(() -> new SecurityException("Esta cuenta no tiene un perfil de vecino."));
        if (!v.getUsuario().isActivo()) {
            throw new SecurityException("Cuenta desactivada.");
        }
        return v;
    }

    private long seguro(Long valor) {
        return valor == null ? 0L : valor;
    }

    @Transactional(readOnly = true)
    public long saldoDeUsuario(String username) {
        Vecino v = vecinoAutenticado(username);
        return seguro(movimientos.saldo(v.getId()));
    }

    @Transactional(readOnly = true)
    public ResumenPuntos resumen(String username, String tipo, int page) {
        Vecino v = vecinoAutenticado(username);
        String filtro = tipo == null ? "" : tipo.trim().toUpperCase(Locale.ROOT);
        if (!TIPOS.contains(filtro)) {
            filtro = "";
        }

        Pageable pageable = PageRequest.of(Math.max(0, Math.min(page, 10000)), 15);
        Page<MovimientoPuntos> pagina = filtro.isEmpty()
                ? movimientos.findByVecino_IdOrderByFechaHoraDescIdDesc(v.getId(), pageable)
                : movimientos.findByVecino_IdAndTipoOrderByFechaHoraDescIdDesc(v.getId(), filtro, pageable);

        Page<MovimientoItem> items = pagina.map(m -> new MovimientoItem(
                m.getId(),
                m.getFechaHora(),
                m.getTipo(),
                m.getPuntos(),
                m.getDescripcion(),
                m.getIdEntrega(),
                m.getIdReporte(),
                m.getIdCanje()
        ));

        long saldo = seguro(movimientos.saldo(v.getId()));
        long porEntregas = seguro(movimientos.totalPorTipo(v.getId(), "ENTREGA"));
        long porReportes = seguro(movimientos.totalPorTipo(v.getId(), "REPORTE"));
        long canjeados = Math.abs(seguro(movimientos.totalPorTipo(v.getId(), "CANJE")));
        long reversos = seguro(movimientos.totalPorTipo(v.getId(), "REVERSO"));

        return new ResumenPuntos(v, saldo, porEntregas, porReportes, canjeados, reversos, filtro, items);
    }
}