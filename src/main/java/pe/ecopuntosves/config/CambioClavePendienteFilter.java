package pe.ecopuntosves.config;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.ecopuntosves.model.Usuario;
import pe.ecopuntosves.repository.UsuarioRepository;

@Component
public class CambioClavePendienteFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarios;

    public CambioClavePendienteFilter(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String ruta = req.getRequestURI().substring(req.getContextPath().length());
        if (ruta.equals("/logout") || ruta.equals("/login") || ruta.equals("/error")
                || ruta.startsWith("/css/") || ruta.equals("/favicon.ico")) {
            chain.doFilter(req, res);
            return;
        }

        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.isAuthenticated() && !(a instanceof AnonymousAuthenticationToken)) {
            Usuario u = usuarios.findByUsername(a.getName()).orElse(null);

            // Corte inmediato si la cuenta fue eliminada o desactivada (CP-17).
            if (u == null || !u.isActivo()) {
                SecurityContextHolder.clearContext();
                HttpSession sesion = req.getSession(false);
                if (sesion != null) {
                    sesion.invalidate();
                }
                res.sendRedirect(req.getContextPath() + "/login?error");
                return;
            }

            // Exige cambio obligatorio de clave temporal antes de operar.
            if (u.isCambioClavePendiente() && !ruta.equals("/cambiar-clave")) {
                res.sendRedirect(req.getContextPath() + "/cambiar-clave");
                return;
            }
        }

        chain.doFilter(req, res);
    }
}