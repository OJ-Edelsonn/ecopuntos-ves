package pe.ecopuntosves.web;

import java.security.Principal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.ecopuntosves.model.Canje;
import pe.ecopuntosves.service.CanjesService;

@Controller
@RequestMapping("/canjes")
public class CanjesController {
    private final CanjesService service;
    public CanjesController(CanjesService service) { this.service = service; }

    @GetMapping
    public String miPanel(@RequestParam(defaultValue = "0") int page,
                          Principal principal, Model m) {
        m.addAttribute("resumen", service.resumen(principal.getName(), page));
        return "canjes/panel";
    }

    @PostMapping("/emitir")
    public String emitir(@RequestParam(name = "puntos", required = false) Integer puntos,
                         Principal principal, RedirectAttributes flash) {
        try {
            Integer id = service.emitir(principal.getName(), puntos);
            flash.addFlashAttribute("mensaje", "Canje registrado y puntos descontados correctamente.");
            return "redirect:/canjes/" + id;
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", ex instanceof IllegalArgumentException ? ex.getMessage()
                    : "No se pudo emitir la constancia. Intenta nuevamente.");
            return "redirect:/canjes";
        }
    }

    @GetMapping("/administrar")
    public String administrar(@RequestParam(defaultValue = "") String codigo,
                              Principal principal, Model m) {
        m.addAttribute("codigo", codigo);
        m.addAttribute("encontrado", service.buscarFuncionario(principal.getName(), codigo));
        return "canjes/administrar";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable int id, Principal principal, Model m) {
        m.addAttribute("canje", service.detalleAutorizado(id, principal.getName()));
        m.addAttribute("esFuncionario", esFuncionario(principal.getName()));
        return "canjes/detalle";
    }

    @GetMapping("/{id}/constancia")
    public String constancia(@PathVariable int id, Principal principal, Model m) {
        m.addAttribute("canje", service.detalleAutorizado(id, principal.getName()));
        return "canjes/constancia";
    }

    @PostMapping("/{id}/anular")
    public String anular(@PathVariable int id, @RequestParam(defaultValue = "") String motivo,
                         Principal principal, RedirectAttributes flash) {
        try {
            service.anular(id, motivo, principal.getName());
            flash.addFlashAttribute("mensaje", "Canje anulado: los puntos fueron devueltos una sola vez.");
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", ex instanceof IllegalArgumentException ? ex.getMessage()
                    : "No se pudo anular. Comprueba el estado y vuelve a intentarlo.");
        }
        return "redirect:/canjes/" + id;
    }

    private boolean esFuncionario(String username) {
        // La interfaz no reemplaza las comprobaciones de rol del servicio y de Spring Security.
        return service.esFuncionario(username);
    }

    @ExceptionHandler(SecurityException.class)
    public void accesoDenegado(SecurityException e) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado.");
    }
}
