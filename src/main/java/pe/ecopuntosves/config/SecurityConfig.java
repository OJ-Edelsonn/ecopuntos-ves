package pe.ecopuntosves.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    // Evita que Spring Boot ejecute el filtro fuera de la cadena Spring Security.
    @Bean
    public FilterRegistrationBean<CambioClavePendienteFilter> deshabilitarRegistroServlet(
            CambioClavePendienteFilter filtro) {
        FilterRegistrationBean<CambioClavePendienteFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setEnabled(false);
        return registro;
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService,
                                                               PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider(userDetailsService);
        p.setPasswordEncoder(passwordEncoder);
        return p;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CambioClavePendienteFilter filtro,
                                           DaoAuthenticationProvider provider) throws Exception {
        http.authenticationProvider(provider)
        .authorizeHttpRequests(auth -> auth
        	    .requestMatchers("/", "/css/**", "/materiales", "/materiales/**").permitAll()
        	    .requestMatchers("/vecinos", "/vecinos/**").hasAnyRole("OPERADOR", "ADMINISTRADOR")
        	    .requestMatchers("/usuarios", "/usuarios/**").hasRole("ADMINISTRADOR")
        	    .requestMatchers("/mi-perfil").hasRole("VECINO")
        	    .requestMatchers("/cambiar-clave").authenticated()

        	    // Módulo 03: Entregas
        	    .requestMatchers("/entregas", "/entregas/**")
        	        .hasAnyRole("OPERADOR", "ADMINISTRADOR")
        	    .requestMatchers("/mis-entregas")
        	        .hasRole("VECINO")
        	        
        	     // Módulo 04: Gestión de puntos críticos
        	        .requestMatchers(
        	            org.springframework.http.HttpMethod.POST,
        	            "/puntos-criticos/*/decidir",
        	            "/puntos-criticos/*/iniciar-atencion",
        	            "/puntos-criticos/*/resolver"
        	        ).hasRole("FUNCIONARIO")

        	        .requestMatchers(
        	            "/puntos-criticos/nuevo",
        	            "/puntos-criticos/mis-reportes"
        	        ).hasRole("VECINO")

        	        .requestMatchers(
        	            "/puntos-criticos",
        	            "/puntos-criticos/**"
        	        ).hasAnyRole("VECINO", "FUNCIONARIO", "ADMINISTRADOR")
        	        
        	     // Módulo 05: Canjes
        	        .requestMatchers(
        	            org.springframework.http.HttpMethod.POST,
        	            "/canjes/*/anular"
        	        ).hasRole("FUNCIONARIO")

        	        .requestMatchers(
        	            org.springframework.http.HttpMethod.POST,
        	            "/canjes/emitir"
        	        ).hasRole("VECINO")

        	        .requestMatchers("/canjes/administrar")
        	            .hasRole("FUNCIONARIO")

        	        .requestMatchers("/canjes")
        	            .hasRole("VECINO")

        	        .requestMatchers("/canjes/**")
        	            .hasAnyRole("VECINO", "FUNCIONARIO")
        	        
        	         // Módulo 06: Reportes y estadísticas
        	            .requestMatchers("/reportes", "/reportes/**")
        	                .hasAnyRole("FUNCIONARIO", "ADMINISTRADOR")
        	            
        	             // Consolidación 03: Cuadrillas municipales
        	                .requestMatchers(
        	                    org.springframework.http.HttpMethod.POST,
        	                    "/cuadrillas/**"
        	                ).hasRole("ADMINISTRADOR")

        	                .requestMatchers("/cuadrillas", "/cuadrillas/**")
        	                    .hasAnyRole("FUNCIONARIO", "ADMINISTRADOR")       
        	                
        	    .anyRequest().authenticated()
        	)
            .formLogin(form -> form.defaultSuccessUrl("/", false).permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/"));
        http.addFilterAfter(filtro, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
