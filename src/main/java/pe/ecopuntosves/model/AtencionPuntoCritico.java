package pe.ecopuntosves.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "atencion_punto_critico")
public class AtencionPuntoCritico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_atencion") private Integer id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_reporte", nullable = false, unique = true) private ReportePuntoCritico reporte;
    @Column(name = "responsable", nullable = false, length = 100) private String responsable;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cuadrilla") private Cuadrilla cuadrilla;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_miembro") private MiembroCuadrilla miembro;
    @Column(name = "fecha_programada", nullable = false) private LocalDate fechaProgramada;
    @Column(name = "fecha_resolucion") private LocalDateTime fechaResolucion;
    @Column(name = "observacion", length = 300) private String observacion;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_funcionario", nullable = false) private Usuario funcionario;

    protected AtencionPuntoCritico() {}
    public AtencionPuntoCritico(ReportePuntoCritico reporte, String responsable,
                               LocalDate fechaProgramada, Usuario funcionario) {
        this.reporte = reporte; this.responsable = responsable;
        this.fechaProgramada = fechaProgramada; this.funcionario = funcionario;
    }
    public void asignarCatalogo(Cuadrilla cuadrilla, MiembroCuadrilla miembro) {
        this.cuadrilla = cuadrilla; this.miembro = miembro;
    }
    public Cuadrilla getCuadrilla() { return cuadrilla; }
    public MiembroCuadrilla getMiembro() { return miembro; }
    public void resolver(LocalDateTime fecha, String observacion, Usuario funcionario) {
        this.fechaResolucion = fecha; this.observacion = observacion; this.funcionario = funcionario;
    }
    public Integer getId() { return id; }
    public String getResponsable() { return responsable; }
    public LocalDate getFechaProgramada() { return fechaProgramada; }
    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
    public String getObservacion() { return observacion; }
    public Usuario getFuncionario() { return funcionario; }
}
