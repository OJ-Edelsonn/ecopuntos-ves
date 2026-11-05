package pe.ecopuntosves.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

    @GetMapping("/")
    public String mostrarInicio(Model model) {
        model.addAttribute("nombreSistema", "EcoPuntos VES");
        return "inicio";
    }
}