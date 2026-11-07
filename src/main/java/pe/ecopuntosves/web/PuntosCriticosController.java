package pe.ecopuntosves.web;

import java.security.Principal;
import java.time.LocalDate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.ecopuntosves.model.ReportePuntoCritico;
import pe.ecopuntosves.service.FotosPuntosCriticosService;
import pe.ecopuntosves.service.PuntosCriticosService;
import pe.ecopuntosves.web.form.ReportePuntoCriticoForm;

@Controller
@RequestMapping("/puntos-criticos")
public class PuntosCriticosController {
    private final PuntosCriticosService servicio;
    private final FotosPuntosCriticosService fotos;

    public PuntosCriticosController(PuntosCriticosService servicio, FotosPuntosCriticosService fotos) {
        this.servicio = servicio; this.fotos = fotos;
    }

    private boolean esFuncionario(Authentication a) {
        return a.getAuthorities().stream().anyMatch(x ->
                x.getAuthority().equals("ROLE_FUNCIONARIO"));
    }
    private boolean esVecino(Authentication a) {
        return a.getAuthorities().stream().anyMatch(x ->
                x.getAuthority().equals("ROLE_VECINO"));
    }
    private void banderas(Model m, Authentication a) {
        m.addAttribute("esFuncionario", esFuncionario(a));
        m.addAttribute("esVecino", esVecino(a));
    }

    @GetMapping
    public String listado(@RequestParam(required = false) Integer sectorId,
                          @RequestParam(defaultValue = "") String estado,
                          @RequestParam(defaultValue = "0") int page,
                          Authentication auth, Model model) {
        if (esVecino(auth)) return "redirect:/puntos-criticos/mis-reportes";
        model.addAttribute("reportes", servicio.listaFuncionario(sectorId, estado, page, auth.getName()));
        model.addAttribute("sectores", servicio.sectores());
        model.addAttribute("sectorId", sectorId);
        model.addAttribute("estado", estado);
        banderas(model, auth);
        return "puntos-criticos/lista";
    }

    @GetMapping("/mis-reportes")
    public String misReportes(@RequestParam(defaultValue = "0") int page,
                              Authentication auth, Model model) {
        model.addAttribute("reportes", servicio.misReportes(auth.getName(), page));
        banderas(model, auth);
        return "puntos-criticos/mis-reportes";
    }

    @GetMapping("/nuevo")
    public String nuevo(Authentication auth, Model model) {
        model.addAttribute("form", new ReportePuntoCriticoForm());
        model.addAttribute("sectores", servicio.sectores());
        banderas(model, auth);
        return "puntos-criticos/formulario";
    }

    @PostMapping("/nuevo")
    public String crear(@ModelAttribute("form") ReportePuntoCriticoForm form,
                        @RequestParam(name = "foto", required = false) MultipartFile foto,
                        Authentication auth, Model model, RedirectAttributes flash) {
        try {
            Integer id = servicio.registrar(form, foto, auth.getName());
            flash.addFlashAttribute("mensaje", "Reporte registrado. Se encuentra pendiente de validación.");
            return "redirect:/puntos-criticos/" + id;
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            model.addAttribute("error", ex instanceof DataIntegrityViolationException
                ? "Ya hay un reporte abierto en esa ubicación y sector." : ex.getMessage());
            model.addAttribute("sectores", servicio.sectores());
            banderas(model, auth);
            return "puntos-criticos/formulario";
        }
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable int id, Principal principal,
                          Authentication auth, Model model) {
        ReportePuntoCritico r = servicio.verAutorizado(id, principal.getName());
        model.addAttribute("reporte", r);
        model.addAttribute("atencion", servicio.obtenerAtencion(id, principal.getName()));
        model.addAttribute("cuadrillas", servicio.cuadrillasActivas());
        model.addAttribute("miembros", servicio.miembrosActivos());
        model.addAttribute("hoy", LocalDate.now(java.time.ZoneId.of("America/Lima")));
        banderas(model, auth);
        return "puntos-criticos/detalle";
    }

    @PostMapping("/{id}/decidir")
    public String decidir(@PathVariable int id, @RequestParam String decision,
                          @RequestParam(defaultValue = "") String motivo,
                          Principal principal, RedirectAttributes flash) {
        try {
            if (!decision.equals("VALIDADO") && !decision.equals("RECHAZADO"))
                throw new IllegalArgumentException("Decisión inválida.");
            servicio.decidir(id, decision.equals("VALIDADO"), motivo, principal.getName());
            flash.addFlashAttribute("mensaje", decision.equals("VALIDADO")
                ? "Reporte validado y puntos asignados una sola vez."
                : "Reporte rechazado con motivo registrado.");
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", "No se pudo procesar la decisión: " +
                (ex instanceof IllegalArgumentException ? ex.getMessage() : "restricción de datos"));
        }
        return "redirect:/puntos-criticos/" + id;
    }

    @PostMapping("/{id}/iniciar-atencion")
    public String iniciarAtencion(@PathVariable int id,
                                  @RequestParam Integer cuadrillaId,
                                  @RequestParam(required = false) Integer miembroId,
                                  @RequestParam(required = false)
                                  @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                                  LocalDate fechaProgramada, Principal principal,
                                  RedirectAttributes flash) {
        try {
            servicio.iniciarAtencion(id, cuadrillaId, miembroId, fechaProgramada, principal.getName());
            flash.addFlashAttribute("mensaje", "Atención asignada y reporte EN_ATENCION.");
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", ex instanceof IllegalArgumentException
                ? ex.getMessage() : "No fue posible asignar la atención.");
        }
        return "redirect:/puntos-criticos/" + id;
    }

    @PostMapping("/{id}/resolver")
    public String resolver(@PathVariable int id, @RequestParam String observacion,
                           Principal principal, RedirectAttributes flash) {
        try {
            servicio.resolver(id, observacion, principal.getName());
            flash.addFlashAttribute("mensaje", "El punto crítico quedó RESUELTO.");
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", ex instanceof IllegalArgumentException
                ? ex.getMessage() : "No fue posible resolver el reporte.");
        }
        return "redirect:/puntos-criticos/" + id;
    }

    @GetMapping("/{id}/foto")
    @ResponseBody
    public ResponseEntity<byte[]> verFoto(@PathVariable int id, Principal principal) {
        ReportePuntoCritico r = servicio.verAutorizado(id, principal.getName());
        if (r.getFotoUrl() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        try {
            return ResponseEntity.ok().contentType(fotos.tipo(r.getFotoUrl()))
                .cacheControl(CacheControl.noStore())
                .body(fotos.leer(r.getFotoUrl()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el archivo.");
        }
    }

    @ExceptionHandler(SecurityException.class)
    public void accesoDenegado(SecurityException ex) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado.");
    }
}
