package kg.medical.platform.service;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import kg.medical.platform.dto.CreateLabRequest;
import kg.medical.platform.dto.LabResponse;
import kg.medical.platform.messaging.EventOutbox;
import kg.medical.platform.messaging.KafkaTopics;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class LabService {
    private final KafkaTopics topics;
    private final JdbcTemplate jdbc;
    private final EventOutbox outbox;
    private final Clock clock;
    public LabService(JdbcTemplate jdbc, EventOutbox outbox, Clock clock, KafkaTopics topics) { this.jdbc=jdbc; this.outbox=outbox; this.clock=clock; this.topics=topics; }
    @Transactional
    public LabResponse create(Long doctorId, CreateLabRequest request) {
        if (jdbc.queryForObject("select count(*) from patients where id=?",Long.class,request.patientId())==0 || jdbc.queryForObject("select count(*) from doctors where id=?",Long.class,doctorId)==0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Врач или пациент не найден");
        }
        Long id=jdbc.queryForObject("insert into lab_requests(doctor_id,patient_id,test_code) values (?,?,?) returning id",Long.class,doctorId,request.patientId(),request.testCode());
        outbox.enqueue(topics.labRequests(),id.toString(),"LAB_REQUESTED",Map.of("requestId",id));
        return get(id);
    }
    @Transactional(readOnly=true)
    public List<LabResponse> patientRequests(Long patientId) {
        return jdbc.query("select * from lab_requests where patient_id=? order by created_at desc,id desc",this::map,patientId);
    }
    @Transactional(readOnly=true)
    public List<LabResponse> doctorRequests(Long doctorId) {
        return jdbc.query("select * from lab_requests where doctor_id=? order by created_at desc,id desc",this::map,doctorId);
    }
    @Transactional
    public LabResponse book(Long patientId,Long requestId,Instant scheduledAt) {
        if (scheduledAt==null || !scheduledAt.isAfter(clock.instant())) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Выберите будущее время анализа"); }
        LabResponse request=lockPatientRequest(patientId,requestId);
        if (!request.status().equals("REQUESTED")) { throw new ResponseStatusException(HttpStatus.CONFLICT,"Направление уже записано или отменено"); }
        jdbc.update("update lab_requests set status='BOOKED',scheduled_at=? where id=?",Timestamp.from(scheduledAt),requestId);
        outbox.enqueue(topics.labRequests(),requestId.toString(),"LAB_BOOKED",Map.of("requestId",requestId,"scheduledAt",scheduledAt.toString()));
        return get(requestId);
    }
    @Transactional
    public LabResponse cancel(Long patientId,Long requestId) {
        LabResponse request=lockPatientRequest(patientId,requestId);
        if (request.status().equals("CANCELED")) { throw new ResponseStatusException(HttpStatus.CONFLICT,"Направление уже отменено"); }
        if (request.scheduledAt()!=null && !request.scheduledAt().isAfter(clock.instant())) { throw new ResponseStatusException(HttpStatus.CONFLICT,"Время анализа уже наступило"); }
        jdbc.update("update lab_requests set status='CANCELED' where id=?",requestId);
        outbox.enqueue(topics.labRequests(),requestId.toString(),"LAB_CANCELED",Map.of("requestId",requestId));
        return get(requestId);
    }
    private LabResponse lockPatientRequest(Long patientId,Long requestId) {
        List<LabResponse> rows=jdbc.query("select * from lab_requests where id=? and patient_id=? for update",this::map,requestId,patientId);
        if (rows.isEmpty()) { throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Направление пациента не найдено"); }
        return rows.getFirst();
    }
    private LabResponse get(Long id) { return jdbc.queryForObject("select * from lab_requests where id=?",this::map,id); }
    private LabResponse map(ResultSet row,int number) throws SQLException {
        Timestamp scheduled=row.getTimestamp("scheduled_at");
        Timestamp dispatched=row.getTimestamp("dispatched_at");
        return new LabResponse(row.getLong("id"),row.getLong("doctor_id"),row.getLong("patient_id"),row.getString("test_code"),row.getString("status"),scheduled==null?null:scheduled.toInstant(),row.getTimestamp("created_at").toInstant(),dispatched==null?null:dispatched.toInstant());
    }
}
