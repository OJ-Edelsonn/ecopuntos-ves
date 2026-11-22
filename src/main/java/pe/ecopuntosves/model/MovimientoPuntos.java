package pe.ecopuntosves.model;

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
@Table(name = "movimiento_puntos")
public class MovimientoPuntos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vecino", nullable = false)
    private Vecino vecino;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "puntos", nullable = false)
    private Integer puntos;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entrega")
    private Entrega entrega;

    @Column(name = "id_entrega", insertable = false, updatable = false)
    private Integer idEntrega;

    @Column(name = "id_reporte", insertable = false, updatable = false)
    private Integer idReporte;

    @Column(name = "id_canje", insertable = false, updatable = false)
    private Integer idCanje;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_regla")
    private ReglaPuntos regla;

    protected MovimientoPuntos() {
    }

    public MovimientoPuntos(Entrega entrega, Vecino vecino, Usuario operador, int puntos, LocalDateTime fecha) {
        this.entrega = entrega;
        this.vecino = vecino;
        this.usuario = operador;
        this.tipo = "ENTREGA";
        this.puntos = puntos;
        this.fechaHora = fecha;
        this.descripcion = "Entrega de materiales reciclables";
    }

    public Integer getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public Integer getPuntos() {
        return puntos;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public Integer getIdEntrega() {
        return idEntrega;
    }

    public Integer getIdReporte() {
        return idReporte;
    }

    public Integer getIdCanje() {
        return idCanje;
    }

    public String getDescripcion() {
        return descripcion;
    }
}