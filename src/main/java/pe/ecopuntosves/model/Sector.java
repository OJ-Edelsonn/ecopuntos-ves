package pe.ecopuntosves.model;

import jakarta.persistence.*;

@Entity
@Table(name = "sector")
public class Sector {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sector") private Integer id;
    @Column(name = "nombre", nullable = false, unique = true, length = 40) private String nombre;
    protected Sector() {}
    public Integer getId() { return id; }
    public String getNombre() { return nombre; }
}
