package pe.ecopuntosves.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "canje")
public class Canje {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_canje") private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_vecino", nullable = false) private Vecino vecino;
    @Column(name = "puntos_canjeados", nullable = false) private Integer puntosCanjeados;
    @Column(name = "monto_descuento", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoDescuento;
    @Column(name = "fecha_hora", nullable = false) private LocalDateTime fechaHora;
    @Column(name = "codigo_constancia", nullable = false, unique = true, length = 20)
    private String codigoConstancia;
    @Column(name = "estado", nullable = false, length = 20) private String estado;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_regla", nullable = false) private ReglaPuntos regla;
    @Column(name = "valor_aplicado", nullable = false) private Integer valorAplicado;
    @Column(name = "codigo_contribuyente", nullable = false, length = 20)
    private String codigoContribuyente;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "id_anulador")
    private Usuario anulador;
    @Column(name = "fecha_anulacion") private LocalDateTime fechaAnulacion;
    @Column(name = "motivo_anulacion", length = 300) private String motivoAnulacion;

    protected Canje() {}

    public Canje(Vecino vecino, int puntos, BigDecimal descuento, LocalDateTime fecha,
                 String codigo, ReglaPuntos regla, int valorAplicado, String contribuyente) {
        this.vecino = vecino;
        this.puntosCanjeados = puntos;
        this.montoDescuento = descuento;
        this.fechaHora = fecha;
        this.codigoConstancia = codigo;
        this.regla = regla;
        this.valorAplicado = valorAplicado;
        this.codigoContribuyente = contribuyente;
        this.estado = "EMITIDO";
    }

    public void anular(Usuario funcionario, LocalDateTime fecha, String motivo) {
        if (!"EMITIDO".equals(estado))
            throw new IllegalArgumentException("La constancia ya fue anulada.");
        this.estado = "ANULADO";
        this.anulador = funcionario;
        this.fechaAnulacion = fecha;
        this.motivoAnulacion = motivo;
    }

    public Integer getId() { return id; }
    public Vecino getVecino() { return vecino; }
    public Integer getPuntosCanjeados() { return puntosCanjeados; }
    public BigDecimal getMontoDescuento() { return montoDescuento; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getCodigoConstancia() { return codigoConstancia; }
    public String getEstado() { return estado; }
    public ReglaPuntos getRegla() { return regla; }
    public Integer getValorAplicado() { return valorAplicado; }
    public String getCodigoContribuyente() { return codigoContribuyente; }
    public Usuario getAnulador() { return anulador; }
    public LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
    public String getMotivoAnulacion() { return motivoAnulacion; }
}
