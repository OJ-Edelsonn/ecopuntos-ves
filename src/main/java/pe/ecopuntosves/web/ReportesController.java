package pe.ecopuntosves.web;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import pe.ecopuntosves.service.ReportesCsvService;
import pe.ecopuntosves.service.ReportesEstadisticasService;
import pe.ecopuntosves.service.ReportesEstadisticasService.Filtro;

@Controller
@RequestMapping("/reportes")
public class ReportesController {
    private final ReportesEstadisticasService reportes;
    private final ReportesCsvService csv;
    public ReportesController(ReportesEstadisticasService reportes, ReportesCsvService csv) {
        this.reportes = reportes; this.csv = csv;
    }
    private void autorizar(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream().noneMatch(
                a -> a.getAuthority().equals("ROLE_FUNCIONARIO") ||
                     a.getAuthority().equals("ROLE_ADMINISTRADOR")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso reservado al personal autorizado.");
    }

    @GetMapping
    public String panel(@RequestParam(required = false) String desde,
                        @RequestParam(required = false) String hasta,
                        @RequestParam(required = false) Integer sectorId,
                        @RequestParam(required = false) Integer materialId,
                        @RequestParam(defaultValue = "") String estado,
                        Authentication auth, Model model) {
        autorizar(auth);
        Filtro filtro;
        try { filtro = reportes.filtro(desde, hasta, sectorId, materialId, estado); }
        catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            filtro = reportes.filtro(null, null, 0, 0, "");
        }
        model.addAttribute("filtro", filtro);
        model.addAttribute("datos", reportes.consultar(filtro));
        model.addAttribute("sectores", reportes.sectores());
        model.addAttribute("materiales", reportes.materiales());
        return "reportes/panel";
    }

    @GetMapping("/exportar/{tipo}")
    public ResponseEntity<byte[]> exportar(@PathVariable String tipo,
                        @RequestParam(required = false) String desde,
                        @RequestParam(required = false) String hasta,
                        @RequestParam(required = false) Integer sectorId,
                        @RequestParam(required = false) Integer materialId,
                        @RequestParam(defaultValue = "") String estado,
                        Authentication auth) {
        autorizar(auth);
        if (!tipo.equals("acopio") && !tipo.equals("incidencias") && !tipo.equals("incentivos"))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Filtro filtro;
        try { filtro = reportes.filtro(desde, hasta, sectorId, materialId, estado); }
        catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        byte[] contenido = csv.generar(tipo, reportes.consultar(filtro));
        String nombre = "ecopuntos_" + tipo + "_" + filtro.desde() + "_" + filtro.hasta() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(contenido);
    }
}
