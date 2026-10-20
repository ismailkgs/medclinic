package kg.medical.platform.notification;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
@Service
@ConditionalOnProperty(name="medical.sms.mode",havingValue="log",matchIfMissing=true)
public class LogSmsGateway implements SmsGateway {
    private static final Logger log=LoggerFactory.getLogger(LogSmsGateway.class);
    public Result send(UUID id,String phone,String message) {
        log.info("SMS SIMULATION: notificationId={}; реальная SMS не отправлена",id);
        return new Result("LOG-"+id,true);
    }
}
