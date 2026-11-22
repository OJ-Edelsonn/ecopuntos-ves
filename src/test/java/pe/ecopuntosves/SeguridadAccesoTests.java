package pe.ecopuntosves;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SeguridadAccesoTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void rutasPublicasRespondenOkYProtegidasRedirigenALogin() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/materiales")).andExpect(status().isOk());

        mvc.perform(get("/entregas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/reglas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "vecino001", roles = "VECINO")
    void vecinoAccedeASusVistasYSeBloqueanRutasDePersonalYEntregasAjenas() throws Exception {
        mvc.perform(get("/mi-perfil")).andExpect(status().isOk());
        mvc.perform(get("/mis-puntos")).andExpect(status().isOk());
        mvc.perform(get("/mis-entregas")).andExpect(status().isOk());
        mvc.perform(get("/canjes")).andExpect(status().isOk());
        mvc.perform(get("/puntos-criticos/mis-reportes")).andExpect(status().isOk());

        // Rutas de otros roles -> 403
        mvc.perform(get("/usuarios")).andExpect(status().isForbidden());
        mvc.perform(get("/reglas")).andExpect(status().isForbidden());
        mvc.perform(get("/entregas")).andExpect(status().isForbidden());
        mvc.perform(get("/reportes")).andExpect(status().isForbidden());
        mvc.perform(get("/cuadrillas")).andExpect(status().isForbidden());

        // Entrega #1 pertenece al vecino 232 (ajeno a vecino001) -> 403 (CP-20)
        mvc.perform(get("/mis-entregas/1")).andExpect(status().isForbidden());

        // POST sin token CSRF -> 403 (CP-31)
        mvc.perform(post("/canjes/emitir").param("puntos", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "operador01", roles = "OPERADOR")
    void operadorSoloAccedeAVecinosYEntregas() throws Exception {
        mvc.perform(get("/vecinos")).andExpect(status().isOk());
        mvc.perform(get("/entregas")).andExpect(status().isOk());

        mvc.perform(get("/usuarios")).andExpect(status().isForbidden());
        mvc.perform(get("/reglas")).andExpect(status().isForbidden());
        mvc.perform(get("/reportes")).andExpect(status().isForbidden());
        mvc.perform(get("/canjes")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "funcionario01", roles = "FUNCIONARIO")
    void funcionarioConsultaCatalogosYReportesPeroNoAdministraCuadrillasNiReglas() throws Exception {
        mvc.perform(get("/puntos-criticos")).andExpect(status().isOk());
        mvc.perform(get("/canjes/administrar")).andExpect(status().isOk());
        mvc.perform(get("/cuadrillas")).andExpect(status().isOk());
        mvc.perform(get("/reportes")).andExpect(status().isOk());

        mvc.perform(get("/reglas")).andExpect(status().isForbidden());
        mvc.perform(get("/usuarios")).andExpect(status().isForbidden());
        mvc.perform(post("/cuadrillas/nueva").with(csrf()).param("nombre", "No permitida"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMINISTRADOR")
    void administradorAccedeAReglasPersonalCuadrillasYSupervision() throws Exception {
        mvc.perform(get("/usuarios")).andExpect(status().isOk());
        mvc.perform(get("/reglas")).andExpect(status().isOk());
        mvc.perform(get("/cuadrillas")).andExpect(status().isOk());
        mvc.perform(get("/reportes")).andExpect(status().isOk());
        mvc.perform(get("/vecinos")).andExpect(status().isOk());
        mvc.perform(get("/entregas")).andExpect(status().isOk());

        // Acciones exclusivas de vecino o funcionario -> 403
        mvc.perform(get("/canjes")).andExpect(status().isForbidden());
        mvc.perform(post("/puntos-criticos/1/decidir").with(csrf()).param("decision", "VALIDADO"))
                .andExpect(status().isForbidden());
    }
}