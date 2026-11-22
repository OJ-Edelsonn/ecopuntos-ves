package pe.ecopuntosves.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.ecopuntosves.model.ReglaPuntos;
import pe.ecopuntosves.model.TipoMaterial;
import pe.ecopuntosves.repository.ReglaPuntosRepository;
import pe.ecopuntosves.repository.TipoMaterialRepository;

@Service
public class ReglasMaterialesService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final BigDecimal MAX_VALOR = new BigDecimal("10000.00");

    private final EntityManager em;
    private final TipoMaterialRepository materiales;
    private final ReglaPuntosRepository reglas;

    public ReglasMaterialesService(EntityManager em,
                                   TipoMaterialRepository materiales,
                                   ReglaPuntosRepository reglas) {
        this.em = em;
        this.materiales = materiales;
        this.reglas = reglas;
    }

    public record MaterialConRegla(TipoMaterial material, ReglaPuntos reglaVigente) {}

    private LocalDateTime ahora() {
        return LocalDateTime.now(LIMA).withNano(0);
    }

    private BigDecimal parsearValor(String texto, boolean soloEntero, String etiqueta) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Ingresa un valor válido para " + etiqueta + ".");
        }
        BigDecimal valor;
        try {
            valor = new BigDecimal(texto.trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El valor de " + etiqueta + " debe ser numérico.");
        }
        if (valor.signum() <= 0 || valor.compareTo(MAX_VALOR) > 0) {
            throw new IllegalArgumentException("El valor de " + etiqueta + " debe ser mayor a 0 y hasta 10000.");
        }
        if (soloEntero) {
            if (valor.stripTrailingZeros().scale() > 0) {
                throw new IllegalArgumentException("El valor de " + etiqueta + " debe ser un número entero positivo.");
            }
            return valor.setScale(2, RoundingMode.UNNECESSARY);
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("El valor de " + etiqueta + " admite como máximo 2 decimales.");
        }
        return valor.setScale(2, RoundingMode.UNNECESSARY);
    }

    @Transactional(readOnly = true)
    public List<MaterialConRegla> listarMaterialesConRegla() {
        List<TipoMaterial> todos = materiales.findAllByOrderByNombreAsc();
        List<MaterialConRegla> resultado = new ArrayList<>(todos.size());
        for (TipoMaterial m : todos) {
            ReglaPuntos vigente = reglas.findByConceptoAndMaterial_IdAndVigenteTrue("RECICLAJE", m.getId())
                    .orElse(null);
            resultado.add(new MaterialConRegla(m, vigente));
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public ReglaPuntos reglaGlobalVigente(String concepto) {
        return reglas.findByConceptoAndMaterialIsNullAndVigenteTrue(concepto).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ReglaPuntos> historialReglas() {
        return reglas.findAllByOrderByVigenteDescFechaInicioDescIdDesc();
    }

    @Transactional
    public void crearMaterial(String nombre, String valorTexto) {
        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");
        if (limpio.isBlank() || limpio.length() > 40) {
            throw new IllegalArgumentException("El nombre del material es obligatorio (máximo 40 caracteres).");
        }
        if (materiales.existsByNombreIgnoreCase(limpio)) {
            throw new IllegalArgumentException("Ya existe un material registrado con ese nombre.");
        }
        BigDecimal valor = parsearValor(valorTexto, false, "la tarifa inicial (pts/kg)");

        TipoMaterial nuevo = materiales.save(new TipoMaterial(limpio));
        em.flush();

        reglas.save(new ReglaPuntos("RECICLAJE", nuevo, valor, ahora()));
        em.flush();
    }

    @Transactional
    public void cambiarEstadoMaterial(int id, boolean activo) {
        TipoMaterial m = materiales.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Material no encontrado."));
        if (activo && reglas.findByConceptoAndMaterial_IdAndVigenteTrue("RECICLAJE", id).isEmpty()) {
            throw new IllegalArgumentException("Define primero una tarifa vigente antes de activar este material.");
        }
        m.setActivo(activo);
        em.flush();
    }

    @Transactional
    public void actualizarTarifaMaterial(int materialId, String valorTexto) {
        TipoMaterial material = em.find(TipoMaterial.class, materialId, LockModeType.PESSIMISTIC_WRITE);
        if (material == null) {
            throw new IllegalArgumentException("Material no encontrado.");
        }
        BigDecimal nuevoValor = parsearValor(valorTexto, false, "la tarifa de " + material.getNombre());

        LocalDateTime fecha = ahora();
        Optional<ReglaPuntos> actualOpt = reglas.bloquearReciclajeVigente(materialId);
        if (actualOpt.isPresent()) {
            ReglaPuntos actual = actualOpt.get();
            if (actual.getValor().compareTo(nuevoValor) == 0) {
                throw new IllegalArgumentException("La nueva tarifa debe ser distinta a la vigente (" + actual.getValor() + " pts/kg).");
            }
            LocalDateTime cierre = fecha.isBefore(actual.getFechaInicio()) ? actual.getFechaInicio() : fecha;
            actual.cerrar(cierre);
            // Flush obligatorio antes del INSERT para liberar uq_regla_vigente en MariaDB.
            em.flush();
        }

        reglas.save(new ReglaPuntos("RECICLAJE", material, nuevoValor, fecha));
        em.flush();
    }

    @Transactional
    public void actualizarReglaGlobal(String concepto, String valorTexto) {
        String normalizado = concepto == null ? "" : concepto.trim().toUpperCase(Locale.ROOT);
        if (!"REPORTE".equals(normalizado) && !"CANJE".equals(normalizado)) {
            throw new IllegalArgumentException("Concepto de regla inválido.");
        }
        String etiqueta = "REPORTE".equals(normalizado) ? "puntos por reporte validado" : "equivalencia de canje";
        BigDecimal nuevoValor = parsearValor(valorTexto, true, etiqueta);

        LocalDateTime fecha = ahora();
        Optional<ReglaPuntos> actualOpt = reglas.bloquearGlobalVigente(normalizado);
        if (actualOpt.isPresent()) {
            ReglaPuntos actual = actualOpt.get();
            if (actual.getValor().compareTo(nuevoValor) == 0) {
                throw new IllegalArgumentException("El nuevo valor debe ser distinto al vigente actual.");
            }
            LocalDateTime cierre = fecha.isBefore(actual.getFechaInicio()) ? actual.getFechaInicio() : fecha;
            actual.cerrar(cierre);
            // Flush obligatorio antes del INSERT para liberar uq_regla_vigente en MariaDB.
            em.flush();
        }

        reglas.save(new ReglaPuntos(normalizado, null, nuevoValor, fecha));
        em.flush();
    }
}