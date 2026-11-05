package pe.ecopuntosves.config;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.ecopuntosves.model.Usuario;
import pe.ecopuntosves.repository.UsuarioRepository;
@Service
public class AutenticacionUsuariosService implements UserDetailsService {
    private final UsuarioRepository usuarios;
    public AutenticacionUsuariosService(UsuarioRepository usuarios) { this.usuarios=usuarios; }
    @Override @Transactional(readOnly=true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario u=usuarios.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Cuenta inexistente"));
        return User.withUsername(u.getUsername()).password(u.getPasswordHash())
            .roles(u.getRol().getNombre()).disabled(!u.isActivo()).build();
    }
}
