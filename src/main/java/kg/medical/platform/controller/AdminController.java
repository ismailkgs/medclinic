package kg.medical.platform.controller;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;
import jakarta.validation.Valid;
import kg.medical.platform.dto.IdentityRequest;
import kg.medical.platform.dto.DoctorReportResponse;
import kg.medical.platform.messaging.OutboxPublisher;
import kg.medical.platform.notification.ReminderService;
import kg.medical.platform.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping(value="/api/admin",produces="application/json;charset=UTF-8")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    @Value("${medical.sms.mode:log}")
    private String smsMode;
    private final ReportService reports;
    private final ReminderService reminders;
    private final OutboxPublisher publisher;
    private final JdbcTemplate jdbc;
    public AdminController(ReportService reports,ReminderService reminders,OutboxPublisher publisher,JdbcTemplate jdbc) { this.reports=reports; this.reminders=reminders; this.publisher=publisher; this.jdbc=jdbc; }
    @PostMapping("/reports/{year}")
    public List<DoctorReportResponse> reports(@PathVariable("year") int year) { return reports.requestAll(year); }
    @GetMapping("/sms")
    public List<Map<String,Object>> sms() { return reminders.history(); }
    @PostMapping("/reminders/run")
    public Map<String,Integer> remind() { int queued=reminders.enqueueDue(); int delivered=reminders.deliverPending(); return Map.of("queued",queued,"delivered",delivered); }
    @PostMapping("/outbox/publish")
    public Map<String,Integer> publish() { return Map.of("published",publisher.publishBatch()); }
    @GetMapping("/outbox")
    public List<Map<String,Object>> outbox() { return jdbc.queryForList("select id,topic,event_key,created_at,published_at from outbox_events order by created_at desc limit 200"); }
    @PutMapping("/profiles/{type}/{id}/identity")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void bind(@PathVariable("type") String type,@PathVariable("id") Long id,@Valid @RequestBody IdentityRequest request) {
        String table=switch(type) { case "patients" -> "patients"; case "doctors" -> "doctors"; default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Тип профиля: patients или doctors"); };
        if (jdbc.update("update "+table+" set keycloak_subject=? where id=?",request.subject(),id)!=1) { throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Профиль не найден"); }
    }
    @GetMapping("/runtime")
    public Map<String,String> runtime() { return Map.of("smsMode",smsMode); }
}
