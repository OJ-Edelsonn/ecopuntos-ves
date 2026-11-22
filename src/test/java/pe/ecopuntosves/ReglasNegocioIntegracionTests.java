package pe.ecopuntosves;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import pe.ecopuntosves.model.Entrega;
import pe.ecopuntosves.model.ReglaPuntos;
import pe.ecopuntosves.model.Vecino;
import pe.ecopuntosves.repository.ReglaPuntosRepository;
import pe.ecopuntosves.repository.VecinoRepository;
import pe.ecopuntosves.service.CanjesService;
import pe.ecopuntosves.service.EntregasService;
import pe.ecopuntosves.service.PuntosCriticosService;
import pe.ecopuntosves.service.ReglasMaterialesService;
import pe.ecopuntosves.service.ReportesCsvService;
import pe.ecopuntosves.service.ReportesEstadisticasService.Acopio;
import pe.ecopuntosves.service.ReportesEstadisticasService.Datos;
import pe.ecopuntosves.service.ReportesEstadisticasService.Filtro;
import pe.ecopuntosves.service.ReportesEstadisticasService.Incidencias;
import pe.ecopuntosves.service.ReportesEstadisticasService.Incentivos;
import pe.ecopuntosves.service.ReportesEstadisticasService.MaterialDato;
import pe.ecopuntosves.web.form.EntregaForm;
import pe.ecopuntosves.web.form.ReportePuntoCriticoForm;

@SpringBootTest
@Transactional
class ReglasNegocioIntegracionTests {

    @Autowired
    private EntregasService entregasService;

    @Autowired
    private CanjesService canjesService;

    @Autowired
    private PuntosCriticosService puntosCriticosService;

    @Autowired
    private ReglasMaterialesService reglasMaterialesService;

    @Autowired
    private ReportesCsvService csvService;

    @Autowired
    private VecinoRepository vecinos;

    @Autowired
    private ReglaPuntosRepository reglas;

    @Test
    void entregaCalculaPuntosSegunReglaVigenteYRechazaMaterialDuplicado() {
        Vecino vecino = vecinos.findByUsuario_Username("vecino001").orElseThrow();
        long saldoInicial = entregasService.saldo(vecino.getId());

        ReglaPuntos reglaPlastico = reglas.findByConceptoAndMaterial_IdAndVigenteTrue("RECICLAJE", 1).orElseThrow();
        ReglaPuntos reglaPapel = reglas.findByConceptoAndMaterial_IdAndVigenteTrue("RECICLAJE", 2).orElseThrow();

        int esperadoPlastico = new BigDecimal("2.00").multiply(reglaPlastico.getValor())
                .setScale(0, RoundingMode.HALF_UP).intValueExact();
        int esperadoPapel = new BigDecimal("3.00").multiply(reglaPapel.getValor())
                .setScale(0, RoundingMode.HALF_UP).intValueExact();
        int puntosEsperados = esperadoPlastico + esperadoPapel;

        EntregaForm form = new EntregaForm();
        form.setDniVecino(vecino.getDni());
        form.setMaterialIds(List.of(1, 2));
        form.setKilos(List.of(new BigDecimal("2.00"), new BigDecimal("3.00")));

        Integer idEntrega = entregasService.registrar(form, "operador01");
        Entrega guardada = entregasService.obtener(idEntrega);

        assertEquals(new BigDecimal("5.00"), guardada.getKilosTotal());
        assertEquals(puntosEsperados, guardada.getPuntosOtorgados());
        assertEquals(saldoInicial + puntosEsperados, entregasService.saldo(vecino.getId()));

        // Material repetido debe rechazarse (CP-22)
        EntregaForm duplicado = new EntregaForm();
        duplicado.setDniVecino(vecino.getDni());
        duplicado.setMaterialIds(List.of(1, 1));
        duplicado.setKilos(List.of(new BigDecimal("1.00"), new BigDecimal("2.00")));

        assertThrows(IllegalArgumentException.class, () -> entregasService.registrar(duplicado, "operador01"));
    }

