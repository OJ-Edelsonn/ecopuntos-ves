package pe.ecopuntosves.web;

import java.security.Principal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.ecopuntosves.model.Usuario;
import pe.ecopuntosves.service.CuentasService;
import pe.ecopuntosves.web.form.PersonalForm;

@Controller
public class UsuarioController {

    private final CuentasService service;

    public UsuarioController(CuentasService service) {
        this.service = service;
    }

    @GetMapping("/usuarios")
    public String lista(@RequestParam(defaultValue = "0") int page, Model m) {
        m.addAttribute("usuarios", service.listarPersonal(page));
        return "usuarios/lista";
    }

    @GetMapping("/usuarios/nuevo")
    public String nuevo(Model m) {
        m.addAttribute("form", new PersonalForm());
        return "usuarios/nuevo";
    }

    @PostMapping("/usuarios/nuevo")
    public String registrar(@ModelAttribute("form") PersonalForm f, Model m, RedirectAttributes flash) {
        try {
            String[] cred = service.registrarPersonal(f).split("\\|", 2);
            flash.addFlashAttribute("usuarioCreado", cred[0]);
            flash.addFlashAttribute("claveTemporal", cred[1]);
            flash.addFlashAttribute("mensaje", "Personal registrado correctamente.");
            return "redirect:/usuarios";
        } catch (IllegalArgumentException e) {
            m.addAttribute("error", e.getMessage());
            return "usuarios/nuevo";
        } catch (DataIntegrityViolationException e) {
            m.addAttribute("error", "El usuario ya existe.");
            return "usuarios/nuevo";
        }
    }

    @GetMapping("/usuarios/{id}/editar")
    public String editar(@PathVariable int id, Model m) {
        Usuario u = service.obtenerPersonal(id);
        PersonalForm f = new PersonalForm();
        f.setUsername(u.getUsername());
        f.setNombres(u.getNombres());
        f.setApellidos(u.getApellidos());
        f.setCorreo(u.getCorreo());
        f.setRol(u.getRol().getNombre());
        m.addAttribute("usuario", u);
        m.addAttribute("form", f);
        return "usuarios/editar";
    }

    @PostMapping("/usuarios/{id}/editar")
    public String actualizar(@PathVariable int id, @ModelAttribute("form") PersonalForm f,
                             Model m, RedirectAttributes flash) {
        try {
            service.editarPersonal(id, f);
            flash.addFlashAttribute("mensaje", "Datos del personal actualizados correctamente.");
            return "redirect:/usuarios";
        } catch (IllegalArgumentException e) {
            m.addAttribute("error", e.getMessage());
            m.addAttribute("usuario", service.obtenerPersonal(id));
            return "usuarios/editar";
        }
    }

    @PostMapping("/usuarios/{id}/estado")
    public String cambiarEstado(@PathVariable int id, Principal principal, RedirectAttributes flash) {
        try {
            service.cambiarEstadoPersonal(id, principal.getName());
            flash.addFlashAttribute("mensaje", "Estado actualizado.");
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @GetMapping("/cambiar-clave")
    public String cambioClave() {
        return "usuarios/cambiar-clave";
    }

    @PostMapping("/cambiar-clave")
    public String cambiarClave(@RequestParam String actual, @RequestParam String nueva,
                               @RequestParam String confirmacion, Principal principal,
                               RedirectAttributes flash) {
        try {
            service.cambiarClave(principal.getName(), actual, nueva, confirmacion);
            flash.addFlashAttribute("mensaje", "Contraseña actualizada correctamente.");
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/cambiar-clave";
        }
    }
}