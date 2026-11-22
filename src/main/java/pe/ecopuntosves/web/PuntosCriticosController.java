package pe.ecopuntosves.web;

import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.ecopuntosves.model.ReportePuntoCritico;
import pe.ecopuntosves.service.FotosPuntosCriticosService;
import pe.ecopuntosves.service.PuntosCriticosService;
import pe.ecopuntosves.service.PuntosCriticosService.FiltroIncidencias;
import pe.ecopuntosves.web.form.ReportePuntoCriticoForm;

@Controller
@RequestMapping("/puntos-criticos")
public class PuntosCriticosController {

    private final PuntosCriticosService servicio;
    private final FotosPuntosCriticosService fotos;

    public PuntosCriticosController(PuntosCriticosService servicio, FotosPuntosCriticosService fotos) {
        this.servicio = servicio;
        this.fotos = fotos;
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
                          @RequestParam(required = false) String desde,
                          @RequestParam(required = false) String hasta,
                          @RequestParam(defaultValue = "0") int page,
                          Authentication auth, Model model) {
        if (esVecino(auth)) {
            return "redirect:/puntos-criticos/mis-reportes";
        }
        FiltroIncidencias filtro;
        try {
            filtro = servicio.prepararFiltro(sectorId, estado, desde, hasta);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            filtro = new FiltroIncidencias(null, null, null, null);
        }
        model.addAttribute("reportes", servicio.listaFuncionario(filtro, page, auth.getName()));
        model.addAttribute("totales", servicio.totalesPorEstado(filtro, auth.getName()));
        model.addAttribute("sectores", servicio.sectores());
        model.addAttribute("sectorId", filtro.sectorId());
        model.addAttribute("estado", filtro.estado() == null ? "" : filtro.estado());
        model.addAttribute("desde", filtro.desde() == null ? "" : filtro.desde().toString());
        model.addAttribute("hasta", filtro.hasta() == null ? "" : filtro.hasta().toString());
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
                        @RequestParam(required = false) MultipartFile foto,
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
        boolean puedeAsignar = esFuncionario(auth) && "VALIDADO".equals(r.getEstado());
        model.addAttribute("reporte", r);
        model.addAttribute("atencion", servicio.obtenerAtencion(id, principal.getName()));
        model.addAttribute("cuadrillas", puedeAsignar ? servicio.cuadrillasActivas() : List.of());
        model.addAttribute("miembros", puedeAsignar ? servicio.miembrosActivos() : List.of());
        model.addAttribute("hoy", LocalDate.now(ZoneId.of("America/Lima")));
        banderas(model, auth);
        return "puntos-criticos/detalle";
    }

    @PostMapping("/{id}/decidir")
    public String decidir(@PathVariable int id,
                          @RequestParam(required = false) String decision,
                          @RequestParam(defaultValue = "") String motivo,
                          Principal principal, RedirectAttributes flash) {
        try {
            if (!"VALIDADO".equals(decision) && !"RECHAZADO".equals(decision)) {
                throw new IllegalArgumentException("Decisión inválida.");
            }
            servicio.decidir(id, "VALIDADO".equals(decision), motivo, principal.getName());
            flash.addFlashAttribute("mensaje", "VALIDADO".equals(decision)
                    ? "Reporte validado y puntos asignados una sola vez."
                    : "Reporte rechazado con motivo registrado.");
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", "No se pudo procesar la decisión: "
                    + (ex instanceof IllegalArgumentException ? ex.getMessage() : "restricción de datos"));
        }
        return "redirect:/puntos-criticos/" + id;
    }

    @PostMapping("/{id}/iniciar-atencion")
    public String iniciarAtencion(@PathVariable int id,
                                  @RequestParam(required = false) Integer cuadrillaId,
                                  @RequestParam(required = false) Integer miembroId,
                                  @RequestParam(required = false)
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                  LocalDate fechaProgramada,
                                  Principal principal, RedirectAttributes flash) {
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
    public String resolver(@PathVariable int id,
                           @RequestParam(required = false) String observacion,
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
        if (r.getFotoUrl() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        try {
            return ResponseEntity.ok().contentType(fotos.tipo(r.getFotoUrl()))
                    .cacheControl(CacheControl.noStore())
                    .body(fotos.leer(r.getFotoUrl()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el archivo.");
        }
    }
}