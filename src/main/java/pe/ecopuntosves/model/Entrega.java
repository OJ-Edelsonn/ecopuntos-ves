package pe.ecopuntosves.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "entrega")
public class Entrega {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrega") private Integer id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_vecino", nullable = false) private Vecino vecino;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_operador", nullable = false) private Usuario operador;
    @Column(name = "fecha_hora", nullable = false) private LocalDateTime fechaHora;
    @Column(name = "kilos_total", nullable = false, precision = 10, scale = 2) private BigDecimal kilosTotal;
    @Column(name = "puntos_otorgados", nullable = false) private Integer puntosOtorgados;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_sector", nullable = false) private Sector sector;
    protected Entrega() {}
    public Entrega(Vecino v, Usuario operador, LocalDateTime fechaHora,
                   BigDecimal kilos, int puntos) {
        this.vecino=v; this.operador=operador; this.fechaHora=fechaHora;
        this.kilosTotal=kilos; this.puntosOtorgados=puntos;
        this.sector=v.getSector(); // Copia historica del sector al registrar.
    }
    public Integer getId() { return id; }
    public Vecino getVecino() { return vecino; }
    public Usuario getOperador() { return operador; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public BigDecimal getKilosTotal() { return kilosTotal; }
    public Integer getPuntosOtorgados() { return puntosOtorgados; }
    public Sector getSector() { return sector; }
}
