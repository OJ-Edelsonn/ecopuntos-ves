package pe.ecopuntosves.model;

import jakarta.persistence.*;

@Entity
@Table(name = "miembro_cuadrilla")
public class MiembroCuadrilla {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_miembro") private Integer id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_cuadrilla", nullable = false) private Cuadrilla cuadrilla;
    @Column(name = "nombres", nullable = false, length = 80) private String nombres;
    @Column(name = "apellidos", nullable = false, length = 80) private String apellidos;
    @Column(name = "cargo", nullable = false, length = 70) private String cargo;
    @Column(name = "activo", nullable = false) private boolean activo = true;
    protected MiembroCuadrilla() {}
    public MiembroCuadrilla(Cuadrilla cuadrilla,String nombres,String apellidos,String cargo) {
        this.cuadrilla=cuadrilla;this.nombres=nombres;this.apellidos=apellidos;this.cargo=cargo;
    }
    public Integer getId() { return id; }
    public Cuadrilla getCuadrilla() { return cuadrilla; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getNombreCompleto() { return nombres+" "+apellidos; }
    public String getCargo() { return cargo; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo=activo; }
}
