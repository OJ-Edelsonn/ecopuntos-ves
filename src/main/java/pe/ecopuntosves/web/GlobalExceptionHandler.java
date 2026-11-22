package pe.ecopuntosves.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({SecurityException.class, AccessDeniedException.class})
    public String accesoDenegado(Exception ex, HttpServletResponse res, Model model) {
        res.setStatus(HttpStatus.FORBIDDEN.value());
        model.addAttribute("detalle", "No cuentas con los permisos necesarios para acceder a este recurso.");
        return "error/403";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String noEncontrado(HttpServletResponse res, Model model) {
        res.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("detalle", "La página o recurso solicitado no existe.");
        return "error/404";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String estadoHttp(ResponseStatusException ex, HttpServletResponse res, Model model) {
        int codigo = ex.getStatusCode().value();
        res.setStatus(codigo);
        model.addAttribute("codigo", codigo);
        model.addAttribute("detalle", ex.getReason() != null ? ex.getReason() : "No se pudo completar la solicitud.");
        if (codigo == 403) {
            return "error/403";
        }
        if (codigo == 404) {
            return "error/404";
        }
        return "error";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String argumentoInvalido(IllegalArgumentException ex, HttpServletResponse res, Model model) {
        String msg = ex.getMessage() == null ? "Solicitud inválida." : ex.getMessage();
        String lower = msg.toLowerCase();
        if (lower.contains("no encontrad") || lower.contains("inexistente") || lower.contains("no existe")) {
            res.setStatus(HttpStatus.NOT_FOUND.value());
            model.addAttribute("detalle", msg);
            return "error/404";
        }
        res.setStatus(HttpStatus.BAD_REQUEST.value());
        model.addAttribute("codigo", 400);
        model.addAttribute("detalle", msg);
        return "error";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String parametroInvalido(HttpServletResponse res, Model model) {
        res.setStatus(HttpStatus.BAD_REQUEST.value());
        model.addAttribute("codigo", 400);
        model.addAttribute("detalle", "El identificador o parámetro proporcionado no tiene un formato válido.");
        return "error";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String archivoMuyGrande(HttpServletResponse res, Model model) {
        res.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        model.addAttribute("codigo", 413);
        model.addAttribute("detalle", "El archivo adjunto supera el límite máximo permitido (1 MB).");
        return "error";
    }
}