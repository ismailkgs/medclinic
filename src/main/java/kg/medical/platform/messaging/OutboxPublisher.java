package kg.medical.platform.messaging;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class OutboxPublisher {
    private final JdbcTemplate jdbc;
    private final KafkaTemplate<String,String> kafka;
    private final Clock clock;
    public OutboxPublisher(JdbcTemplate jdbc, KafkaTemplate<String,String> kafka, Clock clock) { this.jdbc=jdbc; this.kafka=kafka; this.clock=clock; }
    @Transactional
    public int publishBatch() {
        List<Map<String,Object>> events=jdbc.queryForList("select id,topic,event_key,payload from outbox_events where published_at is null order by created_at,id limit 50 for update skip locked");
        for (Map<String,Object> event : events) {
            try {
                kafka.send((String)event.get("topic"),(String)event.get("event_key"),(String)event.get("payload")).get(10,TimeUnit.SECONDS);
                jdbc.update("update outbox_events set published_at=? where id=?",Timestamp.from(clock.instant()),event.get("id"));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Отправка Outbox прервана",exception);
            } catch (Exception exception) {
                throw new IllegalStateException("Kafka недоступна: события сохранены для повторной отправки",exception);
            }
        }
        return events.size();
    }
}
