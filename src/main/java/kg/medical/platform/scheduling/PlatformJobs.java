package kg.medical.platform.scheduling;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import kg.medical.platform.messaging.OutboxPublisher;
import kg.medical.platform.notification.ReminderService;
import kg.medical.platform.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name="medical.jobs.enabled",havingValue="true",matchIfMissing=true)
public class PlatformJobs {
    private static final Logger log=LoggerFactory.getLogger(PlatformJobs.class);
    private final OutboxPublisher publisher;
    private final ReminderService reminders;
    private final ReportService reports;
    private final Clock clock;
    private final ZoneId zone;
    public PlatformJobs(OutboxPublisher publisher,ReminderService reminders,ReportService reports,Clock clock,@Value("${medical.zone:Asia/Bishkek}") String zone) { this.publisher=publisher; this.reminders=reminders; this.reports=reports; this.clock=clock; this.zone=ZoneId.of(zone); }
    @Scheduled(fixedDelayString="${medical.outbox.delay-ms:3000}")
    public void publish() {
        try { publisher.publishBatch(); } catch (RuntimeException exception) { log.warn("Outbox будет повторён: {}",exception.getMessage()); }
    }
    @Scheduled(fixedDelayString="${medical.reminders.delay-ms:60000}")
    public void remind() {
        try { reminders.enqueueDue(); reminders.deliverPending(); } catch (RuntimeException exception) { log.warn("Напоминания будут повторены: {}",exception.getMessage()); }
    }
    @Scheduled(cron="0 0 0 31 1 *",zone="${medical.zone:Asia/Bishkek}")
    public void annualReports() { reports.requestAll(LocalDate.now(clock.withZone(zone)).getYear()-1); }
    @Scheduled(cron="0 5 0 * * *",zone="${medical.zone:Asia/Bishkek}")
    @EventListener(ApplicationReadyEvent.class)
    public void catchUp() { reports.catchUpAnnualReports(); }
}
