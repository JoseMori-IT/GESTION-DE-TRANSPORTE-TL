package com.palmas.transporte.plataforma.seguridad;
import com.palmas.transporte.modulos.identidad.infrastructure.CambioContrasenaFilter;
import com.palmas.transporte.modulos.identidad.infrastructure.UsuarioActivoJwtValidator;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.palmas.transporte.modulos.identidad.web.ActividadUsuarioFilter;
import com.palmas.transporte.modulos.identidad.application.JwtService;
import com.palmas.transporte.modulos.identidad.persistence.UsuarioRepository;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtConverter,
            ActividadUsuarioFilter actividadUsuarioFilter,
            CorsConfigurationSource corsConfigurationSource,
            UsuarioRepository usuarioRepository)
            throws Exception {

        http
            /*
             * La aplicación utiliza JWT enviado mediante
             * Authorization y no autenticación por cookies.
             */
            .csrf(csrf -> csrf.disable())

            .cors(cors -> cors.configurationSource(
                corsConfigurationSource
            ))

            /*
             * El servidor no mantiene sesiones HTTP.
             * Cada solicitud debe presentar su JWT.
             */
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/renovar").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/auth/cambiar-contrasena")
                .authenticated()
                .requestMatchers(HttpMethod.POST, "/api/usuarios/*/restablecer-contrasena")
                .hasAnyRole("MASTER", "COORDINADOR", "ADMINISTRADOR")

                /*
                 * Preflight de CORS.
                 */
                .requestMatchers(
                    HttpMethod.OPTIONS,
                    "/api/**"
                )
                .permitAll()

                /*
                 * Inicio de sesión público.
                 */
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/login"
                )
                .permitAll()

                /*
                 * Estado básico de la aplicación.
                 */
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/estado"
                )
                .permitAll()

                .requestMatchers("/error")
                .permitAll()

                /*
                 * Cierre de sesión con token válido.
                 */
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/logout"
                )
                .authenticated()

                /*
                 * Perfil del usuario autenticado.
                 */
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/perfil"
                )
                .authenticated()

                /*
                 * Operaciones exclusivas del chofer.
                 */
                .requestMatchers("/api/chofer/**")
                .hasRole("CHOFER")

                .requestMatchers(HttpMethod.GET,"/api/configuracion-operativa").authenticated()
                .requestMatchers(HttpMethod.PUT,"/api/configuracion-operativa").hasAnyRole("MASTER","ADMINISTRADOR","COORDINADOR")
                .requestMatchers(HttpMethod.GET,"/api/abastecimientos/**").hasAnyRole("MASTER","COORDINADOR","ADMINISTRADOR","OPERADOR","CONSULTA")
                .requestMatchers("/api/abastecimientos/*/anular").hasAnyRole("MASTER","COORDINADOR","ADMINISTRADOR")
                .requestMatchers("/api/abastecimientos/**").hasAnyRole("MASTER","COORDINADOR","ADMINISTRADOR","OPERADOR")

                /*
                 * Consulta de mantenimiento.
                 */
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/mantenimientos/**"
                )
                .hasAnyRole(
                    "MASTER",
                    "COORDINADOR",
                    "ADMINISTRADOR",
                    "OPERADOR",
                    "CONSULTA"
                )

                /*
                 * Gestión de mantenimiento.
                 */
                .requestMatchers(
                    "/api/mantenimientos/**"
                )
                .hasAnyRole(
                    "MASTER",
                    "COORDINADOR",
                    "ADMINISTRADOR",
                    "OPERADOR"
                )

                /*
                 * Auditoría exclusivamente de lectura.
                 */
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/auditoria/**"
                )
                .hasRole("MASTER")

                /*
                 * No se permite modificar ni eliminar
                 * registros de auditoría.
                 */
                .requestMatchers(
                    "/api/auditoria/**"
                )
                .denyAll()

                /*
                 * Administración de usuarios.
                 */
                .requestMatchers(
                    "/api/usuarios/**",
                    "/api/personal-administrativo/**"
                )
                .hasAnyRole("MASTER", "COORDINADOR", "ADMINISTRADOR")

                .requestMatchers(HttpMethod.POST, "/api/incidencias-viaje/**")
                .hasAnyRole("MASTER", "COORDINADOR", "ADMINISTRADOR", "OPERADOR", "CHOFER")
                .requestMatchers("/api/incidencias-viaje/**").denyAll()

                .requestMatchers(HttpMethod.GET, "/api/validaciones-viaje/**")
                .hasAnyRole("MASTER", "COORDINADOR", "ADMINISTRADOR", "OPERADOR", "CONSULTA")
                .requestMatchers(HttpMethod.POST, "/api/validaciones-viaje/**")
                .hasAnyRole("MASTER", "COORDINADOR", "ADMINISTRADOR", "OPERADOR")
                .requestMatchers("/api/validaciones-viaje/**").denyAll()

                /*
                 * Consultas operativas.
                 */
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/vehiculos/**",
                    "/api/conductores/**",
                    "/api/rutas/**",
                    "/api/ubicaciones/**",
                    "/api/servicios/**",
                    "/api/solicitudes/**"
                )
                .hasAnyRole(
                    "MASTER",
                    "COORDINADOR",
                    "ADMINISTRADOR",
                    "OPERADOR",
                    "CONSULTA"
                )

                /*
                 * Los servicios deben originarse desde
                 * una solicitud aprobada.
                 */
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/servicios",
                    "/api/servicios/**"
                )
                .denyAll()

                /*
                 * Creación de información operativa.
                 */
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/vehiculos/**",
                    "/api/conductores/**",
                    "/api/rutas/**",
                    "/api/ubicaciones/**",
                    "/api/solicitudes/**"
                )
                .hasAnyRole(
                    "MASTER",
                    "COORDINADOR",
                    "ADMINISTRADOR",
                    "OPERADOR"
                )

                /*
                 * Actualizaciones completas.
                 */
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/vehiculos/**",
                    "/api/conductores/**",
                    "/api/rutas/**",
                    "/api/ubicaciones/**",
                    "/api/servicios/**",
                    "/api/solicitudes/**"
                )
                .hasAnyRole(
                    "MASTER",
                    "COORDINADOR",
                    "ADMINISTRADOR",
                    "OPERADOR"
                )

                /*
                 * Actualizaciones parciales.
                 */
                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/vehiculos/**",
                    "/api/conductores/**",
                    "/api/rutas/**",
                    "/api/ubicaciones/**",
                    "/api/servicios/**",
                    "/api/solicitudes/**"
                )
                .hasAnyRole(
                    "MASTER",
                    "COORDINADOR",
                    "ADMINISTRADOR",
                    "OPERADOR"
                )

                /*
                 * Las eliminaciones quedan reservadas
                 * al administrador.
                 */
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/vehiculos/**",
                    "/api/conductores/**",
                    "/api/rutas/**",
                    "/api/ubicaciones/**",
                    "/api/servicios/**",
                    "/api/solicitudes/**"
                )
                .hasAnyRole("MASTER", "COORDINADOR", "ADMINISTRADOR")

                /*
                 * Consulta y descarga de evidencias.
                 * La propiedad del recurso también deberá
                 * validarse dentro del servicio.
                 */
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/evidencias/**"
                )
                .hasAnyRole(
                    "MASTER",
                    "COORDINADOR",
                    "ADMINISTRADOR",
                    "OPERADOR",
                    "CONSULTA",
                    "CHOFER"
                )

                /*
                 * Registro de evidencias.
                 */
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/evidencias/**"
                )
                .hasAnyRole(
                    "MASTER",
                    "COORDINADOR",
                    "ADMINISTRADOR",
                    "OPERADOR",
                    "CHOFER"
                )

                /*
                 * Consulta de encuestas.
                 */
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/encuestas/**"
                )
                .hasAnyRole(
                    "MASTER",
                    "COORDINADOR",
                    "ADMINISTRADOR",
                    "OPERADOR",
                    "CONSULTA"
                )

                /*
                 * Registrar encuestas modifica datos.
                 * CONSULTA queda excluido.
                 */
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/encuestas/**"
                )
                .hasAnyRole(
                    "MASTER",
                    "COORDINADOR",
                    "ADMINISTRADOR",
                    "OPERADOR"
                )

                /*
                 * Política restrictiva por defecto.
                 */
                .anyRequest()
                .denyAll()
            )

            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(
                        jwtConverter
                    )
                )
            )

            .addFilterBefore(new CambioContrasenaFilter(usuarioRepository), AuthorizationFilter.class)
            .addFilterAfter(
                actividadUsuarioFilter,
                BearerTokenAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    JwtAuthenticationConverter
        jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authorities =
            new JwtGrantedAuthoritiesConverter();

        authorities.setAuthoritiesClaimName("rol");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
            new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
            authorities
        );

        return converter;
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${app.jwt.secret}")
            String secret,
            UsuarioActivoJwtValidator usuarioActivoValidator) {

        byte[] clave =
            secret.getBytes(StandardCharsets.UTF_8);

        if (clave.length < 32) {
            throw new IllegalStateException(
                "La clave JWT debe tener al menos "
                    + "32 bytes"
            );
        }

        SecretKey secretKey =
            new SecretKeySpec(
                clave,
                "HmacSHA256"
            );

        NimbusJwtDecoder decoder =
            NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt>
            validatorEstandar =
                JwtValidators.createDefaultWithIssuer(
                    JwtService.EMISOR
                );

        OAuth2TokenValidator<Jwt> validator =
            new DelegatingOAuth2TokenValidator<>(
                validatorEstandar,
                usuarioActivoValidator
            );

        decoder.setJwtValidator(validator);

        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}")
            List<String> allowedOrigins) {

        CorsConfiguration configuration =
            new CorsConfiguration();

        configuration.setAllowedOrigins(
            allowedOrigins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList()
        );

        configuration.setAllowedMethods(
            List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
            )
        );

        configuration.setAllowedHeaders(
            List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With"
            )
        );

        /*
         * No se utilizan cookies de autenticación.
         * El JWT viaja en Authorization.
         */
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/api/**",
            configuration
        );

        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        /*
         * Los hashes anteriores continúan siendo válidos.
         * Las nuevas contraseñas usarán costo 12.
         */
        return new BCryptPasswordEncoder(12);
    }
}
