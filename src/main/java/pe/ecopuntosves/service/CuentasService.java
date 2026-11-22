package pe.ecopuntosves.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.ecopuntosves.model.Rol;
import pe.ecopuntosves.model.Sector;
import pe.ecopuntosves.model.Usuario;
import pe.ecopuntosves.model.Vecino;
import pe.ecopuntosves.repository.RolRepository;
import pe.ecopuntosves.repository.SectorRepository;
import pe.ecopuntosves.repository.UsuarioRepository;
import pe.ecopuntosves.repository.VecinoRepository;
import pe.ecopuntosves.web.form.PersonalForm;
import pe.ecopuntosves.web.form.VecinoForm;

@Service
public class CuentasService {

    private static final String CLAVES = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#";
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final VecinoRepository vecinos;
    private final RolRepository roles;
    private final SectorRepository sectores;
    private final PasswordEncoder encoder;

    public CuentasService(UsuarioRepository usuarios, VecinoRepository vecinos,
                          RolRepository roles, SectorRepository sectores, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.vecinos = vecinos;
        this.roles = roles;
        this.sectores = sectores;
        this.encoder = encoder;
    }

    private static String limpiar(String x) {
        return x == null ? "" : x.trim();
    }

    private static String opcional(String x) {
        String a = limpiar(x);
        return a.isEmpty() ? null : a;
    }

    private static String campo(String x, int max, String nombre) {
        String a = limpiar(x);
        if (a.isEmpty() || a.length() > max) {
            throw new IllegalArgumentException(nombre + " inválido (máximo " + max + " caracteres).");
        }
        return a;
    }

