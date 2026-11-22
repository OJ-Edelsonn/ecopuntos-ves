package pe.ecopuntosves.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "regla_puntos")
public class ReglaPuntos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regla")
    private Integer id;

    @Column(name = "concepto", nullable = false, length = 20)
    private String concepto;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo_material")
    private TipoMaterial material;

    @Column(name = "valor", nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "vigente", nullable = false)
    private boolean vigente;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    protected ReglaPuntos() {
    }

    public ReglaPuntos(String concepto, TipoMaterial material, BigDecimal valor, LocalDateTime fechaInicio) {
        this.concepto = concepto;
        this.material = material;
        this.valor = valor;
        this.vigente = true;
        this.fechaInicio = fechaInicio;
        this.fechaFin = null;
    }

    public void cerrar(LocalDateTime cierre) {
        this.vigente = false;
        this.fechaFin = cierre;
    }

    public Integer getId() {
        return id;
    }

    public String getConcepto() {
        return concepto;
    }

    public TipoMaterial getMaterial() {
        return material;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public boolean isVigente() {
        return vigente;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }
}