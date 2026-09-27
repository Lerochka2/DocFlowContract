package by.bsuir.docflow.docflow_backend.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter authenticationConverter =
                new JwtAuthenticationConverter();

        authenticationConverter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return authenticationConverter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:4200",
                        "http://127.0.0.1:4200"
                )
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
                        "Accept"
                )
        );

        configuration.setAllowCredentials(
                true
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/api/**",
                configuration
        );

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        http
                .cors(cors ->
                        cors.configurationSource(
                                corsConfigurationSource
                        )
                )

                .csrf(csrf ->
                        csrf.disable()
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        )
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        )
                        .permitAll()

                        .requestMatchers(
                                "/api/health",
                                "/api/auth/login",
                                "/error"
                        )
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/me"
                        )
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/password-change/request-code",
                                "/api/auth/password-change/verify-code"
                        )
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/auth/password-change/confirm"
                        )
                        .authenticated()

                        /*
                         * Administration
                         */
                        .requestMatchers(
                                "/api/roles",
                                "/api/roles/**",
                                "/api/users",
                                "/api/users/**"
                        )
                        .hasRole("ADMIN")

                        /*
                         * Upload documents
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/documents/upload"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "DOCUMENT_OPERATOR"
                        )

                        /*
                         * AI processing
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/documents/*/analyze"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "DOCUMENT_OPERATOR"
                        )

                        /*
                         * Contract risk analysis.
                         * Object-level access is checked in service layer.
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/documents/*/risk-analysis"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "DOCUMENT_OPERATOR",
                                "LAWYER",
                                "CONTRACT_MANAGER"
                        )

                        /*
                         * Manual AI review
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/documents/*/review"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "DOCUMENT_OPERATOR"
                        )

                        /*
                         * Link additional document to base contract
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/documents/*/link"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "DOCUMENT_OPERATOR"
                        )

                        /*
                         * Archive completed document / restore from archive
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/documents/*/archive",
                                "/api/documents/*/restore"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "DOCUMENT_OPERATOR"
                        )
                        /*
                         * Delete unfinished document
                         */
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/documents/*"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "DOCUMENT_OPERATOR"
                        )

                        /*
                         * Metadata editing.
                         * Object-level access is checked in service layer.
                         */
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/documents/*/metadata"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "DOCUMENT_OPERATOR",
                                "LAWYER",
                                "CONTRACT_MANAGER"
                        )

                        /*
                         * Upload a new document version.
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/documents/*/versions"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "DOCUMENT_OPERATOR"
                        )

                        /*
                         * Resubmit corrected document for specialist review.
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/documents/*/resubmit-for-review"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "DOCUMENT_OPERATOR"
                        )

                        /*
                         * Specialist decision:
                         * approve / return for correction / reject.
                         * Object-level access is checked in service layer.
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/documents/*/decision"
                        )
                        .hasAnyRole(
                                "LAWYER",
                                "CONTRACT_MANAGER"
                        )

                        /*
                         * Document comments.
                         * Object-level access is checked in service layer.
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/documents/*/comments"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "DOCUMENT_OPERATOR",
                                "LAWYER",
                                "CONTRACT_MANAGER"
                        )

                        /*
                         * Prepare document for SED.
                         * Object-level access is checked in service layer.
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/documents/*/prepare-for-sed"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "DOCUMENT_OPERATOR"
                        )

                        /*
                         * Send document to SED
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/documents/*/send-to-sed"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "DOCUMENT_OPERATOR"
                        )

                        /*
                         * Current user profile
                         */
                        .requestMatchers(
                                "/api/profile",
                                "/api/profile/**"
                        )
                        .authenticated()
                        /*
                         * Approval workflow read access
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/approval/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "DOCUMENT_OPERATOR",
                                "LAWYER",
                                "CONTRACT_MANAGER"
                        )

                        /*
                         * Create approval flow from current route
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/approval/documents/*/initialize"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "DOCUMENT_OPERATOR"
                        )

                        /*
                         * Specialist takes approval step
                         */
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/approval/steps/*/claim"
                        )
                        .hasAnyRole(
                                "LAWYER",
                                "CONTRACT_MANAGER"
                        )
                        /*
                         * Notifications
                         */
                        .requestMatchers(
                                "/api/notifications",
                                "/api/notifications/**"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "DOCUMENT_OPERATOR",
                                "LAWYER",
                                "CONTRACT_MANAGER"
                        )

                        /*
                         * Read-only document APIs.
                         * Object-level filtering/access is checked
                         * in service layer.
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/documents",
                                "/api/documents/**",
                                "/api/workspace/documents",
                                "/api/workspace/dashboard",
                                "/api/workspace/activity"
                        )
                        .hasAnyRole(
                                "ADMIN",
                                "DOCUMENT_OPERATOR",
                                "LAWYER",
                                "CONTRACT_MANAGER"
                        )

                        /*
                         * Everything else is denied by default.
                         */
                        .anyRequest()
                        .denyAll()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                );

        return http.build();
    }
}