    private static String claveTemporal() {
        StringBuilder clave = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            clave.append(CLAVES.charAt(ALEATORIO.nextInt(CLAVES.length())));
        }
        return clave.toString();
    }

    private static void maxOpcional(String x, int max, String campo) {
        if (x != null && x.length() > max) {
            throw new IllegalArgumentException(campo + " excede " + max + " caracteres.");
        }
    }

    @Transactional(readOnly = true)
    public List<Sector> listarSectores() {
        return sectores.findAll(Sort.by("nombre"));
    }

    @Transactional(readOnly = true)
    public Page<Vecino> buscarVecinos(String q, int pagina) {
        PageRequest pg = PageRequest.of(Math.max(0, Math.min(10000, pagina)), 15, Sort.by("id").ascending());
        String texto = limpiar(q);
        return texto.isEmpty() ? vecinos.findAll(pg) : vecinos.buscar(texto, pg);
    }

    @Transactional(readOnly = true)
    public Vecino obtenerVecino(int id) {
        return vecinos.findById(id).orElseThrow(() -> new IllegalArgumentException("Vecino inexistente."));
    }

    @Transactional(readOnly = true)
    public Vecino obtenerPerfil(String username) {
        return vecinos.findByUsuario_Username(username)
                .orElseThrow(() -> new IllegalArgumentException("No existe un perfil de vecino para esta cuenta."));
    }

    @Transactional
    public String registrarVecino(VecinoForm f) {
        String dni = campo(f.getDni(), 8, "DNI");
        if (!dni.matches("[0-9]{8}")) {
            throw new IllegalArgumentException("El DNI debe tener 8 dígitos.");
        }
        if (vecinos.existsByDni(dni)) {
            throw new IllegalArgumentException("Este DNI ya se encuentra registrado.");
        }
        String nombres = campo(f.getNombres(), 80, "Nombres");
        String apellidos = campo(f.getApellidos(), 80, "Apellidos");
        String direccion = campo(f.getDireccion(), 150, "Dirección");
        String telefono = opcional(f.getTelefono());
        String codigo = opcional(f.getCodigoContribuyente());
        maxOpcional(telefono, 15, "Teléfono");
        maxOpcional(codigo, 20, "Código de contribuyente");
        Sector sector = sectores.findById(f.getSectorId() == null ? -1 : f.getSectorId())
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un sector válido."));
        String username = "v" + dni;
        if (usuarios.existsByUsername(username)) {
            throw new IllegalArgumentException("Ya existe una cuenta para ese DNI.");
        }
        String clave = claveTemporal();
        Rol rol = roles.findByNombre("VECINO")
                .orElseThrow(() -> new IllegalStateException("Falta el rol VECINO en la base de datos."));
        Usuario nuevo = usuarios.save(new Usuario(username, encoder.encode(clave), nombres, apellidos, null, rol));
        vecinos.save(new Vecino(nuevo, dni, direccion, telefono, codigo, sector));
        return username + "|" + clave;
    }

    @Transactional
    public void editarVecino(int id, VecinoForm f) {
        Vecino v = obtenerVecino(id);
        String direccion = campo(f.getDireccion(), 150, "Dirección");
        String telefono = opcional(f.getTelefono());
        String codigo = opcional(f.getCodigoContribuyente());
        maxOpcional(telefono, 15, "Teléfono");
        maxOpcional(codigo, 20, "Código de contribuyente");
        Sector sector = sectores.findById(f.getSectorId() == null ? -1 : f.getSectorId())
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un sector válido."));
        v.setDireccion(direccion);
        v.setTelefono(telefono);
        v.setCodigoContribuyente(codigo);
        v.setSector(sector);
    }

    @Transactional(readOnly = true)
    public Page<Usuario> listarPersonal(int pagina) {
        return usuarios.findByRol_NombreIn(
                List.of("ADMINISTRADOR", "OPERADOR", "FUNCIONARIO"),
                PageRequest.of(Math.max(0, Math.min(10000, pagina)), 15, Sort.by("id")));
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPersonal(int id) {
        Usuario u = usuarios.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta de personal no encontrada."));
        if ("VECINO".equals(u.getRol().getNombre())) {
            throw new IllegalArgumentException("Esta cuenta pertenece a un vecino y se gestiona desde Gestión de vecinos.");
        }
        return u;
    }

    @Transactional
    public String registrarPersonal(PersonalForm f) {
        String username = campo(f.getUsername(), 40, "Usuario").toLowerCase(Locale.ROOT);
        if (!username.matches("[a-z][a-z0-9._-]{3,39}")) {
            throw new IllegalArgumentException("Usuario: 4 a 40 caracteres, letras, números, punto, guion o guion bajo.");
        }
        if (usuarios.existsByUsername(username)) {
            throw new IllegalArgumentException("El nombre de usuario ya existe.");
        }
        String nombre = campo(f.getNombres(), 80, "Nombres");
        String apellido = campo(f.getApellidos(), 80, "Apellidos");
        String correo = opcional(f.getCorreo());
        maxOpcional(correo, 100, "Correo");
        if (correo != null && !correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Correo no válido.");
        }
        String nombreRol = limpiar(f.getRol());
        if (!List.of("OPERADOR", "FUNCIONARIO").contains(nombreRol)) {
            throw new IllegalArgumentException("Solo se pueden crear operadores y funcionarios.");
        }
        Rol rol = roles.findByNombre(nombreRol)
                .orElseThrow(() -> new IllegalStateException("Falta el rol " + nombreRol));
        String clave = claveTemporal();
        usuarios.save(new Usuario(username, encoder.encode(clave), nombre, apellido, correo, rol));
        return username + "|" + clave;
    }

    @Transactional
    public void editarPersonal(int id, PersonalForm f) {
        Usuario u = obtenerPersonal(id);
        String nombre = campo(f.getNombres(), 80, "Nombres");
        String apellido = campo(f.getApellidos(), 80, "Apellidos");
        String correo = opcional(f.getCorreo());
        maxOpcional(correo, 100, "Correo");
        if (correo != null && !correo.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Correo no válido.");
        }

        if (!"ADMINISTRADOR".equals(u.getRol().getNombre())) {
            String nombreRol = limpiar(f.getRol());
            if (!List.of("OPERADOR", "FUNCIONARIO").contains(nombreRol)) {
                throw new IllegalArgumentException("Selecciona un rol válido (OPERADOR o FUNCIONARIO).");
            }
            Rol rol = roles.findByNombre(nombreRol)
                    .orElseThrow(() -> new IllegalStateException("Falta el rol " + nombreRol));
            u.setRol(rol);
        }

        u.setNombres(nombre);
        u.setApellidos(apellido);
        u.setCorreo(correo);
    }

    @Transactional
    public void cambiarEstadoPersonal(int id, String adminActual) {
        Usuario u = usuarios.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La cuenta no existe."));
        if ("VECINO".equals(u.getRol().getNombre())) {
            throw new IllegalArgumentException("Esta acción es solo para personal.");
        }
        if (u.getUsername().equals(adminActual)) {
            throw new IllegalArgumentException("No puedes desactivar tu propia cuenta.");
        }
        if (u.isActivo() && "ADMINISTRADOR".equals(u.getRol().getNombre())
                && usuarios.countByRol_NombreAndActivoTrue("ADMINISTRADOR") <= 1) {
            throw new IllegalArgumentException("Debe permanecer al menos un administrador activo.");
        }
        u.setActivo(!u.isActivo());
    }

    @Transactional
    public void cambiarClave(String username, String actual, String nueva, String confirmacion) {
        Usuario u = usuarios.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada."));
        if (!encoder.matches(actual == null ? "" : actual, u.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual es incorrecta.");
        }
        if (nueva == null || nueva.length() < 10 || nueva.length() > 72
                || !nueva.matches("(?s).*\\p{L}.*") || !nueva.matches("(?s).*\\d.*")) {
            throw new IllegalArgumentException("La nueva contraseña requiere entre 10 y 72 caracteres, letras y números.");
        }
        if (!nueva.equals(confirmacion)) {
            throw new IllegalArgumentException("Las contraseñas nuevas no coinciden.");
        }
        if (encoder.matches(nueva, u.getPasswordHash())) {
            throw new IllegalArgumentException("Debes elegir una contraseña diferente.");
        }
        u.setPasswordHash(encoder.encode(nueva));
        u.setCambioClavePendiente(false);
    }
}