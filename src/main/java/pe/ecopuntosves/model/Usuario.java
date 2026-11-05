package pe.ecopuntosves.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario") private Integer id;
    @Column(name = "username", nullable = false, unique = true, length = 40) private String username;
    @Column(name = "password_hash", nullable = false, length = 100) private String passwordHash;
    @Column(name = "nombres", nullable = false, length = 80) private String nombres;
    @Column(name = "apellidos", nullable = false, length = 80) private String apellidos;
    @Column(name = "correo", length = 100) private String correo;
    @Column(name = "activo", nullable = false) private boolean activo = true;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_rol", nullable = false) private Rol rol;
    @Column(name = "cambio_clave_pendiente", nullable = false) private boolean cambioClavePendiente;
    protected Usuario() {}
    public Usuario(String username, String passwordHash, String nombres, String apellidos,
                   String correo, Rol rol) {
        this.username = username; this.passwordHash = passwordHash;
        this.nombres = nombres; this.apellidos = apellidos;
        this.correo = correo; this.rol = rol;
        this.activo = true; this.cambioClavePendiente = true;
    }
    public Integer getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getNombreCompleto() { return nombres + " " + apellidos; }
    public String getCorreo() { return correo; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Rol getRol() { return rol; }
    public boolean isCambioClavePendiente() { return cambioClavePendiente; }
    public void setCambioClavePendiente(boolean pendiente) { this.cambioClavePendiente = pendiente; }
}
