package pe.ecopuntosves.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.ecopuntosves.service.ReglasMaterialesService;

@Controller
@RequestMapping("/reglas")
public class ReglasMaterialesController {

    private final ReglasMaterialesService service;

    public ReglasMaterialesController(ReglasMaterialesService service) {
        this.service = service;
    }

    private void protegerAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ADMINISTRADOR"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso reservado al administrador.");
        }
    }

    @GetMapping
    public String panel(Authentication auth, Model model) {
        protegerAdmin(auth);
        model.addAttribute("materiales", service.listarMaterialesConRegla());
        model.addAttribute("reglaReporte", service.reglaGlobalVigente("REPORTE"));
        model.addAttribute("reglaCanje", service.reglaGlobalVigente("CANJE"));
        model.addAttribute("historial", service.historialReglas());
        return "reglas/panel";
    }

    @PostMapping("/materiales/nuevo")
    public String crearMaterial(@RequestParam(required = false) String nombre,
                                @RequestParam(required = false) String valor,
                                Authentication auth, RedirectAttributes flash) {
        protegerAdmin(auth);
        try {
            service.crearMaterial(nombre, valor);
            flash.addFlashAttribute("mensaje", "Material y tarifa inicial registrados correctamente.");
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", ex instanceof IllegalArgumentException
                    ? ex.getMessage() : "Ya existe un material con ese nombre.");
        }
        return "redirect:/reglas";
    }

    @PostMapping("/materiales/{id}/estado")
    public String cambiarEstadoMaterial(@PathVariable int id,
                                        @RequestParam boolean activo,
                                        Authentication auth, RedirectAttributes flash) {
        protegerAdmin(auth);
        try {
            service.cambiarEstadoMaterial(id, activo);
            flash.addFlashAttribute("mensaje", "Estado del material actualizado.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reglas";
    }

    @PostMapping("/reciclaje/{materialId}")
    public String actualizarTarifaMaterial(@PathVariable int materialId,
                                           @RequestParam(required = false) String valor,
                                           Authentication auth, RedirectAttributes flash) {
        protegerAdmin(auth);
        try {
            service.actualizarTarifaMaterial(materialId, valor);
            flash.addFlashAttribute("mensaje", "Nueva versión de tarifa registrada. Las entregas anteriores conservan su tarifa histórica.");
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", ex instanceof IllegalArgumentException
                    ? ex.getMessage() : "No se pudo actualizar la tarifa. Intenta nuevamente.");
        }
        return "redirect:/reglas";
    }

    @PostMapping("/global/{concepto}")
    public String actualizarReglaGlobal(@PathVariable String concepto,
                                        @RequestParam(required = false) String valor,
                                        Authentication auth, RedirectAttributes flash) {
        protegerAdmin(auth);
        try {
            service.actualizarReglaGlobal(concepto, valor);
            flash.addFlashAttribute("mensaje", "Nueva versión de regla " + concepto.toUpperCase() + " activada correctamente.");
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", ex instanceof IllegalArgumentException
                    ? ex.getMessage() : "No se pudo actualizar la regla. Intenta nuevamente.");
        }
        return "redirect:/reglas";
    }
}