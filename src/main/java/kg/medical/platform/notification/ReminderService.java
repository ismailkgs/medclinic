package kg.medical.platform.notification;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ReminderService {
    private final JdbcTemplate jdbc;
    private final SmsGateway gateway;
    private final Clock clock;
    private final DateTimeFormatter formatter;
    public ReminderService(JdbcTemplate jdbc,SmsGateway gateway,Clock clock,@Value("${medical.zone:Asia/Bishkek}") String zone) { this.jdbc=jdbc; this.gateway=gateway; this.clock=clock; this.formatter=DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of(zone)); }
    @Transactional
    public int enqueueDue() {
        Timestamp now=Timestamp.from(clock.instant());
        Timestamp threshold=Timestamp.from(clock.instant().plus(Duration.ofHours(5)));
        List<Map<String,Object>> rows=jdbc.queryForList("select a.id,a.starts_at,p.phone from appointments a join patients p on p.id=a.patient_id where a.status='CREATED' and a.starts_at>? and a.starts_at<=? and not exists(select 1 from sms_notifications n where n.appointment_id=a.id) order by a.starts_at,a.id limit 100 for update of a skip locked",now,threshold);
        int added=0;
        for (Map<String,Object> row:rows) {
            String message="Напоминание: приём "+formatter.format(((Timestamp)row.get("starts_at")).toInstant())+". Запись №"+row.get("id")+".";
            added+=jdbc.update("insert into sms_notifications(id,appointment_id,phone,message,next_attempt_at) values (?,?,?,?,?) on conflict(appointment_id) do nothing",UUID.randomUUID(),row.get("id"),row.get("phone"),message,now);
        }
        return added;
    }
    @Transactional
    public int deliverPending() {
        List<Map<String,Object>> pending=jdbc.queryForList("select * from sms_notifications where state='PENDING' and next_attempt_at<=? order by created_at,id limit 50 for update skip locked",Timestamp.from(clock.instant()));
        int delivered=0;
        for (Map<String,Object> row:pending) {
            UUID id=(UUID)row.get("id");
            Map<String,Object> appointment=jdbc.queryForMap("select status,starts_at from appointments where id=? for update",row.get("appointment_id"));
            if (!appointment.get("status").equals("CREATED") || !((Timestamp)appointment.get("starts_at")).toInstant().isAfter(clock.instant())) {
                jdbc.update("update sms_notifications set state='SKIPPED',last_error='Запись отменена, завершена или уже началась' where id=?",id);
                continue;
            }
            try {
                SmsGateway.Result result=gateway.send(id,(String)row.get("phone"),(String)row.get("message"));
                jdbc.update("update sms_notifications set state=?,provider_id=?,sent_at=?,attempts=attempts+1,last_error=null where id=?",result.simulated()?"SIMULATED":"SENT",result.providerId(),Timestamp.from(clock.instant()),id);
                delivered++;
            } catch (RuntimeException exception) {
                int attempts=((Number)row.get("attempts")).intValue()+1;
                jdbc.update("update sms_notifications set state=?,attempts=?,next_attempt_at=?,last_error=? where id=?",attempts>=10?"FAILED":"PENDING",attempts,Timestamp.from(clock.instant().plusSeconds(60)),exception.getMessage(),id);
            }
        }
        return delivered;
    }
    @Transactional(readOnly=true)
    public List<Map<String,Object>> history() { return jdbc.queryForList("select id,appointment_id,state,attempts,provider_id,created_at,sent_at,last_error from sms_notifications order by created_at desc,id desc limit 200"); }
}
