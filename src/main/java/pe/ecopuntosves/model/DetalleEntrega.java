package pe.ecopuntosves.model;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name = "detalle_entrega")
public class DetalleEntrega {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle") private Integer id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_entrega", nullable = false) private Entrega entrega;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_tipo_material", nullable = false) private TipoMaterial material;
    @Column(name = "kilos", nullable = false, precision = 10, scale = 2) private BigDecimal kilos;
    @Column(name = "puntos", nullable = false) private Integer puntos;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_regla", nullable = false) private ReglaPuntos regla;
    @Column(name = "valor_aplicado", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorAplicado;
    protected DetalleEntrega() {}
    public DetalleEntrega(Entrega e, TipoMaterial m, BigDecimal kilos, int puntos, ReglaPuntos regla) {
        this.entrega=e; this.material=m; this.kilos=kilos;
        this.puntos=puntos; this.regla=regla; this.valorAplicado=regla.getValor();
    }
    public Integer getId() { return id; }
    public TipoMaterial getMaterial() { return material; }
    public BigDecimal getKilos() { return kilos; }
    public Integer getPuntos() { return puntos; }
    public ReglaPuntos getRegla() { return regla; }
    public BigDecimal getValorAplicado() { return valorAplicado; }
}
