package kg.medical.platform.messaging;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
@Service
public class EventOutbox {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public EventOutbox(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }
    @Transactional(propagation = Propagation.MANDATORY)
    public UUID enqueue(String topic, String key, String type, Map<String,Object> data) {
        UUID id = UUID.randomUUID();
        String payload = mapper.writeValueAsString(Map.of("eventId", id.toString(), "type", type, "data", data));
        jdbc.update("insert into outbox_events(id,topic,event_key,payload) values (?,?,?,?)", id, topic, key, payload);
        return id;
    }
}
