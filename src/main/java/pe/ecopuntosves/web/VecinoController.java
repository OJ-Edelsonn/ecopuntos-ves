package pe.ecopuntosves.web;

import java.security.Principal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.ecopuntosves.model.Vecino;
import pe.ecopuntosves.service.CuentasService;
import pe.ecopuntosves.service.PuntosService;
import pe.ecopuntosves.web.form.VecinoForm;

@Controller
public class VecinoController {

    private final CuentasService service;
    private final PuntosService puntosService;

    public VecinoController(CuentasService service, PuntosService puntosService) {
        this.service = service;
        this.puntosService = puntosService;
    }

    @GetMapping("/vecinos")
    public String listar(@RequestParam(defaultValue = "") String q,
                         @RequestParam(defaultValue = "0") int page, Model m) {
        Page<Vecino> vecinos = service.buscarVecinos(q, page);
        m.addAttribute("vecinos", vecinos);
        m.addAttribute("q", q);
        return "vecinos/lista";
    }

    @GetMapping("/vecinos/nuevo")
    public String nuevo(Model m) {
        m.addAttribute("form", new VecinoForm());
        m.addAttribute("sectores", service.listarSectores());
        return "vecinos/formulario";
    }

    @PostMapping("/vecinos/nuevo")
    public String guardar(@ModelAttribute("form") VecinoForm form, Model m, RedirectAttributes flash) {
        try {
            String[] credenciales = service.registrarVecino(form).split("\\|", 2);
            flash.addFlashAttribute("usuarioCreado", credenciales[0]);
            flash.addFlashAttribute("claveTemporal", credenciales[1]);
            flash.addFlashAttribute("mensaje", "Vecino registrado correctamente.");
            return "redirect:/vecinos";
        } catch (IllegalArgumentException e) {
            m.addAttribute("error", e.getMessage());
            m.addAttribute("sectores", service.listarSectores());
            return "vecinos/formulario";
        } catch (DataIntegrityViolationException e) {
            m.addAttribute("error", "Ya existe un registro con esos datos.");
            m.addAttribute("sectores", service.listarSectores());
            return "vecinos/formulario";
        }
    }

    @GetMapping("/vecinos/{id}")
    public String detalle(@PathVariable int id, Model m) {
        m.addAttribute("vecino", service.obtenerVecino(id));
        return "vecinos/detalle";
    }

    @GetMapping("/vecinos/{id}/editar")
    public String editar(@PathVariable int id, Model m) {
        Vecino v = service.obtenerVecino(id);
        VecinoForm f = new VecinoForm();
        f.setDireccion(v.getDireccion());
        f.setTelefono(v.getTelefono());
        f.setCodigoContribuyente(v.getCodigoContribuyente());
        f.setSectorId(v.getSector().getId());
        m.addAttribute("form", f);
        m.addAttribute("vecino", v);
        m.addAttribute("sectores", service.listarSectores());
        return "vecinos/editar";
    }

    @PostMapping("/vecinos/{id}/editar")
    public String actualizar(@PathVariable int id, @ModelAttribute("form") VecinoForm f,
                             Model m, RedirectAttributes flash) {
        try {
            service.editarVecino(id, f);
            flash.addFlashAttribute("mensaje", "Datos actualizados.");
            return "redirect:/vecinos/" + id;
        } catch (IllegalArgumentException e) {
            m.addAttribute("error", e.getMessage());
            m.addAttribute("vecino", service.obtenerVecino(id));
            m.addAttribute("sectores", service.listarSectores());
            return "vecinos/editar";
        }
    }

    @GetMapping("/mi-perfil")
    public String perfil(Principal principal, Model m) {
        m.addAttribute("vecino", service.obtenerPerfil(principal.getName()));
        m.addAttribute("saldo", puntosService.saldoDeUsuario(principal.getName()));
        return "vecinos/perfil";
    }
}