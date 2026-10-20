package kg.medical.platform.controller;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class CurrentUserController {
    private final JdbcTemplate jdbc;
    public CurrentUserController(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @GetMapping(value="/api/me",produces="application/json;charset=UTF-8")
    @PreAuthorize("hasAnyRole('DOCTOR','PATIENT','ADMIN')")
    public Map<String,Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("subject",jwt.getSubject(),"patientIds",jdbc.queryForList("select id from patients where keycloak_subject=?",Long.class,jwt.getSubject()),"doctorIds",jdbc.queryForList("select id from doctors where keycloak_subject=?",Long.class,jwt.getSubject()));
    }
}
