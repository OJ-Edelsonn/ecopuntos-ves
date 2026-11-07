package pe.ecopuntosves.web.form;

public class ReportePuntoCriticoForm {
    private Integer sectorId;
    private String direccion;
    private String descripcion;
    private String tipoResiduo;
    public Integer getSectorId() { return sectorId; }
    public void setSectorId(Integer sectorId) { this.sectorId = sectorId; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getTipoResiduo() { return tipoResiduo; }
    public void setTipoResiduo(String tipoResiduo) { this.tipoResiduo = tipoResiduo; }
}
