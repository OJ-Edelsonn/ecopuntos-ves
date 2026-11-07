package pe.ecopuntosves.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "regla_puntos")
public class ReglaPuntos {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regla") private Integer id;
    @Column(name = "concepto", nullable = false, length = 20) private String concepto;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "id_tipo_material") private TipoMaterial material;
    @Column(name = "valor", nullable = false, precision = 10, scale = 2) private BigDecimal valor;
    @Column(name = "vigente", nullable = false) private boolean vigente;
    @Column(name = "fecha_inicio", nullable = false) private LocalDateTime fechaInicio;
    @Column(name = "fecha_fin") private LocalDateTime fechaFin;
    protected ReglaPuntos() {}
    public Integer getId() { return id; }
    public String getConcepto() { return concepto; }
    public TipoMaterial getMaterial() { return material; }
    public BigDecimal getValor() { return valor; }
    public boolean isVigente() { return vigente; }
}
