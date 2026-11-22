package pe.ecopuntosves.web;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pe.ecopuntosves.service.PuntosService;

@Controller
public class PuntosController {

    private final PuntosService service;

    public PuntosController(PuntosService service) {
        this.service = service;
    }

    @GetMapping("/mis-puntos")
    public String historial(@RequestParam(defaultValue = "") String tipo,
                            @RequestParam(defaultValue = "0") int page,
                            Principal principal, Model model) {
        model.addAttribute("resumen", service.resumen(principal.getName(), tipo, page));
        return "puntos/historial";
    }
}