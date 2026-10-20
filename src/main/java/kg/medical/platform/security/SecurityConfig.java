package kg.medical.platform.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity

public class SecurityConfig {
    private static final Set<String> PLATFORM_ROLES = Set.of("DOCTOR", "PATIENT", "ADMIN");

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/specialties", "/api/doctors", "/api/doctors/*/slots").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/patients").hasRole("ADMIN")
                        .requestMatchers("/api/patients/**").hasAnyRole("PATIENT", "ADMIN")
                        .requestMatchers("/api/doctors/**").hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers("/api/me").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
                for (Object role : roles) {
                    if (role instanceof String name && PLATFORM_ROLES.contains(name)) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + name));
                    }
                }
            }
            return authorities;
        });
        return converter;
    }
}
