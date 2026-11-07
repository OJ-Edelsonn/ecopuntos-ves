package pe.ecopuntosves.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consultas agregadas de solo lectura sobre las doce tablas existentes. */
@Service
public class ReportesEstadisticasService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Set<String> ESTADOS = Set.of(
            "REPORTADO", "VALIDADO", "RECHAZADO", "EN_ATENCION", "RESUELTO");
    private static final List<String> ORDEN_ESTADOS = List.of(
            "REPORTADO", "VALIDADO", "RECHAZADO", "EN_ATENCION", "RESUELTO");
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);

    private final NamedParameterJdbcTemplate jdbc;
    public ReportesEstadisticasService(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Filtro(LocalDate desde, LocalDate hasta, int sectorId, int materialId, String estado) {}
    public record Opcion(int id, String nombre) {}
    public record MaterialDato(String nombre, BigDecimal kilos, long entregas, long puntos, int porcentaje) {}
    public record SectorKilos(String nombre, BigDecimal kilos, long entregas, int porcentaje) {}
    public record EstadoDato(String nombre, long cantidad, int porcentaje) {}
    public record SectorCasos(String nombre, long cantidad, int porcentaje) {}

    public record Acopio(BigDecimal kilos, long entregas,
                         List<MaterialDato> materiales, List<SectorKilos> sectores) {}
    public record Incidencias(long reportes, long resueltos, BigDecimal horasPromedio,
                              long reincidentes, long baseReincidencia, BigDecimal tasaReincidencia,
                              List<EstadoDato> estados, List<SectorCasos> sectores) {}
    public record Incentivos(long puntosEntrega, long puntosReporte, long reversos,
                             long puntosCanjeados, BigDecimal descuentoEmitido,
                             BigDecimal descuentoAnulado, BigDecimal descuentoNetoCohorte,
                             BigDecimal descuentoNetoPorKilo, BigDecimal kilosBase) {}
    public record Datos(Filtro filtro, Acopio acopio, Incidencias incidencias, Incentivos incentivos) {}

    /** El período es inclusivo en fechas: [desde 00:00, día siguiente de hasta 00:00). */
    public Filtro filtro(String desde, String hasta, Integer sectorId, Integer materialId, String estado) {
        LocalDate hoy = LocalDate.now(LIMA);
        LocalDate inicio = leerFecha(desde, hoy.minusDays(89));
        LocalDate fin = leerFecha(hasta, hoy);
        if (inicio.isAfter(fin))
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la final.");
        if (fin.equals(LocalDate.MAX))
            throw new IllegalArgumentException("La fecha final no es válida.");
        int sector = sectorId == null ? 0 : sectorId;
        int material = materialId == null ? 0 : materialId;
        if (sector < 0 || material < 0)
            throw new IllegalArgumentException("Selecciona un sector y material válidos.");
        String estadoNormalizado = estado == null ? "" : estado.strip().toUpperCase(java.util.Locale.ROOT);
        if (!estadoNormalizado.isEmpty() && !ESTADOS.contains(estadoNormalizado))
            throw new IllegalArgumentException("El estado seleccionado no es válido.");
        return new Filtro(inicio, fin, sector, material, estadoNormalizado);
    }

    private LocalDate leerFecha(String valor, LocalDate defecto) {
        if (valor == null || valor.isBlank()) return defecto;
        try { return LocalDate.parse(valor.strip()); }
        catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Utiliza fechas válidas (AAAA-MM-DD).");
        }
    }

    private MapSqlParameterSource parametros(Filtro f) {
        return new MapSqlParameterSource()
                .addValue("inicio", Timestamp.valueOf(f.desde().atStartOfDay()))
                .addValue("fin", Timestamp.valueOf(f.hasta().plusDays(1).atStartOfDay()))
                .addValue("sector", f.sectorId())
                .addValue("material", f.materialId())
                .addValue("estado", f.estado());
    }
    private static BigDecimal decimal(BigDecimal x) { return x == null ? CERO : x; }
    private static BigDecimal porcentaje(long cantidad, long total) {
        return total == 0 ? null : BigDecimal.valueOf(cantidad).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
    }
    private static int ancho(BigDecimal cantidad, BigDecimal maximo) {
        if (maximo == null || maximo.signum() == 0) return 0;
        int n = cantidad.multiply(BigDecimal.valueOf(100))
                .divide(maximo, 0, RoundingMode.HALF_UP).intValue();
        return Math.min(100, Math.max(n, 0));
    }
    private static int ancho(long cantidad, long maximo) {
        return maximo == 0 ? 0 : Math.min(100,
                (int) Math.round((double) cantidad * 100 / maximo));
    }

    public List<Opcion> sectores() {
        return jdbc.getJdbcTemplate().query("SELECT id_sector, nombre FROM sector ORDER BY nombre",
                (rs, n) -> new Opcion(rs.getInt(1), rs.getString(2)));
    }
    public List<Opcion> materiales() {
        return jdbc.getJdbcTemplate().query(
                "SELECT id_tipo_material, nombre FROM tipo_material ORDER BY nombre",
                (rs, n) -> new Opcion(rs.getInt(1), rs.getString(2)));
    }

    @Transactional(readOnly = true)
    public Datos consultar(Filtro f) {
        MapSqlParameterSource p = parametros(f);
        return new Datos(f, acopio(p), incidencias(p), incentivos(p));
    }

    private Acopio acopio(MapSqlParameterSource p) {
        String base = """
             FROM detalle_entrega d
             JOIN entrega e ON e.id_entrega = d.id_entrega
             WHERE e.fecha_hora >= :inicio AND e.fecha_hora < :fin
               AND (:sector = 0 OR e.id_sector = :sector)
               AND (:material = 0 OR d.id_tipo_material = :material)
             """;
        var totales = jdbc.queryForMap("SELECT COALESCE(SUM(d.kilos),0) AS kilos, " +
                "COUNT(DISTINCT e.id_entrega) AS entregas " + base, p);
        BigDecimal kilos = decimal((BigDecimal) totales.get("kilos"));
        long entregas = ((Number) totales.get("entregas")).longValue();

        String sqlMaterial = """
             SELECT m.nombre, SUM(d.kilos) AS kilos,
                    COUNT(DISTINCT e.id_entrega) AS entregas, SUM(d.puntos) AS puntos
             FROM detalle_entrega d
             JOIN entrega e ON e.id_entrega = d.id_entrega
             JOIN tipo_material m ON m.id_tipo_material = d.id_tipo_material
             WHERE e.fecha_hora >= :inicio AND e.fecha_hora < :fin
               AND (:sector = 0 OR e.id_sector = :sector)
               AND (:material = 0 OR d.id_tipo_material = :material)
             GROUP BY m.id_tipo_material, m.nombre
             ORDER BY kilos DESC, m.nombre
             """;
        var filasMateriales = jdbc.query(sqlMaterial, p, (rs, n) ->
                new MaterialDato(rs.getString("nombre"), decimal(rs.getBigDecimal("kilos")),
                        rs.getLong("entregas"), rs.getLong("puntos"), 0));
        BigDecimal maxMaterial = filasMateriales.stream().map(MaterialDato::kilos)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        List<MaterialDato> materiales = filasMateriales.stream().map(m ->
                new MaterialDato(m.nombre(), m.kilos(), m.entregas(), m.puntos(),
                        ancho(m.kilos(), maxMaterial))).toList();

        String sqlSector = """
             SELECT s.nombre, SUM(d.kilos) AS kilos,
                    COUNT(DISTINCT e.id_entrega) AS entregas
             FROM detalle_entrega d
             JOIN entrega e ON e.id_entrega = d.id_entrega
             JOIN sector s ON s.id_sector = e.id_sector
             WHERE e.fecha_hora >= :inicio AND e.fecha_hora < :fin
               AND (:sector = 0 OR e.id_sector = :sector)
               AND (:material = 0 OR d.id_tipo_material = :material)
             GROUP BY s.id_sector, s.nombre
             ORDER BY kilos DESC, s.nombre
             """;
        var filasSectores = jdbc.query(sqlSector, p, (rs, n) ->
                new SectorKilos(rs.getString("nombre"), decimal(rs.getBigDecimal("kilos")),
                        rs.getLong("entregas"), 0));
        BigDecimal maxSector = filasSectores.stream().map(SectorKilos::kilos)
                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        List<SectorKilos> sectores = filasSectores.stream().map(s ->
                new SectorKilos(s.nombre(), s.kilos(), s.entregas(), ancho(s.kilos(), maxSector))).toList();
        return new Acopio(kilos, entregas, materiales, sectores);
    }

    private Incidencias incidencias(MapSqlParameterSource p) {
        String base = """
             FROM reporte_punto_critico r
             WHERE r.fecha_hora >= :inicio AND r.fecha_hora < :fin
               AND (:sector = 0 OR r.id_sector = :sector)
               AND (:estado = '' OR r.estado = :estado)
             """;
        var estadosSql = jdbc.query("SELECT r.estado, COUNT(*) AS cantidad " + base +
                " GROUP BY r.estado", p, (rs, n) -> new EstadoDato(
                        rs.getString("estado"), rs.getLong("cantidad"), 0));
        Map<String, Long> conteos = new LinkedHashMap<>();
        ORDEN_ESTADOS.forEach(estado -> conteos.put(estado, 0L));
        estadosSql.forEach(e -> conteos.put(e.nombre(), e.cantidad()));
        long total = conteos.values().stream().mapToLong(Long::longValue).sum();
        long resueltos = conteos.get("RESUELTO");
        List<EstadoDato> estados = new ArrayList<>();
        conteos.forEach((nombre, cantidad) -> estados.add(new EstadoDato(
                nombre, cantidad, ancho(cantidad, total))));

        String sqlSectores = """
             SELECT s.nombre, COUNT(*) AS cantidad
             FROM reporte_punto_critico r
             JOIN sector s ON s.id_sector = r.id_sector
             WHERE r.fecha_hora >= :inicio AND r.fecha_hora < :fin
               AND (:sector = 0 OR r.id_sector = :sector)
               AND (:estado = '' OR r.estado = :estado)
             GROUP BY s.id_sector, s.nombre ORDER BY cantidad DESC, s.nombre
             """;
        var filasSectores = jdbc.query(sqlSectores, p, (rs,n) ->
                new SectorCasos(rs.getString("nombre"), rs.getLong("cantidad"), 0));
        long maxSector = filasSectores.stream().mapToLong(SectorCasos::cantidad).max().orElse(0);
        List<SectorCasos> sectores = filasSectores.stream().map(s ->
                new SectorCasos(s.nombre(), s.cantidad(), ancho(s.cantidad(),maxSector))).toList();

        // El tiempo medio incluye atenciones RESUELTAS durante el período, aun si el reporte se creó antes.
        String sqlTiempo = """
             SELECT AVG(TIMESTAMPDIFF(MINUTE, r.fecha_hora, a.fecha_resolucion) / 60.0)
             FROM atencion_punto_critico a
             JOIN reporte_punto_critico r ON r.id_reporte = a.id_reporte
             WHERE a.fecha_resolucion >= :inicio AND a.fecha_resolucion < :fin
               AND r.estado = 'RESUELTO'
               AND (:sector = 0 OR r.id_sector = :sector)
               AND (:estado = '' OR r.estado = :estado)
             """;
        BigDecimal horas = jdbc.queryForObject(sqlTiempo, p, (rs, n) -> rs.getBigDecimal(1));
        if (horas != null) horas = horas.setScale(2, RoundingMode.HALF_UP);

        String sqlReincidencia = """
             SELECT COUNT(*) AS base,
                    COALESCE(SUM(CASE WHEN EXISTS (
                       SELECT 1 FROM reporte_punto_critico anterior
                       JOIN atencion_punto_critico a ON a.id_reporte = anterior.id_reporte
                       WHERE anterior.id_sector = r.id_sector
                         AND anterior.ubicacion_normalizada = r.ubicacion_normalizada
                         AND anterior.id_reporte <> r.id_reporte
                         AND anterior.estado = 'RESUELTO'
                         AND a.fecha_resolucion < r.fecha_hora
                         AND a.fecha_resolucion >= r.fecha_hora - INTERVAL 90 DAY
                    ) THEN 1 ELSE 0 END),0) AS reincidentes
             FROM reporte_punto_critico r
             WHERE r.fecha_hora >= :inicio AND r.fecha_hora < :fin
               AND r.estado <> 'RECHAZADO'
               AND (:sector = 0 OR r.id_sector = :sector)
               AND (:estado = '' OR r.estado = :estado)
             """;
        var recid = jdbc.queryForMap(sqlReincidencia, p);
        long baseReincidencia = ((Number) recid.get("base")).longValue();
        long reincidentes = ((Number) recid.get("reincidentes")).longValue();
        return new Incidencias(total, resueltos, horas, reincidentes,
                baseReincidencia, porcentaje(reincidentes, baseReincidencia), estados, sectores);
    }

    private Incentivos incentivos(MapSqlParameterSource p) {
        // Para los movimientos de entrega/reporte se usa el sector histórico; para canjes, el sector actual.
        // No se aplica el filtro de material ni de estado a los incentivos.
        String sqlMovimientos = """
             SELECT COALESCE(SUM(CASE WHEN mp.tipo='ENTREGA' THEN mp.puntos ELSE 0 END),0) AS entrega,
                    COALESCE(SUM(CASE WHEN mp.tipo='REPORTE' THEN mp.puntos ELSE 0 END),0) AS reporte,
                    COALESCE(SUM(CASE WHEN mp.tipo='REVERSO' THEN mp.puntos ELSE 0 END),0) AS reversos,
                    COALESCE(SUM(CASE WHEN mp.tipo='CANJE' THEN -mp.puntos ELSE 0 END),0) AS canjes
             FROM movimiento_puntos mp
             JOIN vecino v ON v.id_vecino = mp.id_vecino
             LEFT JOIN entrega e ON e.id_entrega = mp.id_entrega
             LEFT JOIN reporte_punto_critico r ON r.id_reporte = mp.id_reporte
             WHERE mp.fecha_hora >= :inicio AND mp.fecha_hora < :fin
               AND (:sector = 0 OR COALESCE(e.id_sector, r.id_sector, v.id_sector) = :sector)
             """;
        var movimientos = jdbc.queryForMap(sqlMovimientos, p);
        String sqlCanjes = """
             SELECT
             COALESCE(SUM(CASE WHEN c.fecha_hora >= :inicio AND c.fecha_hora < :fin
                THEN c.monto_descuento ELSE 0 END),0) AS emitido,
             COALESCE(SUM(CASE WHEN c.estado = 'ANULADO' AND c.fecha_anulacion >= :inicio
                 AND c.fecha_anulacion < :fin THEN c.monto_descuento ELSE 0 END),0) AS anulado,
             COALESCE(SUM(CASE WHEN c.estado = 'EMITIDO' AND c.fecha_hora >= :inicio
                 AND c.fecha_hora < :fin THEN c.monto_descuento ELSE 0 END),0) AS neto
             FROM canje c JOIN vecino v ON v.id_vecino = c.id_vecino
             WHERE (:sector = 0 OR v.id_sector = :sector)
             """;
        var canjes = jdbc.queryForMap(sqlCanjes, p);
        // El denominador siempre incluye TODOS los materiales del sector y período, no solo el material seleccionado.
        String sqlKilos = """
             SELECT COALESCE(SUM(d.kilos),0) FROM detalle_entrega d
             JOIN entrega e ON e.id_entrega = d.id_entrega
             WHERE e.fecha_hora >= :inicio AND e.fecha_hora < :fin
               AND (:sector = 0 OR e.id_sector = :sector)
             """;
        BigDecimal kilosBase = decimal(jdbc.queryForObject(sqlKilos, p,
                (rs, n) -> rs.getBigDecimal(1)));
        BigDecimal emitido = decimal((BigDecimal) canjes.get("emitido"));
        BigDecimal anulado = decimal((BigDecimal) canjes.get("anulado"));
        BigDecimal neto = decimal((BigDecimal) canjes.get("neto"));
        BigDecimal ratio = kilosBase.signum() == 0 ? null :
                neto.divide(kilosBase, 4, RoundingMode.HALF_UP);
        return new Incentivos(
                ((Number) movimientos.get("entrega")).longValue(),
                ((Number) movimientos.get("reporte")).longValue(),
                ((Number) movimientos.get("reversos")).longValue(),
                ((Number) movimientos.get("canjes")).longValue(),
                emitido, anulado, neto, ratio, kilosBase);
    }
}
