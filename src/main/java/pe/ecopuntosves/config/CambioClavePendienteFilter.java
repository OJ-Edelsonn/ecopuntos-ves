package pe.ecopuntosves.config;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.ecopuntosves.repository.UsuarioRepository;
@Component
public class CambioClavePendienteFilter extends OncePerRequestFilter {
    private final UsuarioRepository usuarios;
    public CambioClavePendienteFilter(UsuarioRepository usuarios) { this.usuarios=usuarios; }
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
        String ruta=req.getRequestURI().substring(req.getContextPath().length());
        if (ruta.equals("/cambiar-clave") || ruta.equals("/logout") || ruta.equals("/login")
            || ruta.equals("/error") || ruta.startsWith("/css/") || ruta.equals("/favicon.ico")) {
            chain.doFilter(req,res); return;
        }
        Authentication a=SecurityContextHolder.getContext().getAuthentication();
        if(a!=null && a.isAuthenticated() && !(a instanceof AnonymousAuthenticationToken)) {
            boolean pendiente=usuarios.findByUsername(a.getName())
                .map(u -> u.isCambioClavePendiente()).orElse(false);
            if(pendiente) { res.sendRedirect(req.getContextPath()+"/cambiar-clave"); return; }
        }
        chain.doFilter(req,res);
    }
}
