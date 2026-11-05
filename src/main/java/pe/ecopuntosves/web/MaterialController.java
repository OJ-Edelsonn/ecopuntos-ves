package pe.ecopuntosves.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pe.ecopuntosves.service.TipoMaterialService;

@Controller
public class MaterialController {

    private final TipoMaterialService service;

    public MaterialController(TipoMaterialService service) {
        this.service = service;
    }

    @GetMapping("/materiales")
    public String listar(Model model) {
        model.addAttribute("materiales", service.listarActivos());
        return "materiales";
    }
}
