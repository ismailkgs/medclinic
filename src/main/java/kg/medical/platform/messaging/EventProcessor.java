package kg.medical.platform.messaging;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;
import kg.medical.platform.service.ReportService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
@Service
public class EventProcessor {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final ReportService reports;
    private final Clock clock;
    public EventProcessor(JdbcTemplate jdbc,ObjectMapper mapper,ReportService reports,Clock clock) { this.jdbc=jdbc; this.mapper=mapper; this.reports=reports; this.clock=clock; }
    @Transactional
    public void lab(String payload) {
        JsonNode event=mapper.readTree(payload);
        if (!Set.of("LAB_REQUESTED","LAB_BOOKED","LAB_CANCELED").contains(event.path("type").asText())) { throw new IllegalArgumentException("Неизвестное событие лаборатории"); }
        if (!firstProcessing(event,"laboratory")) { return; }
        long id=event.path("data").path("requestId").asLong();
        if (jdbc.update("update lab_requests set dispatched_at=? where id=?",Timestamp.from(clock.instant()),id)!=1) { throw new IllegalArgumentException("Направление события не найдено"); }
    }
    @Transactional
    public void report(String payload) {
        JsonNode event=mapper.readTree(payload);
        if (!event.path("type").asText().equals("REPORT_REQUESTED")) { throw new IllegalArgumentException("Неизвестное событие отчёта"); }
        if (!firstProcessing(event,"reporting")) { return; }
        reports.generate(event.path("data").path("reportId").asLong());
    }
    private boolean firstProcessing(JsonNode event,String handler) {
        UUID id=UUID.fromString(event.path("eventId").asText());
        return jdbc.update("insert into processed_events(event_id,handler) values (?,?) on conflict do nothing",id,handler)==1;
    }
}