    @Test
    void canjeInvalidoEsRechazadoSinAlterarSaldo() {
        Vecino vecino = vecinos.findByUsuario_Username("vecino001").orElseThrow();
        long saldoAntes = entregasService.saldo(vecino.getId());

        assertThrows(IllegalArgumentException.class, () -> canjesService.emitir("vecino001", 0));
        assertThrows(IllegalArgumentException.class, () -> canjesService.emitir("vecino001", -10));
        assertThrows(IllegalArgumentException.class, () -> canjesService.emitir("vecino001", 99999990));

        assertEquals(saldoAntes, entregasService.saldo(vecino.getId()));
    }

    @Test
    void reporteDuplicadoAbiertoEsBloqueadoEnBaseDeDatos() {
        ReportePuntoCriticoForm primero = new ReportePuntoCriticoForm();
        primero.setSectorId(1);
        primero.setDireccion("Av. Prueba Integracion 9981");
        primero.setTipoResiduo("Residuos mixtos");
        primero.setDescripcion("Acumulación de prueba para validar unicidad abierta.");

        Integer idPrimero = puntosCriticosService.registrar(primero, null, "vecino350");
        assertNotNull(idPrimero);

        // Misma dirección con diferente formato (AV. -> AVENIDA) debe fallar por uq_reporte_ubicacion_abierta (CP-14)
        ReportePuntoCriticoForm duplicado = new ReportePuntoCriticoForm();
        duplicado.setSectorId(1);
        duplicado.setDireccion("AVENIDA PRUEBA INTEGRACIÓN 9981");
        duplicado.setTipoResiduo("Plásticos");
        duplicado.setDescripcion("Segundo reporte en la misma ubicación.");

        assertThrows(DataIntegrityViolationException.class,
                () -> puntosCriticosService.registrar(duplicado, null, "vecino350"));
    }

    @Test
    void versionadoDeReglaCierraVersionAnteriorYActivaNueva() {
        ReglaPuntos anterior = reglas.findByConceptoAndMaterialIsNullAndVigenteTrue("REPORTE").orElseThrow();
        int nuevoValor = anterior.getValor().intValueExact() + 5;

        reglasMaterialesService.actualizarReglaGlobal("REPORTE", String.valueOf(nuevoValor));

        ReglaPuntos anteriorRecargada = reglas.findById(anterior.getId()).orElseThrow();
        ReglaPuntos vigente = reglas.findByConceptoAndMaterialIsNullAndVigenteTrue("REPORTE").orElseThrow();

        assertFalse(anteriorRecargada.isVigente());
        assertNotNull(anteriorRecargada.getFechaFin());
        assertTrue(vigente.isVigente());
        assertEquals(new BigDecimal(nuevoValor).setScale(2), vigente.getValor());
    }

    @Test
    void csvExportaConBomUtf8SeparadorPuntoYComaYNeutralizaFormulas() {
        Filtro filtro = new Filtro(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 0, 0, "");
        Acopio acopio = new Acopio(
                new BigDecimal("10.00"),
                1,
                List.of(new MaterialDato("=SUM(A1:A2)", new BigDecimal("10.00"), 1, 100, 100)),
                List.of());
        Incidencias inc = new Incidencias(0, 0, null, 0, 0, null, List.of(), List.of());
        Incentivos incentivos = new Incentivos(0, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO);

        byte[] bytes = csvService.generar("acopio", new Datos(filtro, acopio, inc, incentivos));
        String contenido = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(contenido.startsWith("\uFEFF"), "Debe iniciar con BOM UTF-8 para Excel");
        assertTrue(contenido.contains(";"), "Debe usar punto y coma como separador");
        assertTrue(contenido.contains("\"'=SUM(A1:A2)\""), "Debe neutralizar fórmulas peligrosas");
    }
}