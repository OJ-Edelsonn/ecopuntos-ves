package pe.ecopuntosves.web.form;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class EntregaForm {
    private String dniVecino;
    private List<Integer> materialIds = new ArrayList<>();
    private List<BigDecimal> kilos = new ArrayList<>();
    public String getDniVecino() { return dniVecino; }
    public void setDniVecino(String dniVecino) { this.dniVecino=dniVecino; }
    public List<Integer> getMaterialIds() { return materialIds; }
    public void setMaterialIds(List<Integer> ids) { this.materialIds=ids; }
    public List<BigDecimal> getKilos() { return kilos; }
    public void setKilos(List<BigDecimal> kilos) { this.kilos=kilos; }
}
