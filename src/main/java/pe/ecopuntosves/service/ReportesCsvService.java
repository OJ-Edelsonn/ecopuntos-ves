package pe.ecopuntosves.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import pe.ecopuntosves.service.ReportesEstadisticasService.*;

/** Exportación CSV compatible con Excel configurado en español: UTF-8 BOM y separador ;. */
@Service
public class ReportesCsvService {
    public byte[] generar(String tipo, Datos d) {
        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"EcoPuntos VES", "Reporte de " + switch (tipo) {
            case "acopio" -> "acopio";
            case "incidencias" -> "puntos críticos";
            case "incentivos" -> "incentivos";
            default -> throw new IllegalArgumentException("Tipo de reporte inválido.");
        }});
        filas.add(new String[]{"Desde", d.filtro().desde().toString()});
        filas.add(new String[]{"Hasta", d.filtro().hasta().toString()});
        filas.add(new String[]{"ID sector (0=todos)", String.valueOf(d.filtro().sectorId())});
        filas.add(new String[]{"ID material (0=todos; solo acopio)", String.valueOf(d.filtro().materialId())});
        filas.add(new String[]{"Estado (solo incidencias)", d.filtro().estado()});
        filas.add(new String[0]);

        switch (tipo) {
            case "acopio" -> acopio(filas, d.acopio());
            case "incidencias" -> incidencias(filas, d.incidencias());
            case "incentivos" -> incentivos(filas, d.incentivos());
            default -> throw new IllegalArgumentException("Tipo de reporte inválido.");
        }
        StringBuilder sb = new StringBuilder("\uFEFF");
        for (String[] fila : filas) {
            for (int i = 0; i < fila.length; i++) {
                if (i > 0) sb.append(';');
                sb.append(escapar(fila[i]));
            }
            sb.append("\r\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void acopio(List<String[]> f, Acopio a) {
        f.add(new String[]{"Total kg", n(a.kilos())});
        f.add(new String[]{"Entregas distintas", String.valueOf(a.entregas())});
        f.add(new String[0]);
        f.add(new String[]{"Material", "Kilogramos", "Entregas", "Puntos de detalles"});
        for (MaterialDato m : a.materiales())
            f.add(new String[]{m.nombre(), n(m.kilos()), String.valueOf(m.entregas()), String.valueOf(m.puntos())});
        f.add(new String[0]);
        f.add(new String[]{"Sector histórico de entrega", "Kilogramos", "Entregas"});
        for (SectorKilos s : a.sectores())
            f.add(new String[]{s.nombre(), n(s.kilos()), String.valueOf(s.entregas())});
    }
    private void incidencias(List<String[]> f, Incidencias i) {
        f.add(new String[]{"Reportes creados en período (estado actual)", String.valueOf(i.reportes())});
        f.add(new String[]{"Actualmente resueltos entre reportes creados", String.valueOf(i.resueltos())});
        f.add(new String[]{"Horas medias de resolución en período", nulo(i.horasPromedio())});
        f.add(new String[]{"Casos reincidentes", String.valueOf(i.reincidentes())});
        f.add(new String[]{"Base no rechazada", String.valueOf(i.baseReincidencia())});
        f.add(new String[]{"Reincidencia (%)", nulo(i.tasaReincidencia())});
        f.add(new String[0]);
        f.add(new String[]{"Estado actual", "Cantidad"});
        for (EstadoDato e : i.estados()) f.add(new String[]{e.nombre(), String.valueOf(e.cantidad())});
        f.add(new String[0]);
        f.add(new String[]{"Sector del reporte", "Cantidad"});
        for (SectorCasos s : i.sectores()) f.add(new String[]{s.nombre(), String.valueOf(s.cantidad())});
    }
    private void incentivos(List<String[]> f, Incentivos i) {
        f.add(new String[]{"Puntos otorgados por entregas", String.valueOf(i.puntosEntrega())});
        f.add(new String[]{"Puntos otorgados por reportes", String.valueOf(i.puntosReporte())});
        f.add(new String[]{"Puntos canjeados", String.valueOf(i.puntosCanjeados())});
        f.add(new String[]{"Puntos restituidos por anulaciones", String.valueOf(i.reversos())});
        f.add(new String[]{"Descuentos emitidos en el período (S/)", n(i.descuentoEmitido())});
        f.add(new String[]{"Descuentos anulados en el período (S/)", n(i.descuentoAnulado())});
        f.add(new String[]{"Descuento neto de la cohorte emitida (S/)", n(i.descuentoNetoCohorte())});
        f.add(new String[]{"Kilogramos de todos los materiales (denominador)", n(i.kilosBase())});
        f.add(new String[]{"Descuento neto de cohorte por kg (S/)", nulo(i.descuentoNetoPorKilo())});
        f.add(new String[0]);
        f.add(new String[]{"Nota", "Los puntos de recompensa y los importes de canje se presentan por separado."});
        f.add(new String[]{"Nota", "El sector de canjes corresponde al domicilio actual; en entregas y reportes se conserva el sector histórico."});
    }

    private static String n(BigDecimal n) { return n == null ? "" : n.toPlainString(); }
    private static String nulo(BigDecimal n) { return n == null ? "Sin datos / No calculable" : n(n); }

    /** Comillas CSV + neutralización de fórmulas que Excel podría ejecutar al abrir datos externos. */
    private static String escapar(String valor) {
        String texto = valor == null ? "" : valor;
        if (!texto.isEmpty() && (texto.charAt(0) == '=' || texto.charAt(0) == '+' ||
                texto.charAt(0) == '-' || texto.charAt(0) == '@' ||
                texto.charAt(0) == '\t' || texto.charAt(0) == '\r')) texto = "'" + texto;
        return '"' + texto.replace("\"", "\"\"") + '"';
    }
}
