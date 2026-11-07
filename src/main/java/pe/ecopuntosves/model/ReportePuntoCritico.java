package pe.ecopuntosves.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "reporte_punto_critico")
public class ReportePuntoCritico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte") private Integer id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_vecino", nullable = false) private Vecino vecino;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_sector", nullable = false) private Sector sector;
    @Column(name = "direccion_referencia", nullable = false, length = 200) private String direccionReferencia;
    @Column(name = "descripcion", nullable = false, length = 500) private String descripcion;
    @Column(name = "tipo_residuo", nullable = false, length = 60) private String tipoResiduo;
    @Column(name = "foto_url", length = 255) private String fotoUrl;
    @Column(name = "fecha_hora", nullable = false) private LocalDateTime fechaHora;
    @Column(name = "estado", nullable = false, length = 20) private String estado;
    @Column(name = "motivo_rechazo", length = 300) private String motivoRechazo;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "id_validador") private Usuario validador;
    @Column(name = "fecha_validacion") private LocalDateTime fechaValidacion;
    @Column(name = "ubicacion_normalizada", nullable = false, length = 200) private String ubicacionNormalizada;
    @Column(name = "fecha_en_atencion") private LocalDateTime fechaEnAtencion;

    protected ReportePuntoCritico() {}
    public ReportePuntoCritico(Vecino vecino, Sector sector, String direccion, String descripcion,
                              String tipo, String normalizada, LocalDateTime fecha) {
        this.vecino = vecino; this.sector = sector; this.direccionReferencia = direccion;
        this.descripcion = descripcion; this.tipoResiduo = tipo;
        this.ubicacionNormalizada = normalizada; this.fechaHora = fecha; this.estado = "REPORTADO";
    }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public void decidir(boolean aprobar, String motivo, Usuario validador, LocalDateTime fecha) {
        this.estado = aprobar ? "VALIDADO" : "RECHAZADO";
        this.motivoRechazo = aprobar ? null : motivo;
        this.validador = validador; this.fechaValidacion = fecha;
    }
    public void iniciarAtencion(LocalDateTime fecha) { this.estado = "EN_ATENCION"; this.fechaEnAtencion = fecha; }
    public void resolver() { this.estado = "RESUELTO"; }
    public Integer getId() { return id; }
    public Vecino getVecino() { return vecino; }
    public Sector getSector() { return sector; }
    public String getDireccionReferencia() { return direccionReferencia; }
    public String getDescripcion() { return descripcion; }
    public String getTipoResiduo() { return tipoResiduo; }
    public String getFotoUrl() { return fotoUrl; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getEstado() { return estado; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public Usuario getValidador() { return validador; }
    public LocalDateTime getFechaValidacion() { return fechaValidacion; }
    public LocalDateTime getFechaEnAtencion() { return fechaEnAtencion; }
    public String getUbicacionNormalizada() { return ubicacionNormalizada; }
}
