package pe.ecopuntosves.web;

import java.security.Principal;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.ecopuntosves.model.Entrega;
import pe.ecopuntosves.service.EntregasService;
import pe.ecopuntosves.web.form.EntregaForm;

@Controller
public class EntregaController {
    private final EntregasService service;
    public EntregaController(EntregasService service) { this.service=service; }

    @GetMapping("/entregas")
    public String listar(@RequestParam(defaultValue="") String dni,
                         @RequestParam(defaultValue="0") int page,
                         @RequestParam(defaultValue="numeroAsc") String orden, Model m) {
        m.addAttribute("entregas",service.listar(dni,page,orden));
        m.addAttribute("dni",dni);
        m.addAttribute("orden", "recientes".equals(orden) ? "recientes" : "numeroAsc");
        return "entregas/lista";
    }
    @GetMapping("/entregas/nueva")
    public String nuevo(@RequestParam(defaultValue="") String dni,Model m) {
        EntregaForm f=new EntregaForm(); f.setDniVecino(dni);
        m.addAttribute("form",f);
        m.addAttribute("materiales",service.materialesActivos());
        return "entregas/formulario";
    }
    @PostMapping("/entregas/nueva")
    public String registrar(@ModelAttribute("form") EntregaForm f, Principal principal,
                            Model m,RedirectAttributes flash) {
        try {
            Integer id=service.registrar(f,principal.getName());
            flash.addFlashAttribute("mensaje","Entrega registrada correctamente; puntos acreditados.");
            return "redirect:/entregas/"+id;
        } catch(IllegalArgumentException | org.springframework.dao.DataIntegrityViolationException ex) {
            m.addAttribute("error",ex instanceof IllegalArgumentException?ex.getMessage():
                "No fue posible guardar. Verifica las cantidades y vuelve a intentarlo.");
            m.addAttribute("materiales",service.materialesActivos());
            return "entregas/formulario";
        }
    }
    @GetMapping("/entregas/{id}")
    public String detalle(@PathVariable int id,Model m) {
        Entrega e=service.obtener(id);
        m.addAttribute("entrega",e); m.addAttribute("detalles",service.detalle(id));
        m.addAttribute("saldo",service.saldo(e.getVecino().getId()));
        return "entregas/detalle";
    }
    @GetMapping("/mis-entregas")
    public String misEntregas(@RequestParam(defaultValue="0") int page,
                              Principal principal,Model m) {
        Page<Entrega> datos=service.misEntregas(principal.getName(),page);
        m.addAttribute("entregas",datos);
        m.addAttribute("saldo",service.miSaldo(principal.getName()));
        return "entregas/mis-entregas";
    }
}
