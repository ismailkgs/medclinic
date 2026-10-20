package kg.medical.platform.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("accessPolicy")

public class AccessPolicy {
    private final JdbcTemplate jdbc;
    public AccessPolicy(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }
    public boolean patient(Long patientId) {
        return allowed(patientId, "PATIENT", "select count(*) from patients where id = ? and keycloak_subject = ?");
    }
    public boolean doctor(Long doctorId) {
        return allowed(doctorId, "DOCTOR", "select count(*) from doctors where id = ? and keycloak_subject = ?");
    }

    private boolean allowed(Long profileId, String requiredRole, String sql) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            return false;
        }
        if (hasRole(authentication, "ADMIN")) {
            return true;
        }
        if (profileId == null || !hasRole(authentication, requiredRole)) {
            return false;
        }

        Long matches = jdbc.queryForObject(sql, Long.class, profileId, token.getToken().getSubject());
        return matches != null && matches == 1;
    }
    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
