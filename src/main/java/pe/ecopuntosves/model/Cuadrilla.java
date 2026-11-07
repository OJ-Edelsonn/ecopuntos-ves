package pe.ecopuntosves.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cuadrilla")
public class Cuadrilla {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuadrilla") private Integer id;
    @Column(name = "nombre", nullable = false, unique = true, length = 100) private String nombre;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_sector") private Sector sector;
    @Column(name = "activo", nullable = false) private boolean activo = true;
    protected Cuadrilla() {}
    public Cuadrilla(String nombre, Sector sector) { this.nombre=nombre; this.sector=sector; }
    public Integer getId() { return id; }
    public String getNombre() { return nombre; }
    public Sector getSector() { return sector; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo=activo; }
}
