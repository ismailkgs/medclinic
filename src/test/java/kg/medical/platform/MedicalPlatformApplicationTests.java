package kg.medical.platform;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.awaitility.Awaitility.await;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import kg.medical.platform.dto.*;
import kg.medical.platform.entity.*;
import kg.medical.platform.messaging.*;
import kg.medical.platform.notification.ReminderService;
import kg.medical.platform.security.SecurityConfig;
import kg.medical.platform.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.ObjectMapper;
@Testcontainers
@SpringBootTest(properties={"medical.jobs.enabled=false","medical.sms.mode=log"})
@AutoConfigureMockMvc
class MedicalPlatformApplicationTests {
    @Container
    static final PostgreSQLContainer POSTGRES=new PostgreSQLContainer("postgres:18-alpine").withDatabaseName("medical_test").withUsername("test").withPassword("test");
    @Container
    static final KafkaContainer KAFKA=new KafkaContainer("apache/kafka:4.1.2");
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username",POSTGRES::getUsername);
        registry.add("spring.datasource.password",POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers",KAFKA::getBootstrapServers);
    }
    @TestConfiguration
    static class TimeConfiguration {
        @Bean
        @Primary
        MutableClock testClock() { return new MutableClock(); }
    }
    static class MutableClock extends Clock {
        private final AtomicReference<Instant> value=new AtomicReference<>(Instant.parse("2030-02-01T00:00:00Z"));
        void set(Instant instant) { value.set(instant); }
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return Clock.fixed(instant(),zone); }
        public Instant instant() { return value.get(); }
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired MutableClock clock;
    @Autowired DoctorSlotService slots;
    @Autowired AppointmentService appointments;
    @Autowired LabService labs;
    @Autowired ReportService reports;
    @Autowired EventProcessor processor;
    @Autowired OutboxPublisher publisher;
    @Autowired ReminderService reminders;
    @Autowired PlatformTransactionManager transactions;
    @Autowired SecurityConfig security;
    @MockitoBean JwtDecoder decoder;
    @BeforeEach
    void reset() {
        clock.set(Instant.parse("2030-02-01T00:00:00Z"));
        jdbc.execute("truncate sms_notifications,processed_events,outbox_events,lab_requests,doctor_reports,appointments,doctor_slots,patients,doctors restart identity cascade");
        jdbc.update("insert into doctors(last_name,first_name,patronymic,specialty_id,keycloak_subject) select 'Учебный','Иван','Иванович',id,'doctor-one' from doctor_specialties where name='Терапевт'");
        jdbc.update("insert into patients(last_name,first_name,patronymic,phone,keycloak_subject) values ('Тестовый','Пётр','Петрович','+12025550123','patient-one'),('Другой','Пациент',null,'+12025550124','patient-two')");
        when(decoder.decode(anyString())).thenThrow(new BadJwtException("Недействительный тестовый токен"));
    }
    private RequestPostProcessor user(String subject,String role) { return jwt().jwt(jwt->jwt.subject(subject)).authorities(new SimpleGrantedAuthority("ROLE_"+role)); }
    private SlotResponse slotAfter(long seconds) { return slots.create(1L,new SlotRequest(clock.instant().plusSeconds(seconds),clock.instant().plusSeconds(seconds+1800))); }
    private AppointmentResponse book(SlotResponse slot) { return appointments.book(1L,new CreateAppointmentRequest(slot.id())); }
    @Test
    void securitySeparatesAnonymousPatientDoctorAndAdmin() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/patients/1/appointments")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/patients/1/appointments").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/patients/1/appointments").with(user("patient-one","PATIENT"))).andExpect(status().isOk());
        mvc.perform(get("/api/patients/2/appointments").with(user("patient-one","PATIENT"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/doctors/1/appointments").with(user("patient-one","PATIENT"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/doctors/1/appointments").with(user("doctor-one","DOCTOR"))).andExpect(status().isOk());
        mvc.perform(get("/api/doctors/1/appointments").with(user("wrong-doctor","DOCTOR"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/patients/2/appointments").with(user("admin","ADMIN"))).andExpect(status().isOk());
    }
    @Test
    void keycloakRolesAreConvertedAndUnknownRolesIgnored() {
        Jwt token=Jwt.withTokenValue("test").header("alg","RS256").subject("person").claim("realm_access",Map.of("roles",List.of("PATIENT","ADMIN","unknown"))).build();
        assertThat(security.jwtAuthenticationConverter().convert(token).getAuthorities()).filteredOn(authority->authority.getAuthority().startsWith("ROLE_")).extracting("authority").containsExactlyInAnyOrder("ROLE_PATIENT","ROLE_ADMIN");
    }
    @Test
    void validationAndSchedulePermissionsAreEnforced() throws Exception {
        String body=mapper.writeValueAsString(new SlotRequest(clock.instant().plusSeconds(86400),clock.instant().plusSeconds(88200)));
        mvc.perform(post("/api/doctors/1/slots").with(user("patient-one","PATIENT")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/doctors/1/slots").with(user("doctor-one","DOCTOR")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post("/api/doctors/1/slots").with(user("doctor-one","DOCTOR")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        mvc.perform(post("/api/patients").with(user("admin","ADMIN")).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("lastName"," ","firstName","x","phone","bad")))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/admin/profiles/patients/2/identity").with(user("patient-one","PATIENT")).contentType(MediaType.APPLICATION_JSON).content("{\"subject\":\"evil\"}")).andExpect(status().isForbidden());
    }
    @Test
    void adjacentSlotsAllowedButOverlapsRejectedAndSoftDeleteWorks() {
        SlotResponse first=slotAfter(86400);
        SlotResponse adjacent=slots.create(1L,new SlotRequest(first.endsAt(),first.endsAt().plusSeconds(1800)));
        assertThatThrownBy(()->slots.create(1L,new SlotRequest(first.startsAt().plusSeconds(1),first.endsAt()))).isInstanceOf(ResponseStatusException.class);
        slots.update(1L,first.id(),new SlotRequest(first.startsAt().minusSeconds(3600),first.endsAt().minusSeconds(3600)));
        slots.delete(1L,adjacent.id());
        assertThat(jdbc.queryForObject("select deleted from doctor_slots where id=?",Boolean.class,adjacent.id())).isTrue();
        assertThat(slots.getFree(1L)).hasSize(1);
    }
    @Test
    void concurrentBookingProducesExactlyOneWinner() throws Exception {
        SlotResponse slot=slotAfter(86400);
        CountDownLatch ready=new CountDownLatch(2);
        CountDownLatch start=new CountDownLatch(1);
        try (ExecutorService executor=Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results=new ArrayList<>();
            for (long patientId:List.of(1L,2L)) {
                results.add(executor.submit(()->{ ready.countDown(); start.await(); try { appointments.book(patientId,new CreateAppointmentRequest(slot.id())); return 201; } catch (ResponseStatusException exception) { return exception.getStatusCode().value(); } }));
            }
            assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(results.get(0).get(20,TimeUnit.SECONDS),results.get(1).get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
        }
        assertThat(jdbc.queryForObject("select count(*) from appointments where slot_id=? and status='CREATED'",Long.class,slot.id())).isEqualTo(1);
    }
    @Test
    void cancellationPreservesHistoryAndSnapshotWhileAllowingRebooking() {
        SlotResponse slot=slotAfter(86400);
        AppointmentResponse first=book(slot);
        assertThatThrownBy(()->slots.delete(1L,slot.id())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->slots.update(1L,slot.id(),new SlotRequest(slot.startsAt().plusSeconds(3600),slot.endsAt().plusSeconds(3600)))).isInstanceOf(ResponseStatusException.class);
        appointments.cancel(1L,first.id());
        slots.update(1L,slot.id(),new SlotRequest(slot.startsAt().plusSeconds(3600),slot.endsAt().plusSeconds(3600)));
        AppointmentResponse second=appointments.book(1L,new CreateAppointmentRequest(slot.id()));
        List<AppointmentResponse> history=appointments.getPatientHistory(1L,null,null,null);
        assertThat(history).hasSize(2);
        assertThat(history.stream().filter(a->a.id().equals(first.id())).findFirst().orElseThrow().startsAt()).isEqualTo(first.startsAt());
        assertThat(second.startsAt()).isEqualTo(slot.startsAt().plusSeconds(3600));
        assertThat(appointments.getPatientHistory(1L,AppointmentStatus.CANCELED,null,"Учебный")).hasSize(1);
        assertThat(appointments.getPatientHistory(1L,null,null,"%")).isEmpty();
        assertThat(appointments.getPatientHistory(1L,null,999L,null)).isEmpty();
        assertThat(appointments.getDoctorHistory(1L,AppointmentStatus.CREATED,"Тестовый")).hasSize(1);
    }
    @Test
    void completionRequiresStartedVisitAndFinalStatusCannotBeChanged() {
        AppointmentResponse appointment=book(slotAfter(3600));
        assertThatThrownBy(()->appointments.changeDoctorStatus(1L,appointment.id(),AppointmentStatus.COMPLETED)).isInstanceOf(ResponseStatusException.class);
        clock.set(appointment.startsAt().plusSeconds(1));
        assertThat(appointments.changeDoctorStatus(1L,appointment.id(),AppointmentStatus.COMPLETED).status()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThatThrownBy(()->appointments.cancel(1L,appointment.id())).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(()->appointments.changeDoctorStatus(1L,appointment.id(),AppointmentStatus.CANCELED)).isInstanceOf(ResponseStatusException.class);
    }
    @Test
    void laboratoryWorkflowAndOwnershipAreEnforced() throws Exception {
        var request=labs.create(1L,new CreateLabRequest(1L,"BLOOD_COUNT"));
        String body=mapper.writeValueAsString(new LabBookingRequest(clock.instant().plusSeconds(86400)));
        mvc.perform(post("/api/patients/1/lab-requests/"+request.id()+"/booking").with(user("patient-two","PATIENT")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/patients/1/lab-requests/"+request.id()+"/booking").with(user("patient-one","PATIENT")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("BOOKED"));
        assertThatThrownBy(()->labs.book(1L,request.id(),clock.instant().plusSeconds(90000))).isInstanceOf(ResponseStatusException.class);
        assertThat(labs.cancel(1L,request.id()).status()).isEqualTo("CANCELED");
        assertThat(jdbc.queryForObject("select count(*) from outbox_events",Long.class)).isEqualTo(3);
    }
    @Test
    @SuppressWarnings("unchecked")
    void outboxRetriesFailureAndRealKafkaDeliveryIsIdempotent() {
        var request=labs.create(1L,new CreateLabRequest(1L,"GLUCOSE"));
        KafkaTemplate<String,String> broken=mock(KafkaTemplate.class);
        when(broken.send(anyString(),anyString(),anyString())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("simulated outage")));
        assertThatThrownBy(()->new TransactionTemplate(transactions).execute(status->new OutboxPublisher(jdbc,broken,clock).publishBatch())).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("select count(*) from outbox_events where published_at is null",Long.class)).isEqualTo(1);
        assertThat(publisher.publishBatch()).isEqualTo(1);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(()->assertThat(jdbc.queryForObject("select dispatched_at from lab_requests where id=?",Timestamp.class,request.id())).isNotNull());
        Timestamp original=jdbc.queryForObject("select dispatched_at from lab_requests where id=?",Timestamp.class,request.id());
        String payload=jdbc.queryForObject("select payload from outbox_events",String.class);
        clock.set(clock.instant().plusSeconds(60));
        processor.lab(payload);
        assertThat(jdbc.queryForObject("select dispatched_at from lab_requests where id=?",Timestamp.class,request.id())).isEqualTo(original);
        assertThat(jdbc.queryForObject("select count(*) from processed_events where handler='laboratory'",Long.class)).isEqualTo(1);
    }
    private void historical(String start,String status) {
        Instant instant=Instant.parse(start);
        Long slotId=jdbc.queryForObject("insert into doctor_slots(doctor_id,starts_at,ends_at) values (1,?,?) returning id",Long.class,Timestamp.from(instant),Timestamp.from(instant.plusSeconds(1800)));
        jdbc.update("insert into appointments(patient_id,slot_id,status,starts_at,ends_at) values (1,?,?,?,?)",slotId,status,Timestamp.from(instant),Timestamp.from(instant.plusSeconds(1800)));
    }
    @Test
    void annualReportRespectsBishkekYearBoundariesAndKafkaProcessing() {
        historical("2028-12-31T18:00:00Z","COMPLETED");
        historical("2029-06-01T00:00:00Z","CREATED");
        historical("2029-12-31T17:00:00Z","CANCELED");
        historical("2029-12-31T18:00:00Z","COMPLETED");
        var queued=reports.request(1L,2029);
        reports.request(1L,2029);
        assertThat(jdbc.queryForObject("select count(*) from outbox_events",Long.class)).isEqualTo(1);
        publisher.publishBatch();
        await().atMost(Duration.ofSeconds(30)).untilAsserted(()->assertThat(reports.get(1L,2029).status()).isEqualTo("READY"));
        var report=reports.get(1L,2029);
        assertThat(report.total()).isEqualTo(3);
        assertThat(report.created()).isEqualTo(1);
        assertThat(report.completed()).isEqualTo(1);
        assertThat(report.canceled()).isEqualTo(1);
        assertThat(report.id()).isEqualTo(queued.id());
        String payload=jdbc.queryForObject("select payload from outbox_events",String.class);
        clock.set(clock.instant().plusSeconds(60));
        processor.report(payload);
        assertThat(reports.get(1L,2029).generatedAt()).isEqualTo(report.generatedAt());
    }
    @Test
    void annualCatchUpRunsOnJanuary31AndDoesNotDuplicateJobs() {
        clock.set(Instant.parse("2030-01-30T17:59:59Z"));
        reports.catchUpAnnualReports();
        assertThat(jdbc.queryForObject("select count(*) from doctor_reports",Long.class)).isZero();
        clock.set(Instant.parse("2030-01-30T18:00:00Z"));
        reports.catchUpAnnualReports();
        reports.catchUpAnnualReports();
        assertThat(jdbc.queryForObject("select count(*) from doctor_reports where report_year=2029",Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from outbox_events",Long.class)).isEqualTo(1);
    }
    @Test
    void reminderIsQueuedAtFiveHoursAndSimulationIsNotReportedAsSent() {
        AppointmentResponse appointment=book(slotAfter(5*3600+60));
        assertThat(reminders.enqueueDue()).isZero();
        clock.set(clock.instant().plusSeconds(60));
        assertThat(reminders.enqueueDue()).isEqualTo(1);
        assertThat(reminders.deliverPending()).isEqualTo(1);
        assertThat(reminders.enqueueDue()).isZero();
        assertThat(reminders.deliverPending()).isZero();
        assertThat(jdbc.queryForObject("select state from sms_notifications where appointment_id=?",String.class,appointment.id())).isEqualTo("SIMULATED");
    }
    @Test
    void cancellationSuppressesAlreadyQueuedReminder() {
        AppointmentResponse appointment=book(slotAfter(3600));
        assertThat(reminders.enqueueDue()).isEqualTo(1);
        appointments.cancel(1L,appointment.id());
        assertThat(reminders.deliverPending()).isZero();
        assertThat(jdbc.queryForObject("select state from sms_notifications where appointment_id=?",String.class,appointment.id())).isEqualTo("SKIPPED");
    }
}
