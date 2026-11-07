package pe.ecopuntosves.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.ecopuntosves.service.CuadrillasService;

@Controller
@RequestMapping("/cuadrillas")
public class CuadrillasController {
    private final CuadrillasService servicio;
    public CuadrillasController(CuadrillasService servicio) { this.servicio=servicio; }
    private boolean admin(Authentication a) { return a!=null && a.getAuthorities().stream()
            .anyMatch(x->x.getAuthority().equals("ROLE_ADMINISTRADOR")); }
    private boolean funcionario(Authentication a) { return a!=null && a.getAuthorities().stream()
            .anyMatch(x->x.getAuthority().equals("ROLE_FUNCIONARIO")); }
    private void proteger(Authentication a,boolean edicion) {
        if(a==null || !a.isAuthenticated() || (edicion ? !admin(a) : !admin(a) && !funcionario(a)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    @GetMapping
    public String listado(Authentication auth,Model model) {
        proteger(auth,false);
        model.addAttribute("cuadrillas",servicio.cuadrillas());
        model.addAttribute("miembros",servicio.miembros());
        model.addAttribute("sectores",servicio.sectores());
        model.addAttribute("esAdmin",admin(auth));
        return "cuadrillas/lista";
    }
    @PostMapping("/nueva")
    public String nueva(@RequestParam String nombre, @RequestParam(required=false) Integer sectorId,
                        Authentication auth,RedirectAttributes flash) {
        proteger(auth,true);
        try { servicio.crearCuadrilla(nombre,sectorId);flash.addFlashAttribute("mensaje","Cuadrilla registrada."); }
        catch(IllegalArgumentException|DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error",ex instanceof IllegalArgumentException ? ex.getMessage():"Nombre duplicado.");
        }
        return "redirect:/cuadrillas";
    }
    @PostMapping("/miembros/nuevo")
    public String nuevoMiembro(@RequestParam Integer cuadrillaId,@RequestParam String nombres,
                               @RequestParam String apellidos,@RequestParam String cargo,
                               Authentication auth,RedirectAttributes flash) {
        proteger(auth,true);
        try { servicio.crearMiembro(cuadrillaId,nombres,apellidos,cargo);
            flash.addFlashAttribute("mensaje","Integrante registrado."); }
        catch(IllegalArgumentException|DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error",ex instanceof IllegalArgumentException ? ex.getMessage():"Integrante duplicado.");
        }
        return "redirect:/cuadrillas";
    }
    @PostMapping("/{id}/estado")
    public String estadoCuadrilla(@PathVariable int id,@RequestParam boolean activo,
                                 Authentication auth,RedirectAttributes flash) {
        proteger(auth,true);
        try { servicio.cambiarCuadrilla(id,activo);flash.addFlashAttribute("mensaje","Estado de cuadrilla actualizado."); }
        catch(IllegalArgumentException ex) { flash.addFlashAttribute("error",ex.getMessage()); }
        return "redirect:/cuadrillas";
    }
    @PostMapping("/miembros/{id}/estado")
    public String estadoMiembro(@PathVariable int id,@RequestParam boolean activo,
                               Authentication auth,RedirectAttributes flash) {
        proteger(auth,true);
        try { servicio.cambiarMiembro(id,activo);flash.addFlashAttribute("mensaje","Estado de integrante actualizado."); }
        catch(IllegalArgumentException ex) {flash.addFlashAttribute("error",ex.getMessage());}
        return "redirect:/cuadrillas";
    }
}
