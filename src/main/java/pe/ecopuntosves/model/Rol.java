package pe.ecopuntosves.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rol")
public class Rol {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol") private Integer id;
    @Column(name = "nombre", nullable = false, unique = true, length = 30) private String nombre;
    protected Rol() {}
    public Integer getId() { return id; }
    public String getNombre() { return nombre; }
}
