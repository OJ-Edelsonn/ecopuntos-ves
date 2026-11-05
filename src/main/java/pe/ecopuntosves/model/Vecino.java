package pe.ecopuntosves.model;

import jakarta.persistence.*;

@Entity
@Table(name = "vecino")
public class Vecino {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vecino") private Integer id;
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_usuario", unique = true, nullable = false) private Usuario usuario;
    @Column(name = "dni", nullable = false, unique = true, length = 8) private String dni;
    @Column(name = "direccion", nullable = false, length = 150) private String direccion;
    @Column(name = "telefono", length = 15) private String telefono;
    @Column(name = "codigo_contribuyente", length = 20) private String codigoContribuyente;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_sector", nullable = false) private Sector sector;
    protected Vecino() {}
    public Vecino(Usuario usuario, String dni, String direccion, String telefono,
                  String codigoContribuyente, Sector sector) {
        this.usuario = usuario; this.dni = dni; this.direccion = direccion;
        this.telefono = telefono; this.codigoContribuyente = codigoContribuyente; this.sector = sector;
    }
    public Integer getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getDni() { return dni; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCodigoContribuyente() { return codigoContribuyente; }
    public void setCodigoContribuyente(String codigo) { this.codigoContribuyente = codigo; }
    public Sector getSector() { return sector; }
    public void setSector(Sector sector) { this.sector = sector; }
}
