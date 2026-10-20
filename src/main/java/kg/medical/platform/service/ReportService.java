package kg.medical.platform.service;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import kg.medical.platform.dto.DoctorReportResponse;
import kg.medical.platform.messaging.EventOutbox;
import kg.medical.platform.messaging.KafkaTopics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class ReportService {
    private final KafkaTopics topics;
    private final JdbcTemplate jdbc;
    private final EventOutbox outbox;
    private final Clock clock;
    private final ZoneId zone;
    public ReportService(JdbcTemplate jdbc,EventOutbox outbox,Clock clock,@Value("${medical.zone:Asia/Bishkek}") String zone, KafkaTopics topics) { this.jdbc=jdbc; this.outbox=outbox; this.clock=clock; this.zone=ZoneId.of(zone); this.topics=topics; }
    @Transactional
    public DoctorReportResponse request(Long doctorId,int year) {
        validateYear(year);
        if (jdbc.queryForObject("select count(*) from doctors where id=?",Long.class,doctorId)==0) { throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Врач не найден"); }
        List<Long> ids=jdbc.queryForList("insert into doctor_reports(doctor_id,report_year) values (?,?) on conflict(doctor_id,report_year) do nothing returning id",Long.class,doctorId,year);
        if (!ids.isEmpty()) { outbox.enqueue(topics.reportRequests(),doctorId.toString(),"REPORT_REQUESTED",Map.of("reportId",ids.getFirst())); }
        return get(doctorId,year);
    }
    @Transactional
    public List<DoctorReportResponse> requestAll(int year) {
        validateYear(year);
        return jdbc.queryForList("select id from doctors order by id",Long.class).stream().map(id->request(id,year)).toList();
    }
    @Transactional
    public void catchUpAnnualReports() {
        LocalDate today=LocalDate.now(clock.withZone(zone));
        if (!today.isBefore(LocalDate.of(today.getYear(),1,31))) { requestAll(today.getYear()-1); }
    }
    @Transactional(readOnly=true)
    public List<DoctorReportResponse> list(Long doctorId) {
        return jdbc.query("select * from doctor_reports where doctor_id=? order by report_year desc",this::map,doctorId);
    }
    @Transactional(readOnly=true)
    public DoctorReportResponse get(Long doctorId,int year) {
        List<DoctorReportResponse> reports=jdbc.query("select * from doctor_reports where doctor_id=? and report_year=?",this::map,doctorId,year);
        if (reports.isEmpty()) { throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Отчёт не найден"); }
        return reports.getFirst();
    }
    @Transactional
    public void generate(Long reportId) {
        Map<String,Object> job=jdbc.queryForMap("select doctor_id,report_year from doctor_reports where id=? for update",reportId);
        int year=((Number)job.get("report_year")).intValue();
        Timestamp from=Timestamp.from(LocalDate.of(year,1,1).atStartOfDay(zone).toInstant());
        Timestamp until=Timestamp.from(LocalDate.of(year+1,1,1).atStartOfDay(zone).toInstant());
        Map<String,Object> totals=jdbc.queryForMap("select count(*) as total,count(*) filter(where a.status='CREATED') as created,count(*) filter(where a.status='COMPLETED') as completed,count(*) filter(where a.status='CANCELED') as canceled from appointments a join doctor_slots s on s.id=a.slot_id where s.doctor_id=? and a.starts_at>=? and a.starts_at<?",job.get("doctor_id"),from,until);
        jdbc.update("update doctor_reports set total_count=?,created_count=?,completed_count=?,canceled_count=?,generated_at=? where id=?",totals.get("total"),totals.get("created"),totals.get("completed"),totals.get("canceled"),Timestamp.from(clock.instant()),reportId);
    }
    private void validateYear(int year) {
        int currentYear=LocalDate.now(clock.withZone(zone)).getYear();
        if (year<1970 || year>=currentYear) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Выберите завершившийся год начиная с 1970"); }
    }
    private DoctorReportResponse map(ResultSet row,int number) throws SQLException {
        Timestamp generated=row.getTimestamp("generated_at");
        return new DoctorReportResponse(row.getLong("id"),row.getLong("doctor_id"),row.getInt("report_year"),generated==null?"QUEUED":"READY",row.getLong("total_count"),row.getLong("created_count"),row.getLong("completed_count"),row.getLong("canceled_count"),generated==null?null:generated.toInstant());
    }
}
